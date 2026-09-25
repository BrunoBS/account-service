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
    }

    @Test
    void shouldAdministerServicesFeaturesAndContexts() {
        String serviceIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "PORTAL_MANAGER",
                        "name", "Portal Manager",
                        "description", "Portal owner"
                ))
                .post("/api/v1/platform/services")
                .then()
                .statusCode(201)
                .body("name", equalTo("Portal Manager"))
                .extract()
                .path("identifier");

        String contextIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "MANAGER_ACCOUNT",
                        "name", "Manager Account",
                        "description", "Manager account context"
                ))
                .post("/api/v1/platform/contexts")
                .then()
                .statusCode(201)
                .body("lifecycle", equalTo("ACTIVE"))
                .extract()
                .path("identifier");

        String featureIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "APPLICATION",
                        "name", "Application",
                        "description", "Application feature",
                        "serviceIdentifier", serviceIdentifier,
                        "settings", "{}"
                ))
                .post("/api/v1/platform/features")
                .then()
                .statusCode(201)
                .body("serviceCode", equalTo("PORTAL_MANAGER"))
                .extract()
                .path("identifier");

        authorized()
                .post("/api/v1/platform/features/" + featureIdentifier + "/contexts/" + contextIdentifier)
                .then()
                .statusCode(200);

        authorized()
                .get("/api/v1/platform/features/" + featureIdentifier + "/contexts")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].code", equalTo("MANAGER_ACCOUNT"));

        authorized()
                .get("/api/v1/platform/features?contextCode=MANAGER_ACCOUNT")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].code", equalTo("APPLICATION"));

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

        authorized()
                .delete("/api/v1/platform/contexts/" + contextIdentifier)
                .then()
                .statusCode(204);
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "platform-api-it")
                .header("Authorization", "Bearer platform-api-it")
                .accept(ContentType.JSON);
    }
}
