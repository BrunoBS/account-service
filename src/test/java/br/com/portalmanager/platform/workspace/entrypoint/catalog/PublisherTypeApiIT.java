package br.com.portalmanager.platform.workspace.entrypoint.catalog;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;

class PublisherTypeApiIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthorizationMock authorization;

    @BeforeEach
    void setup() {
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(s -> s.groups("PM5_OWNER"));
    }

    @Test
    void usesTheGenericCatalogContractWithCodeIdentityAndSoftDelete() {
        for (String code : List.of("WEB_SOCKET", "KAFKA", "APP_CONFIG")) {
            Map<String, Object> body =
                    Map.of(
                            "code",
                            code,
                            "label",
                            code,
                            "description",
                            "Publisher catalog definition",
                            "sortOrder",
                            1,
                            "settings",
                            Map.of(
                                    "scope",
                                    code.equals("APP_CONFIG") ? "APPLICATION" : "WORKSPACE"));
            given().port(port)
                    .header("Authorization", "Bearer test")
                    .header("correlation-id", "publisher-catalog-it")
                    .contentType(ContentType.JSON)
                    .body(body)
                    .post("/api/v1/publishers")
                    .then()
                    .statusCode(201)
                    .body("code", equalTo(code))
                    .body("identifier", nullValue());
            given().port(port)
                    .header("Authorization", "Bearer test")
                    .header("correlation-id", "publisher-catalog-it")
                    .get("/api/v1/publishers/" + code)
                    .then()
                    .statusCode(200);
            given().port(port)
                    .header("Authorization", "Bearer test")
                    .header("correlation-id", "publisher-catalog-it")
                    .delete("/api/v1/publishers/" + code)
                    .then()
                    .statusCode(204);
            given().port(port)
                    .header("Authorization", "Bearer test")
                    .header("correlation-id", "publisher-catalog-it")
                    .get("/api/v1/publishers/" + code)
                    .then()
                    .statusCode(404);
            given().port(port)
                    .header("Authorization", "Bearer test")
                    .header("correlation-id", "publisher-catalog-it")
                    .post("/api/v1/publishers/" + code + "/restore")
                    .then()
                    .statusCode(200);
        }
        assertThat(jdbc.queryForList("select code from publishers order by code", String.class))
                .containsExactly("APP_CONFIG", "KAFKA", "WEB_SOCKET");
    }

    @Test
    void requiresOwnerForCatalogReads() {
        authorization.forbidden();
        given().port(port)
                .header("Authorization", "Bearer test")
                .header("correlation-id", "catalog-denied")
                .get("/api/v1/publishers")
                .then()
                .statusCode(403);
    }

    @Test
    void rejectsCodesOutsideTheEnum() {
        given().port(port)
                .header("Authorization", "Bearer test")
                .header("correlation-id", "publisher-catalog-it")
                .contentType(ContentType.JSON)
                .body(Map.of("code", "HTTP", "label", "Legacy", "description", "Legacy publisher definition", "settings", Map.of()))
                .post("/api/v1/publishers")
                .then()
                .statusCode(400);
    }
}
