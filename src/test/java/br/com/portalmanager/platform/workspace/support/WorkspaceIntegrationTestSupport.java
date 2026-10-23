package br.com.portalmanager.platform.workspace.support;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationSessionBuilder;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.redis.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@WithRedis
public abstract class WorkspaceIntegrationTestSupport {
    private UserSession previousUser;
    private UserSession testUser;

    @MockitoSpyBean protected AuditOutboxAppender auditAppender;
    @MockitoSpyBean protected ParameterPublicationStore publicationStore;
    @MockitoSpyBean protected ParameterKeyRepository parameterKeyRepository;
    @MockitoSpyBean protected ParameterKeyValidator parameterKeyValidator;
    @MockitoSpyBean protected WorkspaceRepository workspaceRepository;
    @MockitoSpyBean protected WorkspaceFinder workspaceFinder;
    @MockitoSpyBean protected SegmentRepository segmentRepository;
    @MockitoSpyBean protected CategoryRepository categoryRepository;

    @BeforeEach
    void establishTestUserContext() {
        previousUser = UserContext.get().orElse(null);
        testUser = AuthorizationSessionBuilder.builder().build();
        UserContext.set(testUser);
    }

    @AfterEach
    void restoreTestUserContext() {
        UserContext.set(previousUser);
        testUser = null;
        previousUser = null;
    }

    protected ExecutorService testUserExecutor(ExecutorService delegate) {
        if (testUser == null) {
            throw new IllegalStateException("Test user context must be established before creating an executor");
        }
        return new TestUserExecutor(delegate, testUser);
    }

    private static final class TestUserExecutor extends AbstractExecutorService {
        private final ExecutorService delegate;
        private final UserSession testUser;

        private TestUserExecutor(ExecutorService delegate, UserSession testUser) {
            this.delegate = delegate;
            this.testUser = testUser;
        }

        @Override
        public void execute(Runnable command) {
            delegate.execute(() -> {
                UserSession previous = UserContext.get().orElse(null);
                UserContext.set(testUser);
                try {
                    command.run();
                } finally {
                    UserContext.set(previous);
                }
            });
        }

        @Override
        public void shutdown() {
            delegate.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        @Override
        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        @Override
        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }
    }
}
