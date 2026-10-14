package br.com.portalmanager.platform.workspace.entrypoint.web.publisher;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@org.springframework.test.context.TestPropertySource(properties = "platform.audit.enabled=true")
class PublisherApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @Autowired
    private SchemaResolutionPort schemas;

    @BeforeEach
    void prepare() {
        jdbc.update("delete from publishers");
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
            """
            INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
            VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                   ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                   ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')
            """
        );
        jdbc.update(
            """
            INSERT IGNORE INTO type_resource_scopes (code, label, description, sort_order, is_active, settings)
            VALUES ('WORKSPACE', 'Workspace', 'Workspace resources', 1, true, '{}'),
                   ('APPLICATION', 'Application', 'Application resources', 2, true, '{}')
            """
        );
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void createsPublisherWithJsonSettingsAndConfiguredSchemaResolution() {
        String code = "KAFKA";
        String identifier = given()
            .port(port)
            .header("correlation-id", "publisher-it")
            .header("Authorization", "Bearer publisher-it")
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    code,
                    "name",
                    "Test Publisher",
                    "description",
                    "Publisher for integration",
                    "scope",
                    "WORKSPACE",
                    "settings",
                    Map.of("topic", "events", "address", "broker:9092")
                )
            )
            .when()
            .post("/api/v1/publishers")
            .then()
            .statusCode(201)
            .body("code", equalTo(code))
            .body("lifecycle", equalTo("ACTIVE"))
            .body("settings.topic", equalTo("events"))
            .extract()
            .path("identifier");
        given()
            .port(port)
            .header("correlation-id", "publisher-it")
            .header("Authorization", "Bearer publisher-it")
            .when()
            .get("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(200)
            .body("settings.address", equalTo("broker:9092"));
        assertThat(schemas.resolve("PUBLISHER", "publisher")).isEqualTo(
            jdbc.queryForObject(
                """
                select v.definition from schema_configuration c
                join schema_versions v on v.schema_id = c.schema_id
                where c.resource_type = 'PUBLISHER' and c.resource_code = 'publisher'
                  and v.status = 'PUBLISHED'
                order by v.schema_version desc limit 1
                """,
                String.class
            )
        );
    }

    @Test
    void invalidPublisherScopeIsRejectedIndependentlyOfSchemaConfiguration() {
        String code = "KAFKA";
        given()
            .port(port)
            .header("correlation-id", "publisher-it")
            .header("Authorization", "Bearer publisher-it")
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "code",
                    code,
                    "name",
                    "Test Publisher",
                    "description",
                    "Publisher for integration",
                    "scope",
                    "INVALID",
                    "settings",
                    Map.of("topic", "events", "address", "broker:9092")
                )
            )
            .when()
            .post("/api/v1/publishers")
            .then()
            .statusCode(400)
            .body("details.field", hasItem("scope"));
    }

    private io.restassured.specification.RequestSpecification request() {
        return given()
            .port(port)
            .header("correlation-id", "publisher-review")
            .header("Authorization", "Bearer publisher-review")
            .contentType(ContentType.JSON);
    }

    private java.util.HashMap<String, Object> payload(String code) {
        return new java.util.HashMap<>(
            Map.of(
                "code",
                code,
                "name",
                " Test Publisher ",
                "description",
                "Publisher review",
                "scope",
                " workspace ",
                "settings",
                settingsFor(code)
            )
        );
    }

    private Map<String, Object> settingsFor(String code) {
        return switch (code) {
            case "KAFKA" -> Map.of("topic", "events", "address", "broker:9092");
            case "APPCONFIG" -> Map.of(
                "arn",
                "arn:aws:appconfig:region:account:application/app",
                "name",
                "config"
            );
            case "HTTP" -> Map.of("clientId", "publisher", "timeout", 30);
            default -> Map.of();
        };
    }

    private String create(String code) {
        return request()
            .body(payload(code))
            .post("/api/v1/publishers")
            .then()
            .statusCode(201)
            .extract()
            .path("identifier");
    }

    @Test
    void rejectsOriginalInvalidTypesUnknownPropertiesAndLimits() {
        for (Map.Entry<String, Object> invalid : Map.<String, Object>of(
            "name",
            123,
            "deprecated",
            "true",
            "settings",
            "{}",
            "id",
            1,
            "description",
            "x".repeat(201)
        ).entrySet()) {
            java.util.HashMap<String, Object> body = payload("KAFKA");
            body.put(invalid.getKey(), invalid.getValue());
            request().body(body).post("/api/v1/publishers").then().statusCode(400);
        }
        java.util.HashMap<String, Object> body = payload("KAFKA");
        body.put("name", "N".repeat(50));
        body.put("description", "D".repeat(200));
        request().body(body).post("/api/v1/publishers").then().statusCode(201).body("name.length()", equalTo(50));
        body.put("code", "UNSUPPORTED");
        request().body(body).post("/api/v1/publishers").then().statusCode(400);
    }

    @Test
    void validatesConcreteSettingsOnCreateAndUpdateWithoutCoercingTypes() {
        jdbc.update("delete from publishers where code in ('KAFKA','APPCONFIG','HTTP')");
        Map<String, Object> validSettings = Map.of(
            "KAFKA",
            Map.of("topic", "events", "address", "broker:9092"),
            "APPCONFIG",
            Map.of("arn", "arn:aws:appconfig:region:account:application/app", "name", "config"),
            "HTTP",
            Map.of("clientId", "publisher", "timeout", 30)
        );
        for (String code : validSettings.keySet()) {
            java.util.HashMap<String, Object> body = payload(code);
            body.put("settings", Map.of());
            request().body(body).post("/api/v1/publishers").then().statusCode(400);
            body.put("settings", validSettings.get(code));
            String identifier = request()
                .body(body)
                .post("/api/v1/publishers")
                .then()
                .statusCode(201)
                .body("settings", equalTo(validSettings.get(code)))
                .extract()
                .path("identifier");
            body.put("code", code);
            body.put("version", 0);
            body.put("settings", Map.of("unknown", true));
            request()
                .body(body)
                .put("/api/v1/publishers/" + identifier)
                .then()
                .statusCode(400);
            body.put("settings", validSettings.get(code));
            body.put("version", "0");
            request()
                .body(body)
                .put("/api/v1/publishers/" + identifier)
                .then()
                .statusCode(400);
            body.put("version", 0);
            String changedCode = code.equals("KAFKA") ? "HTTP" : "KAFKA";
            body.put("code", changedCode);
            body.put("settings", settingsFor(changedCode));
            request()
                .body(body)
                .put("/api/v1/publishers/" + identifier)
                .then()
                .statusCode(400);
            body.put("code", code);
            body.put("settings", validSettings.get(code));
            request()
                .body(body)
                .put("/api/v1/publishers/" + identifier)
                .then()
                .statusCode(200)
                .body("version", equalTo(1));
            request()
                .body(body)
                .put("/api/v1/publishers/" + identifier)
                .then()
                .statusCode(409);
        }
        java.util.HashMap<String, Object> body = payload("HTTP");
        body.put("settings", Map.of("clientId", "client", "timeout", "30"));
        request().body(body).post("/api/v1/publishers").then().statusCode(400);
    }

    @Test
    void preservesCodeUniquenessAcrossInactiveAndQuarantinedStates() {
        String code = "KAFKA";
        String identifier = create(code);
        request()
            .delete("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(400);
        request()
            .post("/api/v1/publishers/" + identifier + "/inactivate")
            .then()
            .statusCode(204);
        request().body(payload(code.toLowerCase())).post("/api/v1/publishers").then().statusCode(400);
        request()
            .post("/api/v1/publishers/" + identifier + "/restore")
            .then()
            .statusCode(200);
        request()
            .post("/api/v1/publishers/" + identifier + "/inactivate")
            .then()
            .statusCode(204);
        request()
            .delete("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(204);
        request()
            .post("/api/v1/publishers/" + identifier + "/restore")
            .then()
            .statusCode(400);
        request()
            .get("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(404);
        request().body(payload(code)).post("/api/v1/publishers").then().statusCode(400);
    }

    @Test
    void rejectsWritesWhenOwnerPolicyIsDeniedAndKeepsGlobalReadsOpen() {
        String identifier = create("KAFKA");
        authorization.reset();
        authorization.forbidden();
        request().body(payload("KAFKA")).post("/api/v1/publishers").then().statusCode(403);
        authorization.verifyCalledWithPolicy("OWNER");
        java.util.HashMap<String, Object> update = payload("KAFKA");
        update.put("version", 0);
        request()
            .body(update)
            .put("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(403);
        request()
            .post("/api/v1/publishers/" + identifier + "/restore")
            .then()
            .statusCode(403);
        request()
            .post("/api/v1/publishers/" + identifier + "/inactivate")
            .then()
            .statusCode(403);
        request()
            .delete("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(403);
        authorization.reset();
        authorization.allow(session -> session.groups("USER"));
        request()
            .get("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(200);
    }

    @Test
    void paginatesFiltersAndRejectsUnsupportedSorts() {
        create("KAFKA");
        create("HTTP");
        request()
            .queryParam("size", 1)
            .queryParam("scope", "workspace")
            .get("/api/v1/publishers")
            .then()
            .statusCode(200)
            .body("content", hasSize(1))
            .body("page.totalElements", equalTo(2));
        request()
            .queryParam("scope", "APPLICATION")
            .get("/api/v1/publishers")
            .then()
            .statusCode(200)
            .body("content", hasSize(0));
        request().queryParam("scope", "INVALID").get("/api/v1/publishers").then().statusCode(400);
        request().queryParam("sort", "settings,asc").get("/api/v1/publishers").then().statusCode(400);
        given().port(port).get("/api/v1/publishers").then().statusCode(401);
    }

    @Test
    void handlesConcurrentDuplicateCodesWithoutExposingDatabaseDetails() throws Exception {
        String code = "KAFKA";
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.function.Supplier<io.restassured.response.Response> createRequest = () -> {
            try {
                start.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return request().body(payload(code)).post("/api/v1/publishers");
        };
        java.util.concurrent.CompletableFuture<io.restassured.response.Response> first =
            java.util.concurrent.CompletableFuture.supplyAsync(createRequest);
        java.util.concurrent.CompletableFuture<io.restassured.response.Response> second =
            java.util.concurrent.CompletableFuture.supplyAsync(createRequest);
        start.countDown();
        io.restassured.response.Response firstResponse = first.get(30, java.util.concurrent.TimeUnit.SECONDS);
        io.restassured.response.Response secondResponse = second.get(30, java.util.concurrent.TimeUnit.SECONDS);
        assertThat(java.util.List.of(firstResponse.statusCode(), secondResponse.statusCode())).contains(201);
        io.restassured.response.Response rejected = firstResponse.statusCode() == 201 ? secondResponse : firstResponse;
        assertThat(rejected.statusCode()).isIn(400, 409);
        assertThat(rejected.asString()).doesNotContain("uk_publishers", "INSERT", "Duplicate entry");
        assertThat(jdbc.queryForObject("select count(*) from publishers where code=?", Long.class, code)).isEqualTo(1L);
    }

    @Test
    void persistedSchemasMatchReferencesAndColumnLimits() throws Exception {
        tools.jackson.databind.ObjectMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        tools.jackson.databind.JsonNode expected = mapper.readTree(
            new org.springframework.core.io.ClassPathResource("schemas/publisher.json").getContentAsString(
                java.nio.charset.StandardCharsets.UTF_8
            )
        );
        assertThat(mapper.readTree(schemas.resolve("PUBLISHER", "publisher"))).isEqualTo(expected);
        assertThat(
            jdbc.queryForObject(
                "select CHARACTER_MAXIMUM_LENGTH from information_schema.columns where table_schema=database() and table_name='publishers' and column_name='description'",
                Long.class
            )
        ).isEqualTo(200L);
    }

    @Test
    void auditsAllMutationsWithFinalSnapshotsAndDoesNotAuditFailures() {
        String code = "KAFKA";
        String identifier = create(code);
        java.util.HashMap<String, Object> body = payload(code);
        body.put("version", 0);
        body.put("description", "Updated description");
        request()
            .body(body)
            .put("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(200);
        request()
            .post("/api/v1/publishers/" + identifier + "/inactivate")
            .then()
            .statusCode(204);
        request()
            .post("/api/v1/publishers/" + identifier + "/restore")
            .then()
            .statusCode(200);
        request()
            .post("/api/v1/publishers/" + identifier + "/inactivate")
            .then()
            .statusCode(204);
        request()
            .delete("/api/v1/publishers/" + identifier)
            .then()
            .statusCode(204);
        request()
            .post("/api/v1/publishers/" + identifier + "/restore")
            .then()
            .statusCode(400);
        assertThat(
            jdbc.queryForList(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType')) FROM audit_outbox ORDER BY id",
                String.class
            )
        ).containsExactly(
            "PUBLISHER_CREATED",
            "PUBLISHER_UPDATED",
            "PUBLISHER_DEACTIVATED",
            "PUBLISHER_RESTORED",
            "PUBLISHER_DEACTIVATED",
            "PUBLISHER_DELETED"
        );
        assertThat(
            jdbc.queryForList(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier')) FROM audit_outbox ORDER BY id",
                String.class
            )
        ).containsOnly(identifier);
        assertThat(
            jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='PUBLISHER_DELETED'",
                String.class
            )
        ).isEqualTo("QUARANTINED");
    }
}
