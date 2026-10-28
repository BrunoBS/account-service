package br.com.portalmanager.platform.workspace.integration.foundation.authorization;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationScenario;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentBuilder;
import br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import static org.hamcrest.Matchers.is;

/** FIN-SEC-001: headers identify the caller origin; routes identify target resources. */
class AuthorizationOriginIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario environmentScenario;
    @Autowired private SchemaScenario schemas;
    @Autowired private ApplicationScopeTypeClient scopes;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private ApplicationClient applications;
    @Autowired private WorkspaceEnvironmentClient environments;
    @Autowired private AuthorizationMock authorization;
    @Autowired private PlatformRequestSpecificationFactory requests;
    private String visibleWorkspace;
    private String hiddenWorkspace;
    private String visibleApplication;
    private String hiddenApplication;
    private String visibleEnvironment;
    private String hiddenEnvironment;
    private final String originWorkspace = UUID.randomUUID().toString();
    private final String originApplication = UUID.randomUUID().toString();
    private final String originEnvironment = UUID.randomUUID().toString();

    @BeforeEach void setupTargetsThroughHttp() {
        environmentScenario.setup();
        new ApplicationScenario(schemas, scopes).setup();
        WorkspaceFactory workspaceFactory = new WorkspaceFactory();
        ApplicationFactory applicationFactory = new ApplicationFactory();
        visibleWorkspace = workspaces.create(workspaceFactory.managerWithAuthorizerGroup("Origin visible workspace", "TEAM_A"))
                .expectCreated().extractBody().identifier();
        hiddenWorkspace = workspaces.create(workspaceFactory.managerWithAuthorizerGroup("Origin hidden workspace", "TEAM_B"))
                .expectCreated().extractBody().identifier();
        visibleApplication = applications.create(visibleWorkspace, applicationFactory.withAuthorizerGroup("TEAM_A"))
                .expectCreated().extractBody().identifier();
        hiddenApplication = applications.create(hiddenWorkspace, applicationFactory.withAuthorizerGroup("TEAM_B"))
                .expectCreated().extractBody().identifier();
        visibleEnvironment = environments.create(visibleWorkspace, EnvironmentBuilder.builder().withAuthorizerGroup("TEAM_A").build())
                .expectCreated().extractBody().identifier();
        hiddenEnvironment = environments.create(hiddenWorkspace, EnvironmentBuilder.builder().withAuthorizerGroup("TEAM_B").build())
                .expectCreated().extractBody().identifier();
        authorization.reset();
        authorization.forbidden();
        authorization.allowResource(resource -> resource.workspace(originWorkspace).application(originApplication).environment(originEnvironment),
                session -> session.userName("origin-caller").groups("USER")
                        .addAuthorizerGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A")
                        .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A")
                        .addAuthorizerGroup("GRP_ENVIRONMENT_DEV_TEAM_A", "DEV", "DEV", "E-TEAM_A"));
    }

    private RequestSpecification origin() {
        return requests.create().header("Authorization", "Bearer origin-test").header("correlation-id", "origin-test")
                .header("workspace-identifier", originWorkspace).header("application-identifier", originApplication)
                .header("environment-identifier", originEnvironment);
    }

    @Test void authorizedOriginCanAccessVisibleTargetsWhoseIdentifiersDifferFromItsHeaders() {
        origin().get("/api/v1/workspaces/" + visibleWorkspace).then().statusCode(200).body("identifier", is(visibleWorkspace));
        origin().get("/api/v1/workspaces/" + visibleWorkspace + "/applications/" + visibleApplication)
                .then().statusCode(200).body("identifier", is(visibleApplication));
        origin().get("/api/v1/workspaces/" + visibleWorkspace + "/environments/" + visibleEnvironment)
                .then().statusCode(200).body("identifier", is(visibleEnvironment));
        authorization.verifyCalledWithWorkspace(originWorkspace);
        authorization.verifyCalledWithApplication(originApplication);
        authorization.verifyCalledWithEnvironment(originEnvironment);
        authorization.verifyCalledWithPolicy("DEV");
    }

    @Test void authorizedOriginDoesNotExposeTargetsOutsideItsVisibilityOrTheirActualParent() {
        origin().get("/api/v1/workspaces/" + hiddenWorkspace).then().statusCode(404);
        origin().get("/api/v1/workspaces/" + hiddenWorkspace + "/applications/" + hiddenApplication).then().statusCode(404);
        origin().get("/api/v1/workspaces/" + hiddenWorkspace + "/environments/" + hiddenEnvironment).then().statusCode(404);
        origin().get("/api/v1/workspaces/" + visibleWorkspace + "/applications/" + hiddenApplication).then().statusCode(404);
        origin().get("/api/v1/workspaces/" + visibleWorkspace + "/environments/" + hiddenEnvironment).then().statusCode(404);
    }

    @Test void missingOrDeniedOriginDoesNotBorrowTheTargetsPermission() {
        requests.create().header("Authorization", "Bearer origin-test").header("correlation-id", "origin-test")
                .get("/api/v1/workspaces/" + visibleWorkspace).then().statusCode(403);
        requests.create().header("Authorization", "Bearer origin-test").header("correlation-id", "origin-test")
                .header("workspace-identifier", visibleWorkspace).header("application-identifier", visibleApplication)
                .header("environment-identifier", visibleEnvironment)
                .get("/api/v1/workspaces/" + visibleWorkspace).then().statusCode(403);
    }
}
