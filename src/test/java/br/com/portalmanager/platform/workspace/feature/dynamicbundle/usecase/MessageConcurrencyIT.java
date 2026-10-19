package br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message.MessageRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator.MessageTranslatorRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.message.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.MessageTranslatorOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageCommandService;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.message.MessageFinder;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorCommandService;
import br.com.portalmanager.platform.workspace.foundation.integration.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MessageConcurrencyIT {

    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MessageCommandService messages;
    @Autowired private MessageTranslatorCommandService translators;
    @Autowired private MessageFinder finder;
    @Autowired private MessageRepository repository;
    @Autowired private MessageTranslatorRepository translatorRepository;
    @Autowired private MicroserviceReferencePort microservices;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings)"
                        + " VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update(
                "INSERT INTO service_context_microservices"
                    + " (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES"
                    + " (?,'review-service','Review service','{}','ACTIVE',NOW(6),NOW(6))",
                MICROSERVICE);
    }

    @Test
    void creationWaitsForParentLockAndRejectsDeletionAfterTheWait() throws Exception {
        MessageOutput created = messages.create(input("review.lock"));
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<MessageTranslatorOutput>> blocked = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                Message parent = finder.findMessageForUpdate(created.identifier());
                                blocked.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return translators.create(
                                                            created.identifier(), translated());
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> blocked.get().get(200, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                                parent.inactivate(LocalDateTime.now());
                                repository.flush();
                                messages.delete(created.identifier());
                            });
            assertThatThrownBy(() -> blocked.get().get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(NotFoundException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_translators", Integer.class))
                .isZero();
    }

    @Test
    void creationReloadsParentPreviouslyCachedAsActive() throws Exception {
        MessageOutput created = messages.create(input("review.cached"));
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch deleted = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<MessageTranslatorOutput> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .execute(
                                                    status -> {
                                                        finder.findMessage(created.identifier());
                                                        cached.countDown();
                                                        await(deleted);
                                                        return translators.create(
                                                                created.identifier(), translated());
                                                    }));
            try {
                await(cached);
                messages.inactivate(created.identifier());
                messages.delete(created.identifier());
            } finally {
                deleted.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(NotFoundException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_translators", Integer.class))
                .isZero();
    }

    @Test
    void updateComparesClientVersionAgainstCurrentTranslatorAfterCachedRead() throws Exception {
        MessageOutput created = messages.create(input("review.version"));
        MessageTranslatorOutput translator = translators.create(created.identifier(), translated());
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<MessageTranslatorOutput> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .execute(
                                                    status -> {
                                                        finder.findTranslator(
                                                                finder.findMessage(
                                                                        created.identifier()),
                                                                translator.identifier());
                                                        cached.countDown();
                                                        await(updated);
                                                        return translators.update(
                                                                created.identifier(),
                                                                translator.identifier(),
                                                                new UpdateMessageTranslatorInput(
                                                                        0L,
                                                                        "pt-BR",
                                                                        "Stale",
                                                                        "Detail",
                                                                        "Suggestion"));
                                                    }));
            try {
                await(cached);
                translators.update(
                        created.identifier(),
                        translator.identifier(),
                        new UpdateMessageTranslatorInput(
                                0L, "pt-BR", "Current", "Detail", "Suggestion"));
            } finally {
                updated.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM message_translators WHERE identifier=?",
                                String.class,
                                translator.identifier()))
                .isEqualTo("Current");
    }

    @Test
    void deletionSnapshotReadsCurrentTranslatorAfterEarlierCachedRead() throws Exception {
        MessageOutput created = messages.create(input("review.purge.snapshot"));
        MessageTranslatorOutput translator = translators.create(created.identifier(), translated());
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        finder.findTranslator(
                                                                finder.findMessage(
                                                                        created.identifier()),
                                                                translator.identifier());
                                                        cached.countDown();
                                                        await(updated);
                                                        Message parent =
                                                                finder.findMessageForUpdate(
                                                                        created.identifier());
                                                        java.util.List<
                                                                        MessageTranslatorRepository
                                                                                .DeletionSnapshot>
                                                                snapshots =
                                                                        translatorRepository
                                                                                .findDeletionSnapshotsByMessageId(
                                                                                        parent
                                                                                                .getId());
                                                        assertThat(snapshots).hasSize(1);
                                                        assertThat(snapshots.get(0).getTitle())
                                                                .isEqualTo("Current");
                                                        assertThat(snapshots.get(0).getVersion())
                                                                .isEqualTo(1L);
                                                    }));
            try {
                await(cached);
                translators.update(
                        created.identifier(),
                        translator.identifier(),
                        new UpdateMessageTranslatorInput(
                                0L, "pt-BR", "Current", "Detail", "Suggestion"));
            } finally {
                updated.countDown();
            }
            pending.get(10, TimeUnit.SECONDS);
        }
    }

    @Test
    void activeMicroserviceRemainsStableUntilMessageTransactionCommits() throws Exception {
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<Integer>> blocked = new AtomicReference<>();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                assertThat(
                                                microservices.findActiveByIdentifier(MICROSERVICE)
                                                        .orElseThrow()
                                                        .internalId())
                                        .isPositive();
                                blocked.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return jdbc.update(
                                                            "UPDATE service_context_microservices"
                                                                + " SET lifecycle_code='INACTIVE'"
                                                                + " WHERE identifier=?",
                                                            MICROSERVICE);
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> blocked.get().get(200, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                                messages.create(input("review.microservice"));
                            });
            assertThat(blocked.get().get(10, TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThatThrownBy(() -> messages.create(input("review.microservice.invalid")))
                .isInstanceOf(ValidationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
                .isEqualTo(1);
    }

    private CreateMessageInput input(String key) {
        return new CreateMessageInput(null, MICROSERVICE, key, "REVIEW-101", 400, null, null);
    }

    private CreateMessageTranslatorInput translated() {
        return new CreateMessageTranslatorInput(null, "pt-BR", "Title", "Detail", "Suggestion");
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new AssertionError("Timed out waiting for the concurrent transaction");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }
}
