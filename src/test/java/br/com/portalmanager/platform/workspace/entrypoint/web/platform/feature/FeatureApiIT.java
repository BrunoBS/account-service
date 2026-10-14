package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature;

import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@TestPropertySource(properties = "platform.audit.enabled=true")
class FeatureApiIT {

    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    private static final String ROUTE = "/api/v1/platform/features";
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AuthorizationMock authorization;
    @Autowired
    private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired
    private FeatureQueryService queryService;
    @MockitoSpyBean
    private AuditOutboxAppender appender;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update("INSERT INTO platform_microservices (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES (?,'parent-service','Parent service','{}','ACTIVE',NOW(6),NOW(6))", MICROSERVICE);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        reset(appender);
    }

    @Test
    void normalizesTextPreservesSettingsAndAuditsTheCreatedSnapshot() {
        Map<String, Object> body = payload("  REVIEW-SERVICE  ", "  Review service  ");
        body.put("description", "  Description  ");
        body.put("settings", Map.of("nested", Map.of("enabled", true, "count", 3), "labels", List.of(" spaced ")));
        String identifier = request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(201)
            .body("code", equalTo("review-service")).body("name", equalTo("Review service"))
            .body("description", equalTo("Description")).body("version", equalTo(0))
            .body("settings.nested.count", equalTo(3)).body("settings.labels[0]", equalTo(" spaced "))
            .extract().path("identifier");
        assertThat(UUID.fromString(identifier).toString()).isEqualTo(identifier);
        assertSnapshot("FEATURE_CREATED", identifier, "ACTIVE", 0);
        assertThat(jdbc.queryForObject("SELECT JSON_TYPE(JSON_EXTRACT(payload,'$.settings')) FROM audit_outbox", String.class)).isEqualTo("OBJECT");
    }

    @Test
    void rejectsRawTypesUnknownFieldsAndLimitsForBothCreateAndUpdate() {
        String identifier = create("review-service", "Review service");
        for (Map<String, ?> invalid : List.of(
            Map.of("microserviceIdentifier", "not-a-uuid"), Map.of("shareable", "true"), Map.of("shareable", 1), Map.of("name", 123), Map.of("code", List.of("review-service")),
            Map.of("settings", "{}"), Map.of("settings", List.of()),
            Map.of("version", "0"), Map.of("version", 0.5), Map.of("version", -1),
            Map.of("actor", "owner"), Map.of("identifier", identifier),
            Map.of("name", "n".repeat(101)), Map.of("description", "d".repeat(501)),
            Map.of("code", "c".repeat(51)), Map.of("name", "   ")
        )) {
            Map<String, Object> body = payload("other-service", "Other service");
            body.putAll(invalid);
            request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(400);
            if (!invalid.containsKey("code")) body.put("code", "review-service");
            body.putIfAbsent("version", 0);
            request().contentType(ContentType.JSON).body(body).put(ROUTE + "/" + identifier).then().statusCode(400);
        }
        for (String body : List.of("null", "[]", "{}", "{\"code\":\"other\",\"name\":\"Other\",\"settings\":null}")) {
            request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(400);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(1);
    }

    @Test
    void acceptsTheExistingPlatformColumnLimits() {
        Map<String, Object> body = payload("c".repeat(50), "n".repeat(100));
        body.put("description", "d".repeat(500));
        request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(201)
            .body("name", equalTo("n".repeat(100))).body("description", equalTo("d".repeat(500)));
    }

    @Test
    void rejectsVersionOnCreateAndRequiresTheCurrentVersionOnUpdate() {
        Map<String, Object> body = payload("review-service", "Review service");
        body.put("version", 0);
        request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isZero();
        body.remove("version");
        String identifier = create("review-service", "Review service");
        request().contentType(ContentType.JSON).body(body).put(ROUTE + "/" + identifier).then().statusCode(400);
        body.put("version", 0);
        body.put("name", "Updated service");
        request().contentType(ContentType.JSON).body(body).put(ROUTE + "/" + identifier).then().statusCode(200).body("version", equalTo(1));
        assertSnapshot("FEATURE_UPDATED", identifier, "ACTIVE", 1);
        body.put("name", "Stale service");
        request().contentType(ContentType.JSON).body(body).put(ROUTE + "/" + identifier).then().statusCode(409);
        body.put("version", 1);
        body.put("code", "changed-code");
        request().contentType(ContentType.JSON).body(body).put(ROUTE + "/" + identifier).then().statusCode(400);
        request().get(ROUTE + "/" + identifier).then().statusCode(200).body("name", equalTo("Updated service"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(2);
    }

    @Test
    void paginatesWithTheCentralLifecycleFilterAndRejectsUnsafeSorting() {
        String active = create("active-service", "Active service");
        String inactive = create("inactive-service", "Inactive service");
        create("second-service", "Second service");
        request().patch(ROUTE + "/" + inactive + "/inactivate").then().statusCode(200);
        request().get(ROUTE + "?size=1&page=0").then().statusCode(200)
            .body("content.size()", equalTo(1)).body("content[0].identifier", equalTo(active)).body("totalElements", equalTo(2));
        request().get(ROUTE + "?size=1&page=1").then().statusCode(200).body("content[0].code", equalTo("second-service"));
        request().get(ROUTE + "?lifecycle=INACTIVE").then().statusCode(200).body("content[0].identifier", equalTo(inactive)).body("totalElements", equalTo(1));
        request().get(ROUTE + "?lifecycle=QUARANTINED").then().statusCode(200).body("totalElements", equalTo(0));
        request().get(ROUTE + "?lifecycle=invalid").then().statusCode(400);
        request().get(ROUTE + "?sort=settings").then().statusCode(400);
        request().get(ROUTE + "?sort=lifecycle.value").then().statusCode(400);
        request().get(ROUTE + "?page=2147483647&size=20").then().statusCode(400);
        request().get(ROUTE + "/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    void rejectsUnauthenticatedAndPolicyDeniedReadsAndWrites() {
        given().port(port).get(ROUTE).then().statusCode(401);
        authorization.reset();
        authorization.forbidden();
        request().get(ROUTE).then().statusCode(403);
        request().get(ROUTE + "/" + UUID.randomUUID()).then().statusCode(403);
        request().contentType(ContentType.JSON).body(payload("denied-service", "Denied service")).post(ROUTE).then().statusCode(403);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features", Integer.class)).isZero();
    }

    @Test
    void reservesCodesAndNamesAcrossLifecycleStatesAndRejectsDuplicateNameOnUpdate() {
        String first = create("first-service", "First service");
        String second = create("second-service", "Second service");
        Map<String, Object> update = payload("second-service", "First service");
        update.put("version", 0);
        request().contentType(ContentType.JSON).body(update).put(ROUTE + "/" + second).then().statusCode(400);
        request().patch(ROUTE + "/" + first + "/inactivate").then().statusCode(200);
        request().contentType(ContentType.JSON).body(payload("first-service", "Different service")).post(ROUTE).then().statusCode(400);
        request().contentType(ContentType.JSON).body(payload("different-service", "First service")).post(ROUTE).then().statusCode(400);
        request().get(ROUTE + "/" + second).then().statusCode(200).body("version", equalTo(0));
    }

    @Test
    void permitsOnlyOneConcurrentUpdateForTheSameClientVersion() throws Exception {
        String identifier = create("review-service", "Review service");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> concurrentUpdate(identifier, "First update", start));
            Future<Integer> second = executor.submit(() -> concurrentUpdate(identifier, "Second update", start));
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(200, 409);
        }
        request().get(ROUTE + "/" + identifier).then().statusCode(200).body("version", equalTo(1));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(2);
    }

    @Test
    void concurrentDuplicateCreatesKeepOneRecordAndDoNotExposeDatabaseErrors() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Response> first = executor.submit(() -> concurrentCreate(start));
            Future<Response> second = executor.submit(() -> concurrentCreate(start));
            start.countDown();
            List<Response> responses = List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            assertThat(responses.stream().filter(response -> response.statusCode() == 201).count()).isEqualTo(1);
            Response rejected = responses.stream().filter(response -> response.statusCode() != 201).findFirst().orElseThrow();
            assertThat(rejected.statusCode()).isIn(400, 409);
            assertThat(rejected.asString()).doesNotContain("SQL", "ConstraintViolation", "platform_features", "Duplicate entry");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(1);
    }

    private Response concurrentCreate(CountDownLatch start) throws InterruptedException {
        if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Concurrent create did not start");
        return request().contentType(ContentType.JSON).body(payload("review-service", "Review service")).post(ROUTE);
    }

    @Test
    void keepsTheResourceContractSeparateFromPerCodeSettingsValidation() {
        String identifier = create("microservice", "Microservice");
        jdbc.update("INSERT INTO schema_definitions (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at) VALUES (0,UUID(),'PLATFORM','review-settings','Review settings','ACTIVE',NOW(6),NOW(6))");
        jdbc.update("INSERT INTO schema_versions (identifier,schema_id,schema_version,version_name,definition,status,created_at) SELECT UUID(),id,1,'Review settings v1','{\"type\":\"object\",\"required\":[\"enabled\"],\"properties\":{\"enabled\":{\"type\":\"boolean\"}},\"additionalProperties\":false}','PUBLISHED',NOW(6) FROM schema_definitions WHERE code='review-settings'");
        jdbc.update("INSERT INTO schema_configuration (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at) SELECT 0,UUID(),'FEATURE','microservice',id,'ACTIVE',NOW(6),NOW(6) FROM schema_definitions WHERE code='review-settings'");
        Map<String, Object> update = payload("microservice", "Updated microservice");
        update.put("version", 0);
        request().contentType(ContentType.JSON).body(update).put(ROUTE + "/" + identifier).then().statusCode(400);
        update.put("settings", Map.of("enabled", true));
        request().contentType(ContentType.JSON).body(update).put(ROUTE + "/" + identifier).then().statusCode(200).body("settings.enabled", equalTo(true));
    }

    @Test
    void rollsBackTheWriteWhenTheAuditOutboxFails() {
        doThrow(new IllegalStateException("Simulated audit failure")).when(appender).append(any(), any());
        request().contentType(ContentType.JSON).body(payload("review-service", "Review service")).post(ROUTE).then().statusCode(500);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isZero();
    }

    @Test
    void reportsCorruptedStoredSettingsAsAServerError() {
        String identifier = create("review-service", "Review service");
        jdbc.update("UPDATE platform_features SET settings='[]' WHERE identifier=?", identifier);
        request().get(ROUTE + "/" + identifier).then().statusCode(500).body("code", equalTo("PLATFORM-0124"));
        request().patch(ROUTE + "/" + identifier + "/inactivate").then().statusCode(500);
        assertThat(jdbc.queryForObject("SELECT lifecycle_code FROM platform_features WHERE identifier=?", String.class, identifier)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(1);
    }

    @Test
    void onlyActivatesAndInactivatesFeaturesAndRetainsTheirAssociations() {
        String identifier = create("review-service", "Review service");
        String context = createContext("review-context");
        String path = ROUTE + "/" + identifier;
        request().post("/api/v1/platform/contexts/" + context + "/features/" + identifier).then().statusCode(200).body("version", equalTo(1));
        request().delete(path).then().statusCode(405);
        request().patch(path + "/activate").then().statusCode(400);
        request().patch(path + "/inactivate").then().statusCode(200).body("version", equalTo(2));
        request().delete(path).then().statusCode(405);
        request().get(path).then().statusCode(404);
        request().post("/api/v1/platform/contexts/" + context + "/features/" + identifier).then().statusCode(404);
        Map<String, Object> update = payload("review-service", "Inactive update");
        update.put("version", 2);
        request().contentType(ContentType.JSON).body(update).put(path).then().statusCode(404);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isEqualTo(1);
        request().delete("/api/v1/platform/contexts/" + context + "/features/" + identifier).then().statusCode(204);
        request().patch(path + "/activate").then().statusCode(200).body("version", equalTo(4));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType')) IN ('FEATURE_DELETED','FEATURE_PURGED')", Integer.class)).isZero();
    }

    @Test
    void auditsActualAssociationChangesAndPreservesVersionsOnRetries() {
        String identifier = create("review-service", "Review service");
        String context = createContext("review-context");
        String path = "/api/v1/platform/contexts/" + context + "/features/" + identifier;
        request().post(path).then().statusCode(200).body("version", equalTo(1));
        request().post(path).then().statusCode(200).body("version", equalTo(1));
        assertSnapshot("FEATURE_CONTEXT_ASSOCIATED", identifier, "ACTIVE", 1);
        request().delete(path).then().statusCode(204);
        request().delete(path).then().statusCode(204);
        assertSnapshot("FEATURE_CONTEXT_REMOVED", identifier, "ACTIVE", 2);
        assertThat(jdbc.queryForObject("SELECT version FROM platform_features WHERE identifier=?", Long.class, identifier))
            .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceType'))='FEATURE'", Integer.class)).isEqualTo(3);
    }

    @Test
    void filtersAndPaginatesContextsAndCombinesTheContextIdentifierWithFeatureLifecycle() {
        String identifier = create("review-service", "Review service");
        String other = create("other-service", "Other service");
        String active = createContext("active-context");
        String inactive = createContext("inactive-context");
        request().post("/api/v1/platform/contexts/" + active + "/features/" + identifier).then().statusCode(200);
        request().post("/api/v1/platform/contexts/" + inactive + "/features/" + identifier).then().statusCode(200);
        request().patch("/api/v1/platform/contexts/" + inactive + "/inactivate").then().statusCode(200);
        String path = "/api/v1/platform/contexts?featureIdentifier=" + identifier;
        request().get(path + "&size=1").then().statusCode(200).body("content[0].identifier", equalTo(active)).body("totalElements", equalTo(1));
        request().get(path + "&lifecycle=INACTIVE").then().statusCode(200).body("content[0].identifier", equalTo(inactive));
        request().get(path + "&lifecycle=QUARANTINED").then().statusCode(200).body("totalElements", equalTo(0));
        request().get(path + "&lifecycle=invalid").then().statusCode(400);
        request().get(path + "&sort=lifecycle.value").then().statusCode(400);
        request().get(ROUTE + "?contextIdentifier=" + active + "&size=1").then().statusCode(200).body("content[0].identifier", equalTo(identifier)).body("totalElements", equalTo(1));
        request().get(ROUTE + "?contextIdentifier=" + inactive).then().statusCode(200).body("content[0].identifier", equalTo(identifier));
        request().get("/api/v1/platform/contexts?featureIdentifier=" + other).then().statusCode(200).body("totalElements", equalTo(0));
        String lastActive = createContext("z-context");
        request().post("/api/v1/platform/contexts/" + lastActive + "/features/" + identifier).then().statusCode(200);
        request().get(path + "&sort=name,desc&size=1").then().statusCode(200)
            .body("content[0].identifier", equalTo(lastActive)).body("totalElements", equalTo(2));
        request().get(path + "&sort=name,desc&size=1&page=1").then().statusCode(200)
            .body("content[0].identifier", equalTo(active)).body("totalElements", equalTo(2));
    }

    @Test
    void validatesParentReferencesForCreationUpdateAndActivation() {
        String identifier = create("review-service", "Review service");
        String parent = request().contentType(ContentType.JSON).body(Map.of("code", "other-parent", "name", "Other parent", "settings", Map.of()))
            .post("/api/v1/platform/microservices").then().statusCode(201).extract().path("identifier");
        Map<String, Object> update = payload("review-service", "Moved feature");
        update.put("version", 0);
        update.put("microserviceIdentifier", parent);
        request().contentType(ContentType.JSON).body(update).put(ROUTE + "/" + identifier).then().statusCode(200).body("microserviceIdentifier", equalTo(parent));
        request().patch("/api/v1/platform/microservices/" + parent + "/inactivate").then().statusCode(200);
        Map<String, Object> create = payload("denied", "Denied feature");
        create.put("microserviceIdentifier", parent);
        request().contentType(ContentType.JSON).body(create).post(ROUTE).then().statusCode(400);
        update.put("version", 1);
        request().contentType(ContentType.JSON).body(update).put(ROUTE + "/" + identifier).then().statusCode(400);
        request().get(ROUTE + "/" + identifier).then().statusCode(200).body("microserviceIdentifier", equalTo(parent));
        request().patch(ROUTE + "/" + identifier + "/inactivate").then().statusCode(200);
        request().patch(ROUTE + "/" + identifier + "/activate").then().statusCode(400);
        request().patch("/api/v1/platform/microservices/" + parent + "/activate").then().statusCode(200);
        request().patch(ROUTE + "/" + identifier + "/activate").then().statusCode(200);
        create.put("microserviceIdentifier", UUID.randomUUID().toString());
        request().contentType(ContentType.JSON).body(create).post(ROUTE).then().statusCode(404);
    }

    @Test
    void contextAssociationsRequireActiveFeaturesAndAuditRemovalAfterInactivation() {
        String identifier = create("review-service", "Review service");
        String context = createContext("review-context");
        String link = "/api/v1/platform/contexts/" + context + "/features/" + identifier;
        request().post(link).then().statusCode(200);
        assertSnapshot("FEATURE_CONTEXT_ASSOCIATED", identifier, "ACTIVE", 1);
        request().patch(ROUTE + "/" + identifier + "/inactivate").then().statusCode(200);
        request().post(link).then().statusCode(404);
        request().delete(link).then().statusCode(204);
        assertSnapshot("FEATURE_CONTEXT_REMOVED", identifier, "INACTIVE", 3);
    }

    @Test
    void rollsBackAssociationAndVersionWhenItsAuditFails() {
        String identifier = create("review-service", "Review service");
        String context = createContext("review-context");
        int events = jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class);
        doThrow(new IllegalStateException("Simulated association audit failure")).when(appender).append(any(), any());
        request().post("/api/v1/platform/contexts/" + context + "/features/" + identifier).then().statusCode(500);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM platform_features WHERE identifier=?", Long.class, identifier)).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(events);
    }

    @Test
    void loadsAPageWithoutAnAdditionalQueryForEachMicroservice() {
        for (int index = 0; index < 4; index++) {
            String parent = request().contentType(ContentType.JSON).body(Map.of("code", "parent-" + index, "name", "Parent " + index, "settings", Map.of()))
                .post("/api/v1/platform/microservices").then().statusCode(201).extract().path("identifier");
            Map<String, Object> body = payload("feature-" + index, "Feature " + index);
            body.put("microserviceIdentifier", parent);
            request().contentType(ContentType.JSON).body(body).post(ROUTE).then().statusCode(201);
        }
        org.hibernate.stat.Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean originallyEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(queryService.findAll(null, null, org.springframework.data.domain.PageRequest.of(0, 2)).getContent()).hasSize(2);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(originallyEnabled);
        }
    }

    @Test
    void listsContextRelationsForInactiveFeaturesForCleanup() {
        String featureIdentifier = create("review-service", "Review service");
        String contextIdentifier = createContext("review-context");
        String featurePath = ROUTE + "/" + featureIdentifier;
        String contextsPath = "/api/v1/platform/contexts?featureIdentifier=" + featureIdentifier;
        request().post("/api/v1/platform/contexts/" + contextIdentifier + "/features/" + featureIdentifier).then().statusCode(200);
        request().patch(featurePath + "/inactivate").then().statusCode(200);
        request().get(featurePath).then().statusCode(404);
        request().get(contextsPath).then().statusCode(200).body("content[0].identifier", equalTo(contextIdentifier));
        request().patch("/api/v1/platform/contexts/" + contextIdentifier + "/inactivate").then().statusCode(200);
        request().get(contextsPath).then().statusCode(200).body("totalElements", equalTo(0));
        request().get(contextsPath + "&lifecycle=INACTIVE").then().statusCode(200).body("content[0].identifier", equalTo(contextIdentifier));
        request().get(featurePath).then().statusCode(404);
        request().get(contextsPath + "&lifecycle=INACTIVE").then().statusCode(200).body("content[0].identifier", equalTo(contextIdentifier));
        request().get("/api/v1/platform/contexts?featureIdentifier=" + UUID.randomUUID()).then().statusCode(404);
        request().delete("/api/v1/platform/contexts/" + contextIdentifier + "/features/" + featureIdentifier).then().statusCode(204);
        request().get(contextsPath + "&lifecycle=INACTIVE").then().statusCode(200).body("totalElements", equalTo(0));
        authorization.reset();
        authorization.forbidden();
        request().get(contextsPath + "&lifecycle=INACTIVE").then().statusCode(403);
    }

    private String createContext(String code) {
        return request().contentType(ContentType.JSON).body(Map.of("code", code, "name", code)).post("/api/v1/platform/contexts")
            .then().statusCode(201).extract().path("identifier");
    }

    private int concurrentUpdate(String identifier, String name, CountDownLatch start) throws InterruptedException {
        if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("Concurrent update did not start");
        Map<String, Object> update = payload("review-service", name);
        update.put("version", 0);
        return request().contentType(ContentType.JSON).body(update).put(ROUTE + "/" + identifier).statusCode();
    }

    private String create(String code, String name) {
        return request().contentType(ContentType.JSON).body(payload(code, name)).post(ROUTE)
            .then().statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> payload(String code, String name) {
        return new HashMap<>(Map.of("code", code, "name", name, "microserviceIdentifier", MICROSERVICE, "settings", Map.of()));
    }

    private RequestSpecification request() {
        return given().port(port).header("correlation-id", "feature-review-it")
            .header("Authorization", "Bearer feature-review-it").accept(ContentType.JSON);
    }

    private void assertSnapshot(String event, String identifier, String lifecycle, int version) {
        List<Map<String, Object>> events = jdbc.queryForList("SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier')) resourceIdentifier, JSON_UNQUOTE(JSON_EXTRACT(payload,'$.identifier')) identifier, JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) lifecycle, JSON_EXTRACT(payload,'$.version') version FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))=?", event);
        assertThat(events).hasSize(1);
        assertThat(events.getFirst()).containsEntry("resourceIdentifier", identifier).containsEntry("identifier", identifier).containsEntry("lifecycle", lifecycle);
        assertThat(String.valueOf(events.getFirst().get("version"))).isEqualTo(String.valueOf(version));
    }

}
