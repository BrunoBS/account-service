package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.*;

class EnvironmentConfigurationAuditIT extends EnvironmentConfigurationHttpFixture {

    @Test
    void auditsSnapshotsAndFlushedVersionsForBothAggregates() {
        for (boolean app : List.of(false, true)) {
            String type = app ? "APPLICATION" : "WORKSPACE";
            String id =
                    post(base(app), create(globalEnvironment))
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            put(base(app) + "/" + id, update(0)).statusCode(200);
            post(base(app) + "/" + id + "/inactivate", null).statusCode(204);
            request().delete(base(app) + "/" + id).then().statusCode(204);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from audit_outbox where"
                                        + " json_unquote(json_extract(metadata,'$.resourceIdentifier'))=?",
                                    Integer.class,
                                    id))
                    .isEqualTo(4);
            String predicate =
                    "json_unquote(json_extract(metadata,'$.eventType'))=? and"
                            + " json_unquote(json_extract(metadata,'$.resourceIdentifier'))=?";
            assertThat(
                            jdbc.queryForObject(
                                    "select json_extract(payload,'$.version') from audit_outbox"
                                            + " where "
                                            + predicate,
                                    Integer.class,
                                    type + "_ENVIRONMENT_CONFIGURATION_QUARANTINED",
                                    id))
                    .isEqualTo(3);
            assertThat(
                            jdbc.queryForObject(
                                    "select json_unquote(json_extract(payload,'$.lifecycle')) from"
                                            + " audit_outbox where "
                                            + predicate,
                                    String.class,
                                    type + "_ENVIRONMENT_CONFIGURATION_QUARANTINED",
                                    id))
                    .isEqualTo("QUARANTINED");
        }
    }

    @Test
    void auditFailureRollsBackCreationReplacementAndLifecycle() {
        for (boolean app : List.of(false, true)) {
            String id =
                    post(base(app), create(globalEnvironment))
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            Mockito.doThrow(new IllegalStateException("Controlled audit test failure"))
                    .when(auditAppender)
                    .append(ArgumentMatchers.any(), ArgumentMatchers.any());
            post(base(app), create(ownEnvironment)).statusCode(500);
            Map<String, Object> replacement = update(0);
            replacement.put("publishers", List.of(publisher(appconfig, 1)));
            if (!app) replacement.put("publishers", List.of(publisher(http, 3)));
            put(base(app) + "/" + id, replacement).statusCode(500);
            post(base(app) + "/" + id + "/inactivate", null).statusCode(500);
            get(base(app) + "/" + id)
                    .statusCode(200)
                    .body("version", equalTo(0))
                    .body("lifecycle", equalTo("ACTIVE"))
                    .body("publishers.publisherCode", contains(kafka, http));
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from "
                                            + (app ? "application" : "workspace")
                                            + "_environment_configurations",
                                    Integer.class))
                    .isEqualTo(1);
            jdbc.update(
                    "update "
                            + (app ? "application" : "workspace")
                            + "_environment_configurations set lifecycle_code='INACTIVE' where"
                            + " identifier=?",
                    id);
            request().delete(base(app) + "/" + id).then().statusCode(500);
            get(base(app) + "/" + id).statusCode(200).body("lifecycle", equalTo("INACTIVE"));
            Mockito.reset(auditAppender);
        }
    }

    @Test
    void auditFailureOfTheFirstConfigurationRollsBackItsOnboardingPhase() {
        Mockito.doThrow(new IllegalStateException("Controlled audit failure"))
                .when(auditAppender)
                .append(ArgumentMatchers.any(), ArgumentMatchers.any());
        for (boolean app : List.of(false, true)) {
            post(base(app), create(globalEnvironment)).statusCode(500);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from "
                                            + (app ? "application" : "workspace")
                                            + "_environment_configurations",
                                    Integer.class))
                    .isZero();
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from workspace_onboarding_completions where"
                                        + " workspace_id=?",
                                Integer.class,
                                workspaceId()))
                .isZero();
    }
}
