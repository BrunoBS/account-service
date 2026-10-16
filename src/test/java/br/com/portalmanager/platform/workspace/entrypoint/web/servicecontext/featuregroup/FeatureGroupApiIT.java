package br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.featuregroup;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@TestPropertySource(properties = "platform.audit.enabled=true")
class FeatureGroupApiIT {
    private static final String ROUTE = "/api/v1/service-contexts/feature-groups";
    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @MockitoSpyBean private AuditOutboxAppender appender;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings)"
                        + " VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update(
                "INSERT INTO service_context_microservices"
                    + " (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES"
                    + " (?,'parent-service','Parent service','{}','ACTIVE',NOW(6),NOW(6))",
                MICROSERVICE);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        reset(appender);
    }

    @Test
    void normalizesOriginalJsonAndCreatesTheAuditedRepresentation() {
        String feature = feature("accounts");
        Map<String, Object> body = payload("  ACCOUNT-GROUP  ", "  Account group  ");
        body.put("description", "  Description  ");
        String identifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post(ROUTE)
                        .then()
                        .statusCode(201)
                        .body("code", equalTo("account-group"))
                        .body("name", equalTo("Account group"))
                        .body("description", equalTo("Description"))
                        .body("version", equalTo(0))
                        .extract()
                        .path("identifier");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature",
                                Integer.class))
                .isZero();
        assertSnapshot("FEATURE_GROUP_CREATED", identifier, "ACTIVE", 0);
    }

    @Test
    void rejectsIncorrectTypesUnknownFieldsAndMalformedIdentifiersBeforeCoercion() {
        String identifier = create("review-group", "Review group");
        List<Map<String, Object>> invalid = new ArrayList<>();
        for (Map<String, Object> change :
                List.<Map<String, Object>>of(
                        Map.of("code", 123),
                        Map.of("name", true),
                        Map.of("description", 123),
                        Map.of("featureIdentifiers", List.of(123)),
                        Map.of("featureIdentifiers", List.of("not-a-uuid")),
                        Map.of(
                                "featureIdentifiers",
                                List.of(Map.of("identifier", UUID.randomUUID().toString()))),
                        Map.of("featureIdentifiers", "not-an-array"),
                        Map.of("identifier", UUID.randomUUID().toString()),
                        Map.of("lifecycle", "ACTIVE"),
                        Map.of("version", "0"),
                        Map.of("version", 0.5))) {
            Map<String, Object> body = payload("new-group", "New group");
            body.putAll(change);
            invalid.add(body);
        }
        for (Map<String, Object> body : invalid) {
            request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(400);
            Map<String, Object> update = payload("review-group", "Changed group");
            update.put("version", 0);
            body.forEach(
                    (key, value) -> {
                        if (!key.equals("code") && !key.equals("name")) update.put(key, value);
                    });
            if (!(body.get("code") instanceof String)) update.put("code", body.get("code"));
            if (!(body.get("name") instanceof String)) update.put("name", body.get("name"));
            request()
                    .contentType(ContentType.JSON)
                    .body(update)
                    .put(ROUTE + "/" + identifier)
                    .then()
                    .statusCode(400);
        }
        request().contentType(ContentType.JSON).body("null").post(ROUTE).then().statusCode(400);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void enforcesColumnLimitsAndRejectsAssociationsInTheGroupContract() {
        Map<String, Object> body = payload("a".repeat(50), "n".repeat(100));
        body.put("description", "d".repeat(500));
        request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(201);
        for (Map<String, Object> change :
                List.<Map<String, Object>>of(
                        Map.of("code", "b".repeat(51)),
                        Map.of("name", "m".repeat(101)),
                        Map.of("description", "e".repeat(501)),
                        Map.of("name", " "),
                        Map.of(
                                "featureIdentifiers",
                                java.util.Collections.nCopies(
                                        101, UUID.randomUUID().toString())))) {
            Map<String, Object> invalid = payload("new-group", "New group");
            invalid.putAll(change);
            request()
                    .contentType(ContentType.JSON)
                    .body(invalid)
                    .post(ROUTE)
                    .then()
                    .statusCode(400);
        }
    }

    @Test
    void requiresTheCurrentVersionAndImmutableCodeOnUpdateAndRejectsVersionOnCreate() {
        Map<String, Object> body = payload("review-group", "Review group");
        body.put("version", 0);
        request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(400);
        String identifier = create("review-group", "Review group");
        body.remove("version");
        request()
                .contentType(ContentType.JSON)
                .body(body)
                .put(ROUTE + "/" + identifier)
                .then()
                .statusCode(400);
        body.put("version", 0);
        body.put("code", "changed-code");
        request()
                .contentType(ContentType.JSON)
                .body(body)
                .put(ROUTE + "/" + identifier)
                .then()
                .statusCode(400);
        body.put("code", "review-group");
        body.put("name", "Changed group");
        request()
                .contentType(ContentType.JSON)
                .body(body)
                .put(ROUTE + "/" + identifier)
                .then()
                .statusCode(200)
                .body("version", equalTo(1));
        request()
                .contentType(ContentType.JSON)
                .body(body)
                .put(ROUTE + "/" + identifier)
                .then()
                .statusCode(409);
        assertSnapshot("FEATURE_GROUP_UPDATED", identifier, "ACTIVE", 1);
    }

    @Test
    void paginatesAndSortsWithTheCentralLifecycleFilter() {
        String first = create("first-group", "A group");
        String second = create("second-group", "Z group");
        request()
                .get(ROUTE + "?sort=name,desc&size=1")
                .then()
                .statusCode(200)
                .body("content[0].identifier", equalTo(second))
                .body("totalElements", equalTo(2));
        request()
                .get(ROUTE + "?sort=name,desc&size=1&page=1")
                .then()
                .statusCode(200)
                .body("content[0].identifier", equalTo(first));
        request().patch(ROUTE + "/" + second + "/inactivate").then().statusCode(200);
        request().get(ROUTE).then().statusCode(200).body("totalElements", equalTo(1));
        request()
                .get(ROUTE + "?lifecycle=INACTIVE")
                .then()
                .statusCode(200)
                .body("content[0].identifier", equalTo(second));
        request().delete(ROUTE + "/" + second).then().statusCode(204);
        request()
                .get(ROUTE + "?lifecycle=QUARANTINED")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
        for (String query :
                List.of(
                        "sort=featureIdentifiers",
                        "sort=lifecycle.value",
                        "lifecycle=invalid",
                        "page=2147483647&size=100")) {
            request().get(ROUTE + "?" + query).then().statusCode(400);
        }
    }

    @Test
    void purgesAnInactiveGroupAndAuditsItsSnapshotBeforeRemoval() {
        String identifier = create("review-group", "Review group");
        String path = ROUTE + "/" + identifier;
        request().patch(path + "/activate").then().statusCode(400);
        request().patch(path + "/inactivate").then().statusCode(200).body("version", equalTo(1));
        request().get(path).then().statusCode(404);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 1, "code", "review-group", "name", "Changed"))
                .put(path)
                .then()
                .statusCode(404);
        request().patch(path + "/inactivate").then().statusCode(404);
        request().patch(path + "/activate").then().statusCode(200).body("version", equalTo(2));
        request().patch(path + "/inactivate").then().statusCode(200).body("version", equalTo(3));
        request().delete(path).then().statusCode(204);
        request().patch(path + "/activate").then().statusCode(404);
        request().get(path).then().statusCode(404);
        request().delete(path).then().statusCode(404);
        assertSnapshot("FEATURE_GROUP_PURGED", identifier, "INACTIVE", 3);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group WHERE identifier=?",
                                Integer.class,
                                identifier))
                .isZero();
    }

    @Test
    void purgesAnEmptyActiveGroupAndAllowsReusingItsCodeAndName() {
        String identifier = create("review-group", "Review group");
        request().delete(ROUTE + "/" + identifier).then().statusCode(204);
        assertSnapshot("FEATURE_GROUP_PURGED", identifier, "ACTIVE", 0);
        String replacement = create("review-group", "Review group");
        assertThat(replacement).isNotEqualTo(identifier);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group WHERE identifier=?",
                                Integer.class,
                                identifier))
                .isZero();
    }

    @Test
    void anyLinkBlocksPurgeEvenWhenTheFeatureOrTheGroupIsInactive() {
        String identifier = create("review-group", "Review group");
        String feature = feature("review-feature");
        String link = ROUTE + "/" + identifier + "/features/" + feature;
        request().post(link).then().statusCode(200);
        request()
                .patch("/api/v1/service-contexts/features/" + feature + "/inactivate")
                .then()
                .statusCode(200);
        request().delete(ROUTE + "/" + identifier).then().statusCode(400);
        request().patch(ROUTE + "/" + identifier + "/inactivate").then().statusCode(200);
        request().delete(ROUTE + "/" + identifier).then().statusCode(400);
        request().delete(link).then().statusCode(204);
        request().delete(ROUTE + "/" + identifier).then().statusCode(204);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group", Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM service_context_features", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void auditFailureRollsBackPhysicalGroupPurge() {
        String identifier = create("review-group", "Review group");
        int eventsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class);
        doThrow(new IllegalStateException("Test purge audit failure"))
                .when(appender)
                .append(
                        any(),
                        argThat(
                                metadata ->
                                        metadata != null
                                                && "FEATURE_GROUP_PURGED"
                                                        .equals(
                                                                metadata.path("eventType")
                                                                        .asString())));
        request().delete(ROUTE + "/" + identifier).then().statusCode(500);
        request().get(ROUTE + "/" + identifier).then().statusCode(200).body("version", equalTo(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(eventsBefore);
        reset(appender);
        request().delete(ROUTE + "/" + identifier).then().statusCode(204);
        assertSnapshot("FEATURE_GROUP_PURGED", identifier, "ACTIVE", 0);
    }

    @Test
    void reservesNamesAndCodesAcrossLifecycleStates() {
        String first = create("first-group", "First group");
        String second = create("second-group", "Second group");
        request()
                .contentType(ContentType.JSON)
                .body(update(second, "First group"))
                .put(ROUTE + "/" + second)
                .then()
                .statusCode(400);
        request().patch(ROUTE + "/" + first + "/inactivate").then().statusCode(200);
        request()
                .contentType(ContentType.JSON)
                .body(payload("first-group", "Other group"))
                .post(ROUTE)
                .then()
                .statusCode(400);
        request()
                .contentType(ContentType.JSON)
                .body(payload("other-group", "First group"))
                .post(ROUTE)
                .then()
                .statusCode(400);
    }

    @Test
    void requiresOwnerAndRejectsUnauthenticatedCalls() {
        given().port(port).get(ROUTE).then().statusCode(401);
        String identifier = create("review-group", "Review group");
        authorization.reset();
        authorization.forbidden();
        request().get(ROUTE).then().statusCode(403);
        request().get(ROUTE + "/" + identifier).then().statusCode(403);
        request()
                .contentType(ContentType.JSON)
                .body(payload("denied-group", "Denied"))
                .post(ROUTE)
                .then()
                .statusCode(403);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 0, "code", "review-group", "name", "Changed"))
                .put(ROUTE + "/" + identifier)
                .then()
                .statusCode(403);
        request().patch(ROUTE + "/" + identifier + "/inactivate").then().statusCode(403);
        request().delete(ROUTE + "/" + identifier).then().statusCode(403);
    }

    @Test
    void concurrentUpdatesWithTheSameVersionHaveExactlyOneWinner() throws Exception {
        String identifier = create("review-group", "Review group");
        Map<String, Object> first = update(identifier, "First change");
        Map<String, Object> second = new HashMap<>(first);
        second.put("name", "Second change");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> one =
                    executor.submit(
                            () -> {
                                start.await(10, TimeUnit.SECONDS);
                                return request()
                                        .contentType(ContentType.JSON)
                                        .body(first)
                                        .put(ROUTE + "/" + identifier)
                                        .statusCode();
                            });
            Future<Integer> two =
                    executor.submit(
                            () -> {
                                start.await(10, TimeUnit.SECONDS);
                                return request()
                                        .contentType(ContentType.JSON)
                                        .body(second)
                                        .put(ROUTE + "/" + identifier)
                                        .statusCode();
                            });
            start.countDown();
            assertThat(List.of(one.get(10, TimeUnit.SECONDS), two.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 409);
        }
    }

    @Test
    void auditFailureRollsBackTheGroupItsRelationsAndFeatureVersions() {
        String feature = feature("review-feature");
        int eventsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class);
        doThrow(new IllegalStateException("Test audit failure"))
                .when(appender)
                .append(any(), any());
        Map<String, Object> body = payload("review-group", "Review group");
        request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(500);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group", Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group_feature",
                                Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM service_context_features", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(eventsBefore);
    }

    @Test
    void concurrentDuplicateCreationHasOneWinnerAndDoesNotExposeSql() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<io.restassured.response.Response> operation =
                    () -> {
                        if (!start.await(10, TimeUnit.SECONDS))
                            throw new AssertionError("Concurrent create did not start");
                        return request()
                                .contentType(ContentType.JSON)
                                .body(payload("same-group", "Same group"))
                                .post(ROUTE);
                    };
            Future<io.restassured.response.Response> first = executor.submit(operation);
            Future<io.restassured.response.Response> second = executor.submit(operation);
            start.countDown();
            List<io.restassured.response.Response> results =
                    List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(response -> response.statusCode() == 201))
                    .hasSize(1);
            io.restassured.response.Response rejected =
                    results.stream()
                            .filter(response -> response.statusCode() != 201)
                            .findFirst()
                            .orElseThrow();
            assertThat(rejected.statusCode()).isIn(400, 409);
            assertThat(rejected.asString())
                    .doesNotContain("SQL", "Duplicate entry", "feature_group");
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM feature_group", Integer.class))
                .isEqualTo(1);
    }

    private String create(String code, String name) {
        return request()
                .contentType(ContentType.JSON)
                .body(payload(code, name))
                .post(ROUTE)
                .then()
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    private String feature(String code) {
        return request()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "code",
                                code,
                                "name",
                                code,
                                "microserviceIdentifier",
                                MICROSERVICE,
                                "settings",
                                Map.of()))
                .post("/api/v1/service-contexts/features")
                .then()
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    private Map<String, Object> payload(String code, String name) {
        return new HashMap<>(Map.of("code", code, "name", name));
    }

    private Map<String, Object> update(String identifier, String name) {
        Map<String, Object> current =
                request()
                        .get(ROUTE + "/" + identifier)
                        .then()
                        .statusCode(200)
                        .extract()
                        .as(new TypeRef<Map<String, Object>>() {});
        Map<String, Object> body = payload((String) current.get("code"), name);
        body.put("version", current.get("version"));
        return body;
    }

    private RequestSpecification request() {
        return given().port(port)
                .header("Authorization", "Bearer group-review")
                .header("correlation-id", "group-review")
                .accept(ContentType.JSON);
    }

    private void assertSnapshot(String event, String identifier, String lifecycle, int version) {
        List<Map<String, Object>> events =
                jdbc.queryForList(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.identifier')) identifier,"
                            + " JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) lifecycle,"
                            + " JSON_EXTRACT(payload,'$.version') version FROM audit_outbox WHERE"
                            + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))=?",
                        event);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst())
                .containsEntry("identifier", identifier)
                .containsEntry("lifecycle", lifecycle);
        assertThat(String.valueOf(events.getFirst().get("version")))
                .isEqualTo(String.valueOf(version));
    }
}
