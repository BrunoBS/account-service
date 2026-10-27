package br.com.portalmanager.platform.workspace.integration.support;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationContextExecutorService;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.category.CategoryRepository;
import br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment.SegmentRepository;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support.LifecycleTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.concurrent.ExecutorService;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@ExtendWith(CleanMySqlAfterEachExtension.class)
public abstract class BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private LifecycleTypeClient lifecycleTypes;

    @MockitoSpyBean protected AuditOutboxAppender auditAppender;
    @MockitoSpyBean protected ParameterPublicationStore publicationStore;
    @MockitoSpyBean protected ParameterKeyRepository parameterKeyRepository;
    @MockitoSpyBean protected ParameterKeyValidator parameterKeyValidator;
    @MockitoSpyBean protected WorkspaceRepository workspaceRepository;
    @MockitoSpyBean protected WorkspaceFinder workspaceFinder;
    @MockitoSpyBean protected SegmentRepository segmentRepository;
    @MockitoSpyBean protected CategoryRepository categoryRepository;

    @BeforeEach
    void createLifecycleSetupThroughHttp() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        CatalogScenario.builder()
                .withLifecycleTypes(lifecycleTypes, List.of(new LifecycleTypeFactory().active()))
                .setup();
    }

    protected ExecutorService testUserExecutor(ExecutorService delegate) {
        return AuthorizationContextExecutorService.wrap(delegate);
    }
}
