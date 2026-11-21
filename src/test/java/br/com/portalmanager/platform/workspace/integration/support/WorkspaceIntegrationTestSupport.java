package br.com.portalmanager.platform.workspace.integration.support;

import br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.internal.executor.AuthorizationContextExecutorService;
import br.com.portalmanager.platform.library.testing.authorization.api.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.api.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.http.api.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.redis.api.annotation.WithRedis;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.concurrent.ExecutorService;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@WithRedis
public abstract class WorkspaceIntegrationTestSupport {
    // These HTTP tests verify the committed outbox; delivery is owned by platform-audit.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected br.com.portalmanager.platform.library.audit.internal.outbox.AuditOutboxProcessor auditProcessor;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    protected br.com.portalmanager.platform.library.objectstorage.api.contract.ObjectStorageClient auditStorage;
    @MockitoSpyBean protected AuditOutboxAppender auditAppender;
    @MockitoSpyBean protected ParameterPublicationStore publicationStore;
    @MockitoSpyBean protected ParameterKeyRepository parameterKeyRepository;
    @MockitoSpyBean protected ParameterKeyValidator parameterKeyValidator;
    @MockitoSpyBean protected WorkspaceRepository workspaceRepository;
    @MockitoSpyBean protected WorkspaceFinder workspaceFinder;
    @MockitoSpyBean protected SegmentRepository segmentRepository;
    @MockitoSpyBean protected CategoryRepository categoryRepository;

    protected ExecutorService testUserExecutor(ExecutorService delegate) {
        return AuthorizationContextExecutorService.wrap(delegate);
    }
}
