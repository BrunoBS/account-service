package br.com.itau.portalmanager.workspace.entrypoint.web.catalog;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class CatalogApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldExposeCompleteCrudForStandardAndDynamicCatalogs() {
        List<CatalogCase> catalogs = List.of(
                new CatalogCase("/api/v1/application-scope-type", "BACKEND"),
                new CatalogCase("/api/v1/authorization-type", "DEV"),
                new CatalogCase("/api/v1/environment-type", "DEFAULT"),
                new CatalogCase("/api/v1/feature-scope", "WORKSPACE"),
                new CatalogCase("/api/v1/feature-type", "MENU"),
                new CatalogCase("/api/v1/infrastructure-type", "VM"),
                new CatalogCase("/api/v1/language-type", "JAVA"),
                new CatalogCase("/api/v1/lifecycle-type", "ACTIVE"),
                new CatalogCase("/api/v1/onboarding-type", "WORKSPACE_REGISTRATION"),
                new CatalogCase("/api/v1/tag-origin-type", "MANUAL"),
                new CatalogCase("/api/v1/visibility-type", "PRIVATE"),
                new CatalogCase("/api/v1/publisher-scope-type", "WORKSPACE"),
                new CatalogCase("/api/v1/schema-scope", "PLATFORM"),
                new CatalogCase("/api/v1/schema-type", "WORKSPACE"),
                new CatalogCase("/api/v1/share-status-type", "NOT_REQUESTED")
        );

        int order = 1;
        for (CatalogCase catalog : catalogs) {
            Map<String, Object> create = standardBody(
                    catalog.code(),
                    catalog.code() + " label",
                    "Descrição válida de " + catalog.code(),
                    order++
            );

            String code = post(catalog.path(), create)
                    .statusCode(201)
                    .body("code", equalTo(catalog.code()))
                    .extract()
                    .path("code");

            get(catalog.path() + "/" + code)
                    .statusCode(200)
                    .body("code", equalTo(code));

            Map<String, Object> update = standardBody(
                    catalog.code(),
                    catalog.code() + " atualizado",
                    "Descrição atualizada de " + catalog.code(),
                    order++
            );

            put(catalog.path() + "/" + code, update)
                    .statusCode(200)
                    .body("code", equalTo(code))
                    .body("label", equalTo(catalog.code() + " atualizado"));

            delete(catalog.path() + "/" + code).statusCode(204);

            post(catalog.path() + "/" + code + "/restore")
                    .statusCode(200)
                    .body("code", equalTo(code))
                    .body("code", equalTo(catalog.code()));
        }
    }

    @Test
    void shouldPersistCatalogSpecificConfigurationInSettings() {
        Map<String, Object> schema = standardBody(
                "APPLICATION",
                "Application",
                "Descrição válida de schema application",
                1
        );
        schema.put("settings", Map.of("scopes", "WORKSPACE"));

        String schemaCode = post("/api/v1/schema-type", schema)
                .statusCode(201)
                .body("code", equalTo("APPLICATION"))
                .body("settings.scopes", equalTo("WORKSPACE"))
                .extract()
                .path("code");

        get("/api/v1/schema-type/" + schemaCode)
                .statusCode(200)
                .body("settings.scopes", equalTo("WORKSPACE"));

        Map<String, Object> feature = standardBody(
                "ROUTE",
                "Route",
                "Descrição válida da feature route",
                2
        );
        feature.put("settings", Map.of(
                "scopes", "APPLICATION",
                "available", true
        ));

        post("/api/v1/feature-type", feature)
                .statusCode(201)
                .body("settings.scopes", equalTo("APPLICATION"))
                .body("settings.available", equalTo(true));

        Map<String, Object> onboarding = standardBody(
                "WORKSPACE_FIRST_ENVIRONMENT",
                "Workspace first environment",
                "Descrição válida da fase de onboarding",
                3
        );
        onboarding.put("settings", Map.of("orientation", "Orientação inicial"));

        post("/api/v1/onboarding-type", onboarding)
                .statusCode(201)
                .body("settings.orientation", equalTo("Orientação inicial"));
    }

    @Test
    void shouldRejectUnknownEnumNameAndInvalidSettingsShape() {
        post(
                "/api/v1/workspace-type",
                standardBody("UNKNOWN", "Unknown", "Descrição válida de tipo desconhecido", 1)
        )
                .statusCode(400)
                .body("details.field", hasItem("code"));

        Map<String, Object> invalidSettings = standardBody(
                "MANAGER",
                "Manager",
                "Descrição válida de manager",
                2
        );
        invalidSettings.put("settings", Map.of("nested", Map.of("not", "scalar")));

        post("/api/v1/workspace-type", invalidSettings)
                .statusCode(400)
                .body("details.field", hasItem("settings"));
    }

    @Test
    void shouldRequireOwnerAuthorizationForCatalogAdministration() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("USER"));

        post(
                "/api/v1/application-scope-type",
                standardBody("BACKEND", "Backend", "Descrição válida de backend", 1)
        ).statusCode(201);

        authorizationMock.verifyCalledWithPolicy("OWNER");
    }

    private Map<String, Object> standardBody(
            String code,
            String label,
            String description,
            Integer sortOrder
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("label", label);
        body.put("description", description);
        body.put("sortOrder", sortOrder);
        body.put("settings", Map.of());
        return body;
    }

    private ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private ValidatableResponse post(String path, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(path)
                .then();
    }

    private ValidatableResponse post(String path) {
        return authorized()
                .when()
                .post(path)
                .then();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(path)
                .then();
    }

    private ValidatableResponse delete(String path) {
        return authorized()
                .when()
                .delete(path)
                .then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "catalog-api-it")
                .header("Authorization", "Bearer catalog-api-it")
                .accept(ContentType.JSON);
    }

    private record CatalogCase(String path, String code) {
    }
}
