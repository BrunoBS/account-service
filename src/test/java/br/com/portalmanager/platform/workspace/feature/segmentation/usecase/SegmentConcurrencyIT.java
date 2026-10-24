package br.com.portalmanager.platform.workspace.feature.segmentation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CreateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.UpdateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.CreateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.SegmentOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.UpdateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category.CategoryCommandService;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category.CategoryQueryService;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentCommandService;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentQueryService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultScenario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SegmentConcurrencyIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{
    @Autowired private SchemaDefaultScenario schemaDefaults;
    @Autowired private SegmentCommandService segments;
    @Autowired private CategoryCommandService categories;
    @Autowired private SegmentQueryService segmentQueries;
    @Autowired private CategoryQueryService categoryQueries;
    @Autowired private PlatformTransactionManager transactionManager;
    @BeforeEach
    void prepare() {
        reset(segmentRepository, categoryRepository);
        schemaDefaults.setup();
    }

    @Test
    void concurrentSegmentCreationUsesTheUniqueConstraintEvenWhenBothPrechecksPass()
            throws Exception {
        doReturn(false).when(segmentRepository).existsByCode("mobile-pf");
        List<Object> results =
                concurrently(
                        () -> segments.create(new CreateSegmentInput(null, "mobile-pf", "Mobile")),
                        () ->
                                segments.create(
                                        new CreateSegmentInput(null, "mobile-pf", "Other name")));
        assertThat(results.stream().filter(SegmentOutput.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(DataIntegrityViolationException.class::isInstance))
                .hasSize(1);
        assertThat(segmentRepository.count()).isEqualTo(1);
    }

    @Test
    void categoryConstraintReservesCodesInsideTheParentEvenWhenThePrecheckMissesADuplicate()
            throws Exception {
        SegmentOutput segment = createSegment();
        Long internalId = segmentRepository.findByIdentifier(segment.identifier()).orElseThrow().getId();
        doReturn(false).when(categoryRepository).existsBySegmentIdAndCode(internalId, "premium");
        List<Object> results =
                concurrently(
                        () ->
                                categories.create(
                                        new CreateCategoryInput(
                                                null, "premium", "Premium", segment.identifier())),
                        () ->
                                categories.create(
                                        new CreateCategoryInput(
                                                null, "premium", "Other", segment.identifier())));
        assertThat(results.stream().filter(CategoryOutput.class::isInstance)).hasSize(1);
        assertThat(results.stream().filter(DataIntegrityViolationException.class::isInstance))
                .hasSize(1);
        assertThat(categoryRepository.count())
                .isEqualTo(1);
    }

    @Test
    void concurrentUpdatesWithTheSameClientVersionHaveExactlyOneWinnerForEachResource()
            throws Exception {
        SegmentOutput segment = createSegment();
        CategoryOutput category = createCategory(segment);
        List<Object> segmentResults =
                concurrently(
                        () ->
                                segments.update(
                                        segment.identifier(),
                                        new UpdateSegmentInput(0L, segment.code(), "First")),
                        () ->
                                segments.update(
                                        segment.identifier(),
                                        new UpdateSegmentInput(0L, segment.code(), "Second")));
        assertThat(segmentResults.stream().filter(SegmentOutput.class::isInstance)).hasSize(1);
        assertThat(
                        segmentResults.stream()
                                .filter(ResourceVersionConflictException.class::isInstance))
                .hasSize(1);
        List<Object> categoryResults =
                concurrently(
                        () ->
                                categories.update(
                                        segment.identifier(),
                                        category.identifier(),
                                        new UpdateCategoryInput(0L, category.code(), "First")),
                        () ->
                                categories.update(
                                        segment.identifier(),
                                        category.identifier(),
                                        new UpdateCategoryInput(0L, category.code(), "Second")));
        assertThat(categoryResults.stream().filter(CategoryOutput.class::isInstance)).hasSize(1);
        assertThat(
                        categoryResults.stream()
                                .filter(ResourceVersionConflictException.class::isInstance))
                .hasSize(1);
    }

    @Test
    void updateComparesTheLatestSegmentVersionAfterAnEarlierRepeatableReadSnapshot()
            throws Exception {
        SegmentOutput segment = createSegment();
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            Future<?> stale =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        segmentRepository
                                                                .findByIdentifier(segment.identifier())
                                                                .orElseThrow();
                                                        cached.countDown();
                                                        await(changed);
                                                        segments.update(
                                                                segment.identifier(),
                                                                new UpdateSegmentInput(
                                                                        0L,
                                                                        segment.code(),
                                                                        "Stale"));
                                                    }));
            try {
                await(cached);
                segments.update(
                        segment.identifier(),
                        new UpdateSegmentInput(0L, segment.code(), "Current"));
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> stale.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(segmentQueries.findByIdentifier(segment.identifier()).name())
                .isEqualTo("Current");
    }

    @Test
    void updateComparesTheLatestCategoryVersionAfterAnEarlierRepeatableReadSnapshot()
            throws Exception {
        SegmentOutput segment = createSegment();
        CategoryOutput category = createCategory(segment);
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            Future<?> stale =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        categoryRepository
                                                                .findBySegmentIdentifierAndIdentifier(segment.identifier(), category.identifier())
                                                                .orElseThrow();
                                                        cached.countDown();
                                                        await(changed);
                                                        categories.update(
                                                                segment.identifier(),
                                                                category.identifier(),
                                                                new UpdateCategoryInput(
                                                                        0L,
                                                                        category.code(),
                                                                        "Stale"));
                                                    }));
            try {
                await(cached);
                categories.update(
                        segment.identifier(),
                        category.identifier(),
                        new UpdateCategoryInput(0L, category.code(), "Current"));
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> stale.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ResourceVersionConflictException.class);
        }
        assertThat(
                        categoryQueries
                                .findByIdentifier(segment.identifier(), category.identifier())
                                .name())
                .isEqualTo("Current");
    }

    @Test
    void categoryCreationWaitsForSegmentInactivationAndRejectsTheCommittedInactiveState()
            throws Exception {
        SegmentOutput segment = createSegment();
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> waiting = new AtomicReference<>();
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                segments.inactivate(segment.identifier());
                                waiting.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return createCategory(segment);
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> waiting.get().get(250, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                            });
            assertThatThrownBy(() -> waiting.get().get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    void categoryCreationHoldsTheActiveParentUntilCommitAndLaterInactivationDoesNotCascade()
            throws Exception {
        SegmentOutput segment = createSegment();
        CountDownLatch attempted = new CountDownLatch(1);
        AtomicReference<Future<?>> waiting = new AtomicReference<>();
        AtomicReference<CategoryOutput> created = new AtomicReference<>();
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            new TransactionTemplate(transactionManager)
                    .executeWithoutResult(
                            status -> {
                                created.set(createCategory(segment));
                                waiting.set(
                                        executor.submit(
                                                () -> {
                                                    attempted.countDown();
                                                    return segments.inactivate(
                                                            segment.identifier());
                                                }));
                                await(attempted);
                                assertThatThrownBy(
                                                () -> waiting.get().get(250, TimeUnit.MILLISECONDS))
                                        .isInstanceOf(TimeoutException.class);
                            });
            waiting.get().get(10, TimeUnit.SECONDS);
        }
        CategoryOutput category =
                categoryQueries.findByIdentifier(segment.identifier(), created.get().identifier());
        assertThat(category.lifecycle()).isEqualTo("ACTIVE");
        assertThat(category.available()).isFalse();
        assertThat(category.version()).isZero();
    }

    @Test
    void categoryCreationReloadsTheParentAfterAnEarlierCachedActiveRead() throws Exception {
        SegmentOutput segment = createSegment();
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            Future<?> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .executeWithoutResult(
                                                    status -> {
                                                        segmentRepository
                                                                .findByIdentifier(segment.identifier())
                                                                .orElseThrow();
                                                        cached.countDown();
                                                        await(changed);
                                                        createCategory(segment);
                                                    }));
            try {
                await(cached);
                segments.inactivate(segment.identifier());
            } finally {
                changed.countDown();
            }
            assertThatThrownBy(() -> pending.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(ValidationException.class);
        }
        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    void categoryUpdateUsesTheInactiveParentAfterAnEarlierCachedActiveRead() throws Exception {
        assertCategoryUpdateUsesCurrentParentAvailability(true);
    }

    @Test
    void categoryUpdateUsesTheActiveParentAfterAnEarlierCachedInactiveRead() throws Exception {
        assertCategoryUpdateUsesCurrentParentAvailability(false);
    }

    private void assertCategoryUpdateUsesCurrentParentAvailability(boolean initiallyActive)
            throws Exception {
        SegmentOutput segment = createSegment();
        CategoryOutput category = createCategory(segment);
        if (!initiallyActive) segments.inactivate(segment.identifier());
        Long internalId = segmentRepository.findByIdentifier(segment.identifier()).orElseThrow().getId();
        CountDownLatch cached = new CountDownLatch(1);
        CountDownLatch changed = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newSingleThreadExecutor())) {
            Future<CategoryOutput> pending =
                    executor.submit(
                            () ->
                                    new TransactionTemplate(transactionManager)
                                            .execute(
                                                    status -> {
                                                        assertThat(
                                                                        segmentRepository
                                                                                .findById(
                                                                                        internalId)
                                                                                .orElseThrow()
                                                                                .isActive())
                                                                .isEqualTo(initiallyActive);
                                                        cached.countDown();
                                                        await(changed);
                                                        return categories.update(
                                                                segment.identifier(),
                                                                category.identifier(),
                                                                new UpdateCategoryInput(
                                                                        0L,
                                                                        category.code(),
                                                                        "Current"));
                                                    }));
            try {
                await(cached);
                if (initiallyActive) segments.inactivate(segment.identifier());
                else segments.activate(segment.identifier());
            } finally {
                changed.countDown();
            }
            CategoryOutput updated = pending.get(10, TimeUnit.SECONDS);
            assertThat(updated.available()).isEqualTo(!initiallyActive);
            assertThat(updated.lifecycle()).isEqualTo("ACTIVE");
            assertThat(updated.version()).isEqualTo(1L);
        }
        assertThat(
                        categoryQueries
                                .findByIdentifier(segment.identifier(), category.identifier())
                                .available())
                .isEqualTo(!initiallyActive);
    }

    private SegmentOutput createSegment() {
        return segments.create(new CreateSegmentInput(null, "mobile-pf", "Mobile"));
    }

    private CategoryOutput createCategory(SegmentOutput segment) {
        return categories.create(
                new CreateCategoryInput(null, "premium", "Premium", segment.identifier()));
    }

    private List<Object> concurrently(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = testUserExecutor(Executors.newFixedThreadPool(2))) {
            Future<Object> one = executor.submit(() -> result(start, first));
            Future<Object> two = executor.submit(() -> result(start, second));
            start.countDown();
            return List.of(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        }
    }

    private Object result(CountDownLatch start, Callable<?> operation) throws Exception {
        await(start);
        try {
            return operation.call();
        } catch (ConflictException | DataIntegrityViolationException exception) {
            return exception;
        }
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS))
                throw new AssertionError("Concurrent operation did not start");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
