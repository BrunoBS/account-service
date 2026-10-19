package br.com.portalmanager.platform.workspace.entrypoint.servicecontext;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class ServiceContextApiIT {

    @LocalServerPort private int port;

    @Autowired private AuthorizationMock authorizationMock;

    @Autowired private JdbcTemplate jdbc;

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
                """);
    }

    @Test
    void shouldAdministerServicesFeaturesAndContexts() {
        String microserviceIdentifier =
                authorized()
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
                                        Map.of()))
                        .post("/api/v1/service-contexts/microservices")
                        .then()
                        .statusCode(201)
                        .body("name", equalTo("Portal Manager"))
                        .body("settings", equalTo(Map.of()))
                        .extract()
                        .path("identifier");

        String featureGroupIdentifier =
                authorized()
                        .contentType(ContentType.JSON)
                        .body(
                                Map.of(
                                        "code",
                                        "manager-account",
                                        "name",
                                        "Manager Account",
                                        "description",
                                        "Manager account group"))
                        .post("/api/v1/service-contexts/feature-groups")
                        .then()
                        .statusCode(201)
                        .body("lifecycle", equalTo("ACTIVE"))
                        .extract()
                        .path("identifier");

        String featureIdentifier =
                authorized()
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
                                        Map.of()))
                        .post("/api/v1/service-contexts/features")
                        .then()
                        .statusCode(201)
                        .body("microserviceCode", equalTo("portal-manager"))
                        .body("settings", equalTo(Map.of()))
                        .extract()
                        .path("identifier");

        String batchGroupIdentifier =
                authorized()
                        .contentType(ContentType.JSON)
                        .body(Map.of("code", "batch-group", "name", "Batch group"))
                        .post("/api/v1/service-contexts/feature-groups")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String link =
                "/api/v1/service-contexts/feature-groups/"
                        + batchGroupIdentifier
                        + "/features/"
                        + featureIdentifier;
        authorized().post(link).then().statusCode(200);
        authorized().post(link).then().statusCode(200);
        authorized()
                .contentType(ContentType.JSON)
                .body(contextUpdate(batchGroupIdentifier, Map.of("name", "Updated group")))
                .put("/api/v1/service-contexts/feature-groups/" + batchGroupIdentifier)
                .then()
                .statusCode(200);
        authorized()
                .queryParam("featureGroupIdentifier", batchGroupIdentifier)
                .get("/api/v1/service-contexts/features")
                .then()
                .statusCode(200)
                .body("content", hasSize(1));
        authorized().delete(link).then().statusCode(204);
        authorized()
                .patch("/api/v1/service-contexts/feature-groups/" + batchGroupIdentifier + "/inactivate")
                .then()
                .statusCode(200);
        authorized()
                .delete("/api/v1/service-contexts/feature-groups/" + batchGroupIdentifier)
                .then()
                .statusCode(204);

        authorized()
                .post(
                        "/api/v1/service-contexts/feature-groups/"
                                + featureGroupIdentifier
                                + "/features/"
                                + featureIdentifier)
                .then()
                .statusCode(200);

        authorized()
                .post(
                        "/api/v1/service-contexts/feature-groups/"
                                + featureGroupIdentifier
                                + "/features/"
                                + featureIdentifier)
                .then()
                .statusCode(200);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature relation JOIN"
                                        + " service_context_features feature ON feature.id ="
                                        + " relation.feature_id JOIN feature_group"
                                        + " grp ON grp.id = relation.feature_group_id WHERE"
                                        + " feature.identifier = ? AND grp.identifier = ?",
                                Integer.class,
                                featureIdentifier,
                                featureGroupIdentifier))
                .isEqualTo(1);
        authorized()
                .delete("/api/v1/service-contexts/feature-groups/" + featureGroupIdentifier)
                .then()
                .statusCode(400);

        authorized()
                .get("/api/v1/service-contexts/feature-groups?featureIdentifier=" + featureIdentifier)
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].code", equalTo("manager-account"));

        authorized()
                .queryParam("featureGroupIdentifier", featureGroupIdentifier)
                .get("/api/v1/service-contexts/features")
                .then()
                .statusCode(200)
                .body("content", hasSize(1))
                .body("content[0].code", equalTo("application"));

        authorized()
                .patch("/api/v1/service-contexts/feature-groups/" + featureGroupIdentifier + "/inactivate")
                .then()
                .statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"));

        authorized()
                .patch("/api/v1/service-contexts/feature-groups/" + featureGroupIdentifier + "/activate")
                .then()
                .statusCode(200)
                .body("lifecycle", equalTo("ACTIVE"));

        authorized()
                .delete(
                        "/api/v1/service-contexts/feature-groups/"
                                + featureGroupIdentifier
                                + "/features/"
                                + featureIdentifier)
                .then()
                .statusCode(204);

        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature relation JOIN"
                                        + " service_context_features feature ON feature.id ="
                                        + " relation.feature_id WHERE feature.identifier = ?",
                                Integer.class,
                                featureIdentifier))
                .isZero();
        authorized()
                .get("/api/v1/service-contexts/feature-groups?featureIdentifier=" + featureIdentifier)
                .then()
                .statusCode(200)
                .body("content.size()", equalTo(0));

        authorized()
                .patch("/api/v1/service-contexts/feature-groups/" + featureGroupIdentifier + "/inactivate")
                .then()
                .statusCode(200);
        authorized()
                .delete("/api/v1/service-contexts/feature-groups/" + featureGroupIdentifier)
                .then()
                .statusCode(204);
    }

    @Test
    void rejectsMalformedFeatureSettingsBeforePersistence() {
        String microserviceIdentifier =
                authorized()
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
                                        Map.of()))
                        .post("/api/v1/service-contexts/microservices")
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
                                "not-json"))
                .post("/api/v1/service-contexts/features")
                .then()
                .statusCode(400)
                .body("details.field", hasItem("settings"));
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given().port(port)
                .header("correlation-id", "platform-api-it")
                .header("Authorization", "Bearer platform-api-it")
                .accept(ContentType.JSON);
    }

    private Map<String, Object> contextUpdate(String featureGroupIdentifier, Map<String, Object> changes) {
        Map<String, Object> current =
                authorized()
                        .get("/api/v1/service-contexts/feature-groups/" + featureGroupIdentifier)
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(new io.restassured.common.mapper.TypeRef<Map<String, Object>>() {});
        Map<String, Object> body = new java.util.HashMap<>(changes);
        body.put("version", current.get("version"));
        body.put("code", current.get("code"));
        return body;
    }
}
