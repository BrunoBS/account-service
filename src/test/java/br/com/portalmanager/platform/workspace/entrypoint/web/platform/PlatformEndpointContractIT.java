package br.com.portalmanager.platform.workspace.entrypoint.web.platform;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class PlatformEndpointContractIT {
    private static final String MICROSERVICES = "/api/v1/platform/microservices";
    private static final String FEATURES = "/api/v1/platform/features";
    private static final String CONTEXTS = "/api/v1/platform/contexts";
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void exercisesResourceRoutesAndTheSeparateAssociationController() {
        String microservice = createMicroservice("route-microservice");
        String feature = createFeature(microservice);
        String context = createContext();
        for (String path : List.of(MICROSERVICES + "/" + microservice, FEATURES + "/" + feature, CONTEXTS + "/" + context)) {
            request().get(path).then().statusCode(200).body("version", equalTo(0));
        }
        request().contentType(ContentType.JSON).body(Map.of("version", 0, "code", "route-microservice", "name", "Updated microservice", "settings", Map.of()))
            .put(MICROSERVICES + "/" + microservice).then().statusCode(200).body("version", equalTo(1));
        request().contentType(ContentType.JSON).body(Map.of("version", 0, "code", "route-feature", "name", "Updated feature", "microserviceIdentifier", microservice, "settings", Map.of()))
            .put(FEATURES + "/" + feature).then().statusCode(200).body("version", equalTo(1));
        request().contentType(ContentType.JSON).body(Map.of("version", 0, "code", "route-context", "name", "Updated context"))
            .put(CONTEXTS + "/" + context).then().statusCode(200).body("version", equalTo(1));
        request().post(CONTEXTS + "/" + context + "/features/" + feature).then().statusCode(200);
        for (String collection : List.of(MICROSERVICES, FEATURES, CONTEXTS)) {
            request().get(collection).then().statusCode(200).body("totalElements", equalTo(1));
        }
        request().queryParam("contextIdentifier", context).get(FEATURES).then().statusCode(200)
            .body("totalElements", equalTo(1)).body("content[0].identifier", equalTo(feature));
        request().get(CONTEXTS + "?featureIdentifier=" + feature).then().statusCode(200)
            .body("totalElements", equalTo(1)).body("content[0].identifier", equalTo(context));
        String relationship = CONTEXTS + "/" + context + "/features/" + feature;
        request().post(relationship).then().statusCode(200).body("identifier", equalTo(feature)).body("version", equalTo(2));
        request().delete(relationship).then().statusCode(204).body(equalTo(""));
        request().get(FEATURES + "/" + feature).then().statusCode(200).body("version", equalTo(3));
        request().get(CONTEXTS + "/" + context).then().statusCode(200).body("version", equalTo(3));
        for (String path : List.of(CONTEXTS + "/" + context, FEATURES + "/" + feature, MICROSERVICES + "/" + microservice)) {
            request().patch(path + "/inactivate").then().statusCode(200).body("lifecycle", equalTo("INACTIVE"));
            request().patch(path + "/activate").then().statusCode(200).body("lifecycle", equalTo("ACTIVE"));
        }
        request().patch(CONTEXTS + "/" + context + "/inactivate").then().statusCode(200);
        request().delete(CONTEXTS + "/" + context).then().statusCode(204);
        request().get(CONTEXTS + "/" + context).then().statusCode(404);
        request().patch(FEATURES + "/" + feature + "/inactivate").then().statusCode(200);
        request().delete(FEATURES + "/" + feature).then().statusCode(405);
        request().get(FEATURES + "/" + feature).then().statusCode(404);
        request().patch(MICROSERVICES + "/" + microservice + "/inactivate").then().statusCode(200);
        request().delete(MICROSERVICES + "/" + microservice).then().statusCode(405);
        request().get(MICROSERVICES + "?lifecycle=INACTIVE").then().statusCode(200).body("totalElements", equalTo(1));
        request().get(FEATURES + "?lifecycle=INACTIVE").then().statusCode(200).body("totalElements", equalTo(1));
        request().get(CONTEXTS).then().statusCode(200).body("totalElements", equalTo(0));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_contexts", Integer.class)).isZero();
    }

    @Test
    void everyRouteRequiresOwnerAndDeniedCallsDoNotMutateAnyResource() {
        String microservice = createMicroservice("route-microservice");
        String feature = createFeature(microservice);
        String context = createContext();
        authorization.reset();
        authorization.forbidden();
        for (String collection : List.of(MICROSERVICES, FEATURES, CONTEXTS)) request().get(collection).then().statusCode(403);
        request().queryParam("contextIdentifier", context).get(FEATURES).then().statusCode(403);
        request().get(CONTEXTS + "?featureIdentifier=" + feature).then().statusCode(403);
        for (String path : List.of(MICROSERVICES + "/" + microservice, FEATURES + "/" + feature, CONTEXTS + "/" + context)) {
            request().get(path).then().statusCode(403);
            request().patch(path + "/activate").then().statusCode(403);
            request().patch(path + "/inactivate").then().statusCode(403);
            request().delete(path).then().statusCode(path.startsWith(CONTEXTS) ? 403 : 405);
        }
        Map<String, Object> microserviceBody = Map.of("code", "route-microservice", "name", "Changed microservice", "settings", Map.of());
        Map<String, Object> featureBody = Map.of("code", "route-feature", "name", "Changed feature", "microserviceIdentifier", microservice, "settings", Map.of());
        Map<String, Object> contextBody = Map.of("code", "route-context", "name", "Changed context");
        request().contentType(ContentType.JSON).body(microserviceBody).post(MICROSERVICES).then().statusCode(403);
        request().contentType(ContentType.JSON).body(featureBody).post(FEATURES).then().statusCode(403);
        request().contentType(ContentType.JSON).body(contextBody).post(CONTEXTS).then().statusCode(403);
        request().contentType(ContentType.JSON).body(withVersion(microserviceBody)).put(MICROSERVICES + "/" + microservice).then().statusCode(403);
        request().contentType(ContentType.JSON).body(withVersion(featureBody)).put(FEATURES + "/" + feature).then().statusCode(403);
        request().contentType(ContentType.JSON).body(withVersion(contextBody)).put(CONTEXTS + "/" + context).then().statusCode(403);
        request().post(CONTEXTS + "/" + context + "/features/" + feature).then().statusCode(403);
        request().delete(CONTEXTS + "/" + context + "/features/" + feature).then().statusCode(403);
        for (String table : List.of("platform_microservices", "platform_features", "platform_feature_contexts")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE version=0 AND lifecycle_code='ACTIVE'", Integer.class)).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
    }

    @Test
    void associationChangesInvalidateContextEditsAndInactivationAllowsRelationshipCleanup() {
        String microservice = createMicroservice("route-microservice");
        String feature = createFeature(microservice);
        String context = createContext();
        String relationship = CONTEXTS + "/" + context + "/features/" + feature;
        request().post(relationship).then().statusCode(200);
        request().contentType(ContentType.JSON).body(Map.of("version", 0, "code", "route-context", "name", "Stale"))
            .put(CONTEXTS + "/" + context).then().statusCode(409);
        request().get(CONTEXTS + "/" + context).then().statusCode(200).body("name", equalTo("Route context")).body("version", equalTo(1));
        request().get(FEATURES + "?contextIdentifier=" + context).then().statusCode(200).body("totalElements", equalTo(1));
        request().patch(CONTEXTS + "/" + context + "/inactivate").then().statusCode(200);
        request().patch(FEATURES + "/" + feature + "/inactivate").then().statusCode(200);
        request().get(CONTEXTS + "?featureIdentifier=" + feature + "&lifecycle=INACTIVE").then().statusCode(200)
            .body("content[0].identifier", equalTo(context));
        request().delete(FEATURES + "/" + feature).then().statusCode(405);
        request().get(CONTEXTS + "?featureIdentifier=" + feature + "&lifecycle=INACTIVE").then().statusCode(200)
            .body("content[0].identifier", equalTo(context));
        request().delete(CONTEXTS + "/" + context).then().statusCode(400);
        request().delete(relationship).then().statusCode(204);
        request().delete(CONTEXTS + "/" + context).then().statusCode(204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_feature_context_relations", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM platform_features WHERE lifecycle_code='INACTIVE'", Integer.class)).isEqualTo(1);
    }

    private Map<String, Object> withVersion(Map<String, Object> body) {
        Map<String, Object> update = new java.util.HashMap<>(body);
        update.put("version", 0);
        return update;
    }
    private String createMicroservice(String code) {
        return request().contentType(ContentType.JSON).body(Map.of("code", code, "name", code, "settings", Map.of()))
            .post(MICROSERVICES).then().statusCode(201).extract().path("identifier");
    }
    private String createFeature(String microservice) {
        return request().contentType(ContentType.JSON).body(Map.of("code", "route-feature", "name", "Route feature", "microserviceIdentifier", microservice, "settings", Map.of()))
            .post(FEATURES).then().statusCode(201).extract().path("identifier");
    }
    private String createContext() {
        return request().contentType(ContentType.JSON).body(Map.of("code", "route-context", "name", "Route context"))
            .post(CONTEXTS).then().statusCode(201).extract().path("identifier");
    }
    private RequestSpecification request() {
        return given().port(port).header("Authorization", "Bearer platform-contract-review").header("correlation-id", "platform-contract-review").accept(ContentType.JSON);
    }
}
