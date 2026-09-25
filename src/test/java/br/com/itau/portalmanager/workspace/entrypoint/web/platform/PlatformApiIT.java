package br.com.itau.portalmanager.workspace.entrypoint.web.platform;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

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
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));

        jdbc.update("""
                insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
                values
                    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                    ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into type_feature_scopes (code, label, description, sort_order, is_active, settings)
                values ('ADMINISTRATION', 'Administration', 'Administrative scope', 1, true, '{}')
                on duplicate key update code = values(code)
                """);
    }

    @Test
    void shouldAdministerServicesFeaturesAndScopes() {
        String serviceIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "audit-service",
                        "name", "Audit Service",
                        "description", "Audit owner"
                ))
                .post("/api/v1/platform/services")
                .then()
                .statusCode(201)
                .body("code", equalTo("audit-service"))
                .extract()
                .path("identifier");

        String featureIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "AUDIT",
                        "name", "Audit",
                        "description", "Audit feature",
                        "serviceIdentifier", serviceIdentifier,
                        "settings", "{}"
                ))
                .post("/api/v1/platform/features")
                .then()
                .statusCode(201)
                .body("serviceCode", equalTo("audit-service"))
                .extract()
                .path("identifier");

        authorized()
                .post("/api/v1/platform/features/" + featureIdentifier + "/scopes/ADMINISTRATION")
                .then()
                .statusCode(200);

        authorized()
                .get("/api/v1/platform/features/" + featureIdentifier + "/scopes")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].code", equalTo("ADMINISTRATION"));

        authorized()
                .patch("/api/v1/platform/features/" + featureIdentifier + "/inactivate")
                .then()
                .statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"));

        authorized()
                .patch("/api/v1/platform/features/" + featureIdentifier + "/activate")
                .then()
                .statusCode(200)
                .body("lifecycle", equalTo("ACTIVE"));

        authorized()
                .get("/api/v1/platform/features?scopeCode=ADMINISTRATION")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].code", equalTo("AUDIT"));

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Audit Platform Service", "description", "Updated"))
                .put("/api/v1/platform/services/" + serviceIdentifier)
                .then()
                .statusCode(200)
                .body("name", equalTo("Audit Platform Service"));
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "platform-api-it")
                .header("Authorization", "Bearer platform-api-it")
                .accept(ContentType.JSON);
    }
}
