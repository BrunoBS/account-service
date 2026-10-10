package br.com.portalmanager.platform.workspace.entrypoint.web.platform;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class PlatformApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        SchemaDefaultFixture.seed(jdbc);
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));

        jdbc.update(
            """
            insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
            values
                ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')
            on duplicate key update code = values(code)
            """
        );
    }

    @Test
    void shouldAdministerServicesFeaturesAndContexts() {
        String microserviceIdentifier = authorized()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    "portal-manager",
                    "name",
                    "Portal Manager",
                    "description",
                    "Portal owner",
                    "settings",
                    Map.of()
                )
            )
            .post("/api/v1/platform/microservices")
            .then()
            .statusCode(201)
            .body("name", equalTo("Portal Manager"))
            .body("settings", equalTo(Map.of()))
            .extract()
            .path("identifier");

        String contextIdentifier = authorized()
            .contentType(ContentType.JSON)
            .body(
                Map.of("code", "manager-account", "name", "Manager Account", "description", "Manager account context")
            )
            .post("/api/v1/platform/contexts")
            .then()
            .statusCode(201)
            .body("lifecycle", equalTo("ACTIVE"))
            .extract()
            .path("identifier");

        String featureIdentifier = authorized()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    "application",
                    "name",
                    "Application",
                    "description",
                    "Application feature",
                    "microserviceIdentifier",
                    microserviceIdentifier,
                    "settings",
                    Map.of()
                )
            )
            .post("/api/v1/platform/features")
            .then()
            .statusCode(201)
            .body("microserviceCode", equalTo("portal-manager"))
            .body("settings", equalTo(Map.of()))
            .extract()
            .path("identifier");

        String batchContextIdentifier = authorized()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    "batch-context",
                    "name",
                    "Batch context",
                    "featureIdentifiers",
                    List.of(featureIdentifier, featureIdentifier)
                )
            )
            .post("/api/v1/platform/contexts")
            .then()
            .statusCode(201)
            .extract()
            .path("identifier");
        authorized()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Batch updated"))
            .put("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(200);
        authorized()
            .get("/api/v1/platform/features?contextCode=batch-context")
            .then()
            .statusCode(200)
            .body("$", hasSize(1));
        authorized()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Should rollback", "featureIdentifiers", List.of(UUID.randomUUID().toString())))
            .put("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(404);
        authorized()
            .get("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(200)
            .body("name", equalTo("Batch updated"));
        authorized()
            .get("/api/v1/platform/features?contextCode=batch-context")
            .then()
            .statusCode(200)
            .body("$", hasSize(1));
        authorized()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Batch updated", "featureIdentifiers", List.of()))
            .put("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(200);
        authorized()
            .get("/api/v1/platform/features?contextCode=batch-context")
            .then()
            .statusCode(200)
            .body("$", hasSize(0));
        authorized()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Batch updated", "featureIdentifiers", List.of(featureIdentifier)))
            .put("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(200);
        authorized()
            .contentType(ContentType.JSON)
            .body(Map.of("name", "Batch updated", "featureIdentifiers", List.of()))
            .put("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(200);
        authorized()
            .delete("/api/v1/platform/contexts/" + batchContextIdentifier)
            .then()
            .statusCode(204);

        authorized()
            .post("/api/v1/platform/features/" + featureIdentifier + "/contexts/" + contextIdentifier)
            .then()
            .statusCode(200);

        authorized()
            .post("/api/v1/platform/features/" + featureIdentifier + "/contexts/" + contextIdentifier)
            .then()
            .statusCode(200);
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM platform_feature_context_relations relation JOIN platform_features feature ON feature.id = relation.feature_id JOIN platform_feature_contexts context ON context.id = relation.feature_context_id WHERE feature.identifier = ? AND context.identifier = ?",
                Integer.class,
                featureIdentifier,
                contextIdentifier
            )
        ).isEqualTo(1);
        authorized()
            .delete("/api/v1/platform/contexts/" + contextIdentifier)
            .then()
            .statusCode(400);

        authorized()
            .get("/api/v1/platform/features/" + featureIdentifier + "/contexts")
            .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].code", equalTo("manager-account"));

        authorized()
            .get("/api/v1/platform/features?contextCode=manager-account")
            .then()
            .statusCode(200)
            .body("$", hasSize(1))
            .body("[0].code", equalTo("application"));

        authorized()
            .patch("/api/v1/platform/contexts/" + contextIdentifier + "/inactivate")
            .then()
            .statusCode(200)
            .body("lifecycle", equalTo("INACTIVE"));

        authorized()
            .patch("/api/v1/platform/contexts/" + contextIdentifier + "/activate")
            .then()
            .statusCode(200)
            .body("lifecycle", equalTo("ACTIVE"));

        authorized()
            .delete("/api/v1/platform/features/" + featureIdentifier + "/contexts/" + contextIdentifier)
            .then()
            .statusCode(200);

        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM platform_feature_context_relations relation JOIN platform_features feature ON feature.id = relation.feature_id WHERE feature.identifier = ?",
                Integer.class,
                featureIdentifier
            )
        ).isZero();
        authorized()
            .get("/api/v1/platform/features/" + featureIdentifier + "/contexts")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0));

        authorized()
            .delete("/api/v1/platform/contexts/" + contextIdentifier)
            .then()
            .statusCode(204);
    }

    @Test
    void rejectsMalformedFeatureSettingsBeforePersistence() {
        String microserviceIdentifier = authorized()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    "settings-service",
                    "name",
                    "Settings Service",
                    "description",
                    "Schema validation test",
                    "settings",
                    Map.of()
                )
            )
            .post("/api/v1/platform/microservices")
            .then()
            .statusCode(201)
            .extract()
            .path("identifier");
        authorized()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    "invalid-settings-feature",
                    "name",
                    "Invalid Settings",
                    "microserviceIdentifier",
                    microserviceIdentifier,
                    "settings",
                    "not-json"
                )
            )
            .post("/api/v1/platform/features")
            .then()
            .statusCode(400)
            .body("details.field", hasItem("settings"));
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
            .port(port)
            .header("correlation-id", "platform-api-it")
            .header("Authorization", "Bearer platform-api-it")
            .accept(ContentType.JSON);
    }
}
