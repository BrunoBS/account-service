package br.com.itau.portalmanager.workspace.input.web.catalog;

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
import static org.hamcrest.Matchers.notNullValue;

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
                new CatalogCase("/api/v1/workspace-type", "ADMIN"),
                new CatalogCase("/api/v1/application-scope-type", "BACKEND"),
                new CatalogCase("/api/v1/authorization-type", "DEV"),
                new CatalogCase("/api/v1/environment-type", "DEFAULT"),
                new CatalogCase("/api/v1/feature-scope", "WORKSPACE"),
                new CatalogCase("/api/v1/infrastructure-type", "VM"),
                new CatalogCase("/api/v1/language-type", "JAVA"),
                new CatalogCase("/api/v1/lifecycle-type", "ACTIVE"),
                new CatalogCase("/api/v1/tag-origin-type", "MANUAL"),
                new CatalogCase("/api/v1/visibility-type", "PRIVATE"),
                new CatalogCase("/api/v1/publisher-scope-type", "WORKSPACE"),
                new CatalogCase("/api/v1/schema-scope", "PLATFORM"),
                new CatalogCase("/api/v1/share-status-type", "NOT_REQUESTED")
        );

        int order = 1;
        for (CatalogCase catalog : catalogs) {
            Map<String, Object> create = standardBody(
                    catalog.name(),
                    catalog.name() + " label",
                    "Descrição válida de " + catalog.name(),
                    order++
            );

            Integer id = post(catalog.path(), create)
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("name", equalTo(catalog.name()))
                    .extract()
                    .path("id");

            get(catalog.path() + "/" + id)
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("name", equalTo(catalog.name()));

            Map<String, Object> update = standardBody(
                    catalog.name(),
                    catalog.name() + " atualizado",
                    "Descrição atualizada de " + catalog.name(),
                    order++
            );

            put(catalog.path() + "/" + id, update)
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("label", equalTo(catalog.name() + " atualizado"));

            delete(catalog.path() + "/" + id).statusCode(204);

            post(catalog.path() + "/" + id + "/restore")
                    .statusCode(200)
                    .body("id", equalTo(id))
                    .body("name", equalTo(catalog.name()));
        }
    }

    @Test
    void shouldPreserveFeatureTypeAdvancedCrud() {
        Integer scopeId = post(
                "/api/v1/feature-scope",
                standardBody("APPLICATION", "Application", "Escopo de aplicação", 1)
        ).statusCode(201).extract().path("id");

        Map<String, Object> feature = standardBody(
                "MENU",
                "Menu",
                "Descrição válida da feature menu",
                1
        );
        feature.put("featureScopeId", scopeId);
        feature.put("featureScopeName", "APPLICATION");
        feature.put("available", true);

        Integer id = post("/api/v1/feature-type", feature)
                .statusCode(201)
                .body("featureScopeId", equalTo(scopeId))
                .body("featureScopeName", equalTo("APPLICATION"))
                .body("available", equalTo(true))
                .extract()
                .path("id");

        get("/api/v1/feature-type?featureScopeName=APPLICATION&available=true")
                .statusCode(200)
                .body("name", hasItem("MENU"));

        feature.put("label", "Menu atualizado");
        feature.put("available", false);

        put("/api/v1/feature-type/" + id, feature)
                .statusCode(200)
                .body("label", equalTo("Menu atualizado"))
                .body("available", equalTo(false));

        delete("/api/v1/feature-type/" + id).statusCode(204);
        post("/api/v1/feature-type/" + id + "/restore").statusCode(200);
    }

    @Test
    void shouldPreserveSchemaTypeAdvancedCrud() {
        post(
                "/api/v1/schema-scope",
                standardBody("WORKSPACE", "Workspace", "Escopo de schema do workspace", 1)
        ).statusCode(201);

        Map<String, Object> schema = standardBody(
                "WORKSPACE",
                "Workspace",
                "Descrição válida do schema workspace",
                1
        );
        schema.put("scope", "WORKSPACE");

        Integer id = post("/api/v1/schema-type", schema)
                .statusCode(201)
                .body("scope", equalTo("WORKSPACE"))
                .extract()
                .path("id");

        get("/api/v1/schema-type?scope=WORKSPACE")
                .statusCode(200)
                .body("name", hasItem("WORKSPACE"));

        schema.put("label", "Workspace atualizado");
        put("/api/v1/schema-type/" + id, schema)
                .statusCode(200)
                .body("label", equalTo("Workspace atualizado"))
                .body("scope", equalTo("WORKSPACE"));

        delete("/api/v1/schema-type/" + id).statusCode(204);
        post("/api/v1/schema-type/" + id + "/restore").statusCode(200);
    }

    @Test
    void shouldPreserveOnboardingAdvancedCrud() {
        Map<String, Object> onboarding = standardBody(
                "WORKSPACE_REGISTRATION",
                "Workspace registration",
                "Descrição válida da fase de onboarding",
                1
        );
        onboarding.put("orientation", "Orientação inicial");

        Integer id = post("/api/v1/onboarding-type", onboarding)
                .statusCode(201)
                .body("orientation", equalTo("Orientação inicial"))
                .extract()
                .path("id");

        onboarding.put("label", "Workspace registration updated");
        onboarding.put("orientation", "Nova orientação");

        put("/api/v1/onboarding-type/" + id, onboarding)
                .statusCode(200)
                .body("orientation", equalTo("Nova orientação"));

        delete("/api/v1/onboarding-type/" + id).statusCode(204);
        post("/api/v1/onboarding-type/" + id + "/restore")
                .statusCode(200)
                .body("orientation", equalTo("Nova orientação"));
    }

    @Test
    void shouldRejectUnknownEnumNameAndInvalidSettingsShape() {
        post(
                "/api/v1/workspace-type",
                standardBody("UNKNOWN", "Unknown", "Descrição válida de tipo desconhecido", 1)
        )
                .statusCode(400)
                .body("details.field", hasItem("name"));

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
                "/api/v1/workspace-type",
                standardBody("CATALOG", "Catalog", "Descrição válida de catalog", 1)
        ).statusCode(201);

        authorizationMock.verifyCalledWithPolicy("OWNER");
    }

    private Map<String, Object> standardBody(
            String name,
            String label,
            String description,
            Integer sortOrder
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
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

    private record CatalogCase(String path, String name) {
    }
}
