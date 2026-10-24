package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;

import org.junit.jupiter.api.Test;

import java.util.*;

class OnboardingConfigurationIT extends EnvironmentConfigurationHttpFixture {
    @Test
    void fourRealEventsCompleteOnboardingAndHistorySurvivesLifecycleAndRestoration() {
        for (boolean copy : List.of(false, true)) {
            String workspaceIdentifier = createRegisteredWorkspace();
            assertThat(history(workspaceIdentifier)).hasSize(1);
            String applicationIdentifier =
                    post(
                                    "/api/v1/workspaces/" + workspaceIdentifier + "/applications",
                                    Map.of(
                                            "name",
                                            "First app",
                                            "alias",
                                            "first-app",
                                            "acronym",
                                            "APP",
                                            "applicationScope",
                                            "BACKEND",
                                            "settings",
                                            Map.of("language", "JAVA", "infrastructure", "VM"),
                                            "tags",
                                            List.of()))
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            assertThat(history(workspaceIdentifier)).hasSize(2);
            String workspaceBase = "/api/v1/workspaces/" + workspaceIdentifier;
            String wsConfigurations = workspaceBase + "/environment-configurations";
            String appConfigurations =
                    workspaceBase
                            + "/applications/"
                            + applicationIdentifier
                            + "/environment-configurations";
            String wsConfiguration =
                    post(wsConfigurations, create(globalEnvironment))
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            assertThat(history(workspaceIdentifier)).hasSize(3);
            Map<String, Object> applicationInput = create(globalEnvironment);
            if (copy) {
                applicationInput.remove("publishers");
                applicationInput.put("copyWorkspacePublishers", true);
            }
            String appConfiguration =
                    post(appConfigurations, applicationInput)
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            List<Map<String, Object>> recorded = history(workspaceIdentifier);
            assertThat(recorded).hasSize(4);
            assertThat(recorded)
                    .extracting(row -> row.get("onboarding_phase_code"))
                    .containsExactlyInAnyOrder(
                            "WORKSPACE_REGISTRATION",
                            "WORKSPACE_FIRST_ENVIRONMENT",
                            "FIRST_APPLICATION_REGISTRATION",
                            "APPLICATION_FIRST_ENVIRONMENT");
            get(workspaceBase + "/onboarding")
                    .statusCode(200)
                    .body("readyToComplete", equalTo(true));
            request().patch(workspaceBase + "/onboarding").then().statusCode(204);
            put(wsConfigurations + "/" + wsConfiguration, update(0)).statusCode(200);
            post(wsConfigurations + "/" + wsConfiguration + "/inactivate", null).statusCode(204);
            request().delete(wsConfigurations + "/" + wsConfiguration).then().statusCode(204);
            post(appConfigurations + "/" + appConfiguration + "/inactivate", null).statusCode(204);
            request().delete(appConfigurations + "/" + appConfiguration).then().statusCode(204);
            post(workspaceBase + "/inactivate", null).statusCode(204);
            post(workspaceBase + "/restore", null).statusCode(200);
            assertThat(history(workspaceIdentifier)).isEqualTo(recorded);
            assertThat(history(createRegisteredWorkspace())).hasSize(1);
        }
    }

    @Test
    void invalidUnauthorizedAndFailedConfigurationRequestsDoNotRecordPhases() {
        for (boolean app : List.of(false, true)) {
            authorization.forbidden();
            post(base(app), create(globalEnvironment)).statusCode(403);
            ownerAccess();
            post(base(app), create(foreignEnvironment)).statusCode(404);
            Map<String, Object> invalid = create(globalEnvironment);
            invalid.put("publishers", List.of());
            post(base(app), invalid).statusCode(400);
            authorization.reset();
            authorization.allow(session -> session.userName(" ").groups("PM5_OWNER"));
            post(base(app), create(globalEnvironment))
                    .statusCode(500)
                    .body("code", equalTo("ONBOARDING-0004"));
            ownerAccess();
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from "
                                            + (app ? "application" : "workspace")
                                            + "_environment_configurations",
                                    Integer.class))
                    .isZero();
        }
        assertThat(history(workspace)).isEmpty();
    }

    private String createRegisteredWorkspace() {
        return post(
                        "/api/v1/workspaces",
                        Map.of(
                                "workspaceType",
                                "MANAGER",
                                "name",
                                "Onboarding " + UUID.randomUUID(),
                                "description",
                                "Workspace onboarding integration",
                                "requester",
                                "requester",
                                "acronym",
                                "ONB",
                                "authorizerGroup",
                                "ONB_TEAM",
                                "settings",
                                Map.of(),
                                "emailGroup",
                                "workspace@example.com",
                                "approvers",
                                List.of(
                                        Map.of(
                                                "functional",
                                                "F1234",
                                                "email",
                                                "approver@example.com"))))
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    private List<Map<String, Object>> history(String identifier) {
        return jdbc.queryForList(
                "select c.onboarding_phase_code,c.requester,c.completed_at from"
                    + " workspace_onboarding_completions c join workspaces w on w.id=c.workspace_id"
                    + " where w.identifier=? order by c.onboarding_phase_code",
                identifier);
    }
}
