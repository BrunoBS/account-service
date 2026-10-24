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
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultScenario;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import tools.jackson.databind.json.JsonMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MessageConcurrencyIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{

    @Autowired private SchemaDefaultScenario schemaDefaults;
    @Autowired private PlatformRequestSpecificationFactory requests;
    private String microserviceIdentifier;
    @Autowired private MessageCommandService messages;
    @Autowired private MessageTranslatorCommandService translators;
    @Autowired private MessageFinder finder;
    @Autowired private MessageRepository repository;
    @Autowired private MessageTranslatorRepository translatorRepository;
    @Autowired private MicroserviceReferencePort microservices;
    @Autowired private br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.microservice.MicroserviceCommandService microserviceCommands;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void prepare() {
        schemaDefaults.setup();
        microserviceIdentifier = new MicroserviceClient(requests, JsonMapper.builder().build())
                .create("review-service", "Review service").identifier();
    }

    @Test
    void creationWaitsForParentLockAndRejectsDeletionAfterTheWait() throws Exception {
        MessageOutput created = messages.create(input("review.lock"));
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<MessageTranslatorOutput>> blocked = new AtomicReference<>();
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
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
        assertThat(translatorRepository.count())
                .isZero();
    }

    @Test
    void creationReloadsParentPreviouslyCachedAsActive() throws Exception {
        MessageOutput created = messages.create(input("review.cached"));
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch deleted = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
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
        assertThat(translatorRepository.count())
                .isZero();
    }

    @Test
    void updateComparesClientVersionAgainstCurrentTranslatorAfterCachedRead() throws Exception {
        MessageOutput created = messages.create(input("review.version"));
        MessageTranslatorOutput translator = translators.create(created.identifier(), translated());
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
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
                        translatorRepository.findByIdentifierAndMessageId(
                                translator.identifier(), repository.findByIdentifier(created.identifier()).orElseThrow().getId())
                                .orElseThrow().getTitle())
                .isEqualTo("Current");
    }

    @Test
    void deletionSnapshotReadsCurrentTranslatorAfterEarlierCachedRead() throws Exception {
        MessageOutput created = messages.create(input("review.purge.snapshot"));
        MessageTranslatorOutput translator = translators.create(created.identifier(), translated());
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch updated = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
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
                                                        List<
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
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                assertThat(
                                                microservices
                                                        .findActiveByIdentifier(microserviceIdentifier)
                                                        .orElseThrow()
                                                        .internalId())
                                        .isPositive();
                                blocked.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    microserviceCommands.inactivate(microserviceIdentifier);
                                                    return 1;
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
        assertThat(repository.count())
                .isEqualTo(1);
    }

    private CreateMessageInput input(String key) {
        return new CreateMessageInput(null, microserviceIdentifier, key, "REVIEW-101", 400, null, null);
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
