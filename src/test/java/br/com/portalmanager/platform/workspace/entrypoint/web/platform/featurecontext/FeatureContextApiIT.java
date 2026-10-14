package br.com.portalmanager.platform.workspace.entrypoint.web.platform.featurecontext;

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
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
class FeatureContextApiIT {
    private static final String CONTEXTS = "/api/v1/platform/contexts";
    private static final String FEATURES = "/api/v1/platform/features";
    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @MockitoSpyBean private AuditOutboxAppender appender;

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
    void individualRetriesDoNotIncrementVersionsOrDuplicateAuditEvents() {
        String context = context("review-context");
        String feature = feature("review-feature");
        String path = links(context) + "/" + feature;
        request().post(path).then().statusCode(200).body("version", equalTo(1));
        request().post(path).then().statusCode(200).body("version", equalTo(1));
        assertThat(contextVersion(context)).isEqualTo(1);
        request().delete(path).then().statusCode(204).body(equalTo(""));
        request().delete(path).then().statusCode(204).body(equalTo(""));
        assertThat(contextVersion(context)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT version FROM platform_features WHERE identifier=?", Long.class, feature))
            .isEqualTo(2);
        assertThat(count("platform_feature_context_relations")).isZero();
        assertThat(events("FEATURE_CONTEXT_ASSOCIATED")).isEqualTo(1);
        assertThat(events("FEATURE_CONTEXT_REMOVED")).isEqualTo(1);
        assertThat(count("platform_features")).isEqualTo(1);
    }

    @Test
    void newBindingsRequireActiveFeaturesAndParentsButExistingBindingsCanBePreservedOrRemoved() {
        String context = context("review-context");
        String otherContext = context("other-context");
        String feature = feature("review-feature");
        request().post(links(context) + "/" + feature).then().statusCode(200);
        request().patch(FEATURES + "/" + feature + "/inactivate").then().statusCode(200);
        request().post(links(context) + "/" + feature).then().statusCode(404);
        assertThat(count("platform_feature_context_relations")).isEqualTo(1);
        request().post(links(otherContext) + "/" + feature).then().statusCode(404);
        request().delete(links(context) + "/" + feature).then().statusCode(204);
        request().patch(FEATURES + "/" + feature + "/activate").then().statusCode(200);
        request().patch("/api/v1/platform/microservices/" + MICROSERVICE + "/inactivate").then().statusCode(200);
        request().post(links(otherContext) + "/" + feature).then().statusCode(400);
        assertThat(count("platform_feature_context_relations")).isZero();
    }

    @Test
    void inactiveContextsRejectNewBindingsButAllowRelationshipCleanup() {
        String context = context("review-context");
        String feature = feature("review-feature");
        request().post(links(context) + "/" + feature).then().statusCode(200);
        request().patch(CONTEXTS + "/" + context + "/inactivate").then().statusCode(200);
        request().delete(CONTEXTS + "/" + context).then().statusCode(400);
        request().post(links(context) + "/" + feature).then().statusCode(400);
        request().delete(links(context) + "/" + feature).then().statusCode(204);
        request().delete(CONTEXTS + "/" + context).then().statusCode(204);
    }

    @Test
    void queriesBothDirectionsWithPaginationLifecycleAndValidatedSorts() {
        String context = context("review-context");
        String first = feature("a-feature");
        String second = feature("z-feature");
        feature("unrelated-feature");
        request().post(links(context) + "/" + first).then().statusCode(200);
        request().post(links(context) + "/" + second).then().statusCode(200);
        request().get(featuresByContext(context) + "&size=1&sort=name,desc").then().statusCode(200)
            .body("totalElements", equalTo(2)).body("content[0].identifier", equalTo(second));
        request().get(featuresByContext(context) + "&size=1&sort=name,desc&page=1").then().statusCode(200)
            .body("content[0].identifier", equalTo(first));
        request().patch(FEATURES + "/" + second + "/inactivate").then().statusCode(200);
        request().get(featuresByContext(context)).then().statusCode(200).body("totalElements", equalTo(1));
        request().get(featuresByContext(context) + "&lifecycle=INACTIVE").then().statusCode(200)
            .body("content[0].identifier", equalTo(second));
        request().get(contextsByFeature(first)).then().statusCode(200)
            .body("content[0].identifier", equalTo(context));
        for (String query : List.of("sort=microservice.name", "lifecycle=invalid", "page=2147483647&size=100")) {
            request().get(featuresByContext(context) + "&" + query).then().statusCode(400);
        }
        request().get(featuresByContext(UUID.randomUUID().toString())).then().statusCode(404);
        request().get(featuresByContext("")).then().statusCode(200).body("totalElements", equalTo(2));
        request().get(featuresByContext(" ")).then().statusCode(200).body("totalElements", equalTo(2));
        request().get(featuresByContext(" " + context + " ")).then().statusCode(200)
            .body("totalElements", equalTo(1)).body("content[0].identifier", equalTo(first));
        request().get(featuresByContext(context("empty-context"))).then().statusCode(200)
            .body("totalElements", equalTo(0));
        request().get(links(context)).then().statusCode(404);
        request().get(FEATURES + "/contexts/" + context + "/features").then().statusCode(404);
        request().get(contextsByFeature(UUID.randomUUID().toString())).then().statusCode(404);
        request().get(contextsByFeature("")).then().statusCode(200).body("totalElements", equalTo(2));
        request().get(contextsByFeature(" ")).then().statusCode(200).body("totalElements", equalTo(2));
        request().get(contextsByFeature(first)).then().statusCode(200)
            .body("totalElements", equalTo(1)).body("content[0].identifier", equalTo(context));
        request().get(contextsByFeature(" " + first + " ")).then().statusCode(200)
            .body("totalElements", equalTo(1)).body("content[0].identifier", equalTo(context));
        request().get(FEATURES + "/" + first + "/contexts").then().statusCode(404);
    }

    @Test
    void contextUpdatesCannotModifyLinksAndRejectAssociationFields() {
        String context = context("review-context");
        String feature = feature("review-feature");
        request().post(links(context) + "/" + feature).then().statusCode(200);
        request().contentType(ContentType.JSON).body(Map.of("version", 1, "code", "review-context", "name", "Changed"))
            .put(CONTEXTS + "/" + context).then().statusCode(200);
        request().contentType(ContentType.JSON).body(Map.of("version", 2, "code", "review-context", "name", "Changed", "featureIdentifiers", List.of()))
            .put(CONTEXTS + "/" + context).then().statusCode(400);
        request().contentType(ContentType.JSON).body(Map.of("code", "other-context", "name", "Other", "featureIdentifiers", List.of(feature)))
            .post(CONTEXTS).then().statusCode(400);
        assertThat(count("platform_feature_context_relations")).isEqualTo(1);
    }

    @Test
    void failedAssociationAuditRollsBackLinkAndResourceVersions() {
        String context = context("review-context");
        String feature = feature("review-feature");
        int eventsBefore = count("audit_outbox");
        doThrow(new IllegalStateException("Test audit failure")).when(appender).append(any(), any());
        request().post(links(context) + "/" + feature).then().statusCode(500);
        assertThat(count("platform_feature_context_relations")).isZero();
        assertThat(contextVersion(context)).isZero();
        assertThat(jdbc.queryForObject("SELECT version FROM platform_features", Long.class)).isZero();
        assertThat(count("audit_outbox")).isEqualTo(eventsBefore);
    }

    @Test
    void writesAreAvailableOnlyUnderContextsAndThereIsNoReplacementEndpoint() {
        String context = context("review-context");
        String feature = feature("review-feature");
        request().contentType(ContentType.JSON).body(Map.of("version", 0, "featureIdentifiers", List.of(feature)))
            .put(links(context)).then().statusCode(404);
        String removedRoute = FEATURES + "/" + feature + "/contexts/" + context;
        request().post(removedRoute).then().statusCode(404);
        request().delete(removedRoute).then().statusCode(404);
        assertThat(count("platform_feature_context_relations")).isZero();
        assertThat(contextVersion(context)).isZero();
    }

    @Test
    void everyAssociationEndpointRequiresOwnerAndDeniedRequestsDoNotMutateData() {
        String context = context("review-context");
        String feature = feature("review-feature");
        given().port(port).get(featuresByContext(context)).then().statusCode(401);
        authorization.reset(); authorization.forbidden();
        request().get(featuresByContext(context)).then().statusCode(403);
        request().get(contextsByFeature(feature)).then().statusCode(403);
        request().post(links(context) + "/" + feature).then().statusCode(403);
        request().delete(links(context) + "/" + feature).then().statusCode(403);
        request().post(links(context) + "/" + feature).then().statusCode(403);
        assertThat(count("platform_feature_context_relations")).isZero();
        assertThat(contextVersion(context)).isZero();
    }

    private String context(String code) {
        return request().contentType(ContentType.JSON).body(Map.of("code", code, "name", code))
            .post(CONTEXTS).then().statusCode(201).extract().path("identifier");
    }
    private String feature(String code) {
        return request().contentType(ContentType.JSON).body(Map.of("code", code, "name", code, "microserviceIdentifier", MICROSERVICE, "settings", Map.of()))
            .post(FEATURES).then().statusCode(201).extract().path("identifier");
    }
    private String featuresByContext(String contextIdentifier) { return FEATURES + "?contextIdentifier=" + contextIdentifier; }
    private String contextsByFeature(String featureIdentifier) { return CONTEXTS + "?featureIdentifier=" + featureIdentifier; }
    private String links(String contextIdentifier) { return CONTEXTS + "/" + contextIdentifier + "/features"; }
    private int count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private int events(String event) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))=?", Integer.class, event);
    }
    private long contextVersion(String identifier) {
        return jdbc.queryForObject("SELECT version FROM platform_feature_contexts WHERE identifier=?", Long.class, identifier);
    }
    private RequestSpecification request() {
        return given().port(port).header("Authorization", "Bearer feature-context-review").header("correlation-id", "feature-context-review").accept(ContentType.JSON);
    }
}
