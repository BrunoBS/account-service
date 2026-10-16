package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicparameter;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.observability.logging.config.PlatformObservabilityProperties;
import br.com.portalmanager.platform.library.observability.logging.converter.JsonMdcConverter;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.repository.parameterkey.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyQueryService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.validation.parameterkey.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@TestPropertySource(properties = "platform.audit.enabled=true")
class ParameterKeyApiIT {
    private static final String BASE = "/api/v1/dynamic-parameters";
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PlatformObservabilityProperties observability;
    @Autowired private AuthorizationMock authorization;
    @Autowired private ParameterKeyQueryService queries;
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @MockitoSpyBean private AuditOutboxAppender appender;
    @MockitoSpyBean private ParameterKeyRepository repository;
    @MockitoSpyBean private ParameterKeyValidator validator;
    private String microservice;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        reset(appender, repository, validator);
        microservice = createMicroservice("account-service");
    }

    @Test
    void createsNormalizedDraftAndReturnsOnlyPublicMetadataWithoutPublishing() {
        String identifier =
                create(body(" retry.attempts ", " configuration ", Map.of("attempts", 3)));
        request()
                .get(BASE + "/" + identifier)
                .then()
                .statusCode(200)
                .body("key", equalTo("retry.attempts"))
                .body("fullKey", equalTo("account-service.retry.attempts"))
                .body("version", equalTo(0))
                .body("type", equalTo("CONFIGURATION"))
                .body("status", equalTo("DRAFT"))
                .body("active", equalTo(true))
                .body("value.attempts", equalTo(3))
                .body("microserviceIdentifier", equalTo(microservice))
                .body("$", not(hasKey("id")))
                .body("$", not(hasKey("microserviceId")))
                .body("$", not(hasKey("publishedValue")));
        assertThat(UUID.fromString(identifier)).isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_value IS NULL AND published_type_code IS NULL"
                                        + " FROM parameter_keys",
                                Boolean.class))
                .isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "SELECT payload FROM audit_outbox WHERE"
                                    + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='PARAMETER_KEY_CREATED'",
                                String.class))
                .doesNotContain(
                        "attempts\": 3", "\"value\"", "publishedValue", "\"microserviceId\"");
    }

    @Test
    void storesEveryJsonShapeIncludingLiteralNullAndKeepsSqlNullDistinct() {
        for (String json :
                List.of("null", "\"literal\"", "123", "true", "[1,2]", "{\"enabled\":true}")) {
            String key = "shape-" + UUID.randomUUID();
            String identifier =
                    request()
                            .contentType(ContentType.JSON)
                            .body(
                                    "{\"microserviceIdentifier\":\""
                                            + microservice
                                            + "\",\"key\":\""
                                            + key
                                            + "\",\"type\":\"CONFIGURATION\",\"value\":"
                                            + json
                                            + "}")
                            .post(BASE)
                            .then()
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            String value =
                    request()
                            .get(BASE + "/" + identifier)
                            .then()
                            .statusCode(200)
                            .extract()
                            .asString();
            assertThat(mapper.readTree(value).get("value")).isEqualTo(mapper.readTree(json));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT value IS NULL FROM parameter_keys WHERE identifier=?",
                                    Boolean.class,
                                    identifier))
                    .isFalse();
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT published_value IS NULL FROM parameter_keys WHERE"
                                            + " identifier=?",
                                    Boolean.class,
                                    identifier))
                    .isTrue();
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM parameter_keys WHERE JSON_TYPE(value)='NULL'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void usesNativeMysqlJsonDecimalPrecisionAsApproved() {
        String precise = "19.900000000000000000001";
        BigDecimal storedDecimal = new BigDecimal("19.9");
        String identifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(
                                "{\"microserviceIdentifier\":\""
                                        + microservice
                                        + "\",\"key\":\"amount\",\"type\":\"CONFIGURATION\",\"value\":"
                                        + precise
                                        + "}")
                        .post(BASE)
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        JsonNode response =
                mapper.readTree(
                        request()
                                .get(BASE + "/" + identifier)
                                .then()
                                .statusCode(200)
                                .extract()
                                .asString());
        assertThat(response.get("value").decimalValue())
                .as(
                        "HTTP value; persisted JSON: %s",
                        jdbc.queryForObject(
                                "SELECT value FROM parameter_keys WHERE identifier=?",
                                String.class,
                                identifier))
                .isEqualByComparingTo(storedDecimal);
        assertThat(
                        mapper.readTree(
                                        jdbc.queryForObject(
                                                "SELECT value FROM parameter_keys WHERE"
                                                        + " identifier=?",
                                                String.class,
                                                identifier))
                                .decimalValue())
                .isEqualByComparingTo(storedDecimal);
        String expressionIdentifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(
                                expressionBody(
                                        "decimal", "GREATER_THAN_OR_EQUALS", "DECIMAL", "19.90"))
                        .post(BASE)
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        JsonNode expression =
                mapper.readTree(
                        request()
                                .get(BASE + "/" + expressionIdentifier)
                                .then()
                                .statusCode(200)
                                .extract()
                                .asString());
        assertThat(expression.get("value").get("value").decimalValue())
                .isEqualByComparingTo(storedDecimal);
    }

    @Test
    void validatesDecimalHundredthsForScalarsAndListsOnCreateAndUpdate() {
        String editable = create(body("decimal-editable", "CONFIGURATION", 1));
        int version = 0;
        for (String value :
                List.of(
                        "-19.99", "-0.29", "-0.01", "0", "0.29", "19", "19.9", "19.90", "19.99",
                        "1.23e2")) {
            for (String operator : List.of("EQUALS", "CONTAINS")) {
                String operand = operator.equals("CONTAINS") ? "[" + value + "]" : value;
                String expression =
                        "{\"operator\":\""
                                + operator
                                + "\",\"valueType\":\"DECIMAL\",\"value\":"
                                + operand
                                + "}";
                request()
                        .contentType(ContentType.JSON)
                        .body(rawBody("decimal-" + UUID.randomUUID(), "EXPRESSION", expression))
                        .post(BASE)
                        .then()
                        .statusCode(201);
                version =
                        request()
                                .contentType(ContentType.JSON)
                                .body(
                                        "{\"version\":"
                                                + version
                                                + ",\"type\":\"EXPRESSION\",\"value\":"
                                                + expression
                                                + "}")
                                .put(BASE + "/" + editable)
                                .then()
                                .statusCode(200)
                                .extract()
                                .path("version");
            }
        }
        clearInvocations(validator);
        for (String value : List.of("19.901", "-0.001", "0.291", "19.999")) {
            for (String operator : List.of("EQUALS", "CONTAINS")) {
                String operand = operator.equals("CONTAINS") ? "[19.99," + value + "]" : value;
                String expression =
                        "{\"operator\":\""
                                + operator
                                + "\",\"valueType\":\"DECIMAL\",\"value\":"
                                + operand
                                + "}";
                request()
                        .contentType(ContentType.JSON)
                        .body(rawBody("invalid-decimal", "EXPRESSION", expression))
                        .post(BASE)
                        .then()
                        .statusCode(400);
                request()
                        .contentType(ContentType.JSON)
                        .body(
                                "{\"version\":"
                                        + version
                                        + ",\"type\":\"EXPRESSION\",\"value\":"
                                        + expression
                                        + "}")
                        .put(BASE + "/" + editable)
                        .then()
                        .statusCode(400);
            }
        }
        verify(validator, never()).validateCreateInput(any());
        verify(validator, never()).validateUpdate(any(), any());
        request()
                .get(BASE + "/" + editable)
                .then()
                .statusCode(200)
                .body("version", equalTo(version));
    }

    @Test
    void appliesExactLongBoundariesToScalarsAndListsUsingThePublishedSchema() {
        for (String value :
                List.of(
                        "-9223372036854775808",
                        "-9223372036854775807",
                        "9223372036854775806",
                        "9223372036854775807",
                        "-9223372036854775809",
                        "9223372036854775808")) {
            int status =
                    value.equals("-9223372036854775809") || value.equals("9223372036854775808")
                            ? 400
                            : 201;
            request()
                    .contentType(ContentType.JSON)
                    .body(expressionBody("long-" + UUID.randomUUID(), "EQUALS", "LONG", value))
                    .post(BASE)
                    .then()
                    .statusCode(status);
            request()
                    .contentType(ContentType.JSON)
                    .body(
                            expressionBody(
                                    "long-" + UUID.randomUUID(),
                                    "CONTAINS",
                                    "LONG",
                                    "[" + value + "]"))
                    .post(BASE)
                    .then()
                    .statusCode(status);
        }
    }

    @Test
    void validatesAllExpressionOperatorsWithTheSinglePublishedSchema() {
        String editable = create(body("editable", "CONFIGURATION", 1));
        for (Map.Entry<String, String> operand :
                Map.of(
                                "STRING",
                                "\"PRD\"",
                                "INTEGER",
                                "10",
                                "LONG",
                                "9223372036854775807",
                                "DECIMAL",
                                "19.99")
                        .entrySet()) {
            for (String operator :
                    List.of(
                            "EQUALS",
                            "NOT_EQUALS",
                            "CONTAINS",
                            "NOT_CONTAINS",
                            "GREATER_THAN",
                            "GREATER_THAN_OR_EQUALS",
                            "LESS_THAN",
                            "LESS_THAN_OR_EQUALS")) {
                boolean membership = operator.equals("CONTAINS") || operator.equals("NOT_CONTAINS");
                boolean ordering =
                        !membership && !operator.equals("EQUALS") && !operator.equals("NOT_EQUALS");
                String value = membership ? "[" + operand.getValue() + "]" : operand.getValue();
                int status = ordering && operand.getKey().equals("STRING") ? 400 : 201;
                request()
                        .contentType(ContentType.JSON)
                        .body(
                                expressionBody(
                                        "operator-" + UUID.randomUUID(),
                                        operator,
                                        operand.getKey(),
                                        value))
                        .post(BASE)
                        .then()
                        .statusCode(status);
            }
        }
        request()
                .contentType(ContentType.JSON)
                .body(expressionBody("string", "EQUALS", "STRING", "\"PRD\""))
                .post(BASE)
                .then()
                .statusCode(201);
        clearInvocations(validator);
        for (String expression :
                List.of(
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"LONG\",\"value\":[10,\"20\"]}",
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"LONG\",\"value\":10}",
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"LONG\",\"value\":[]}",
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"LONG\",\"value\":[10,10]}",
                        "{\"operator\":\"GREATER_THAN\",\"valueType\":\"STRING\",\"value\":\"ABC\"}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"LONG\",\"value\":null}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"INTEGER\",\"value\":2147483648}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"INTEGER\",\"value\":10.5}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"LONG\",\"value\":10.5}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"STRING\",\"value\":[\"PRD\"]}",
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"STRING\",\"value\":[\"PRD\",10]}",
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"INTEGER\",\"value\":[10,10.5]}",
                        "{\"operator\":\"CONTAINS\",\"valueType\":\"DECIMAL\",\"value\":[19.99,\"19.90\"]}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"DECIMAL\",\"value\":[19.99]}",
                        "{\"operator\":\"EQUALS\",\"valueType\":\"LONG\",\"value\":10,\"extra\":true}",
                        "{\"operator\":\"AND\",\"conditions\":[]}",
                        "null",
                        "true")) {
            request()
                    .contentType(ContentType.JSON)
                    .body(rawBody("invalid", "EXPRESSION", expression))
                    .post(BASE)
                    .then()
                    .statusCode(400);
            request()
                    .contentType(ContentType.JSON)
                    .body("{\"version\":0,\"type\":\"EXPRESSION\",\"value\":" + expression + "}")
                    .put(BASE + "/" + editable)
                    .then()
                    .statusCode(400);
        }
        verify(validator, never()).validateCreateInput(any());
        verify(validator, never()).validateUpdate(any(), any());
    }

    @Test
    void rejectsCoercionUnknownFieldsAndImmutablePropertiesOnCreateAndUpdate() {
        String identifier = create(body("setting", "CONFIGURATION", "unchanged"));
        for (Map<String, Object> change :
                List.<Map<String, Object>>of(
                        Map.of("key", 123),
                        Map.of("description", false),
                        Map.of("type", true),
                        Map.of("key", "contains spaces"),
                        Map.of("key", "config:bad"),
                        Map.of("key", "k".repeat(81)),
                        Map.of("description", "d".repeat(201)),
                        Map.of("name", "forbidden"),
                        Map.of("id", 1),
                        Map.of("active", false),
                        Map.of("publishedValue", "forbidden"),
                        Map.of("status", "PUBLISHED"))) {
            Map<String, Object> invalid = body("invalid", "CONFIGURATION", 1);
            invalid.putAll(change);
            request().contentType(ContentType.JSON).body(invalid).post(BASE).then().statusCode(400);
        }
        for (Map<String, Object> change :
                List.<Map<String, Object>>of(
                        Map.of("key", "changed"),
                        Map.of("microserviceIdentifier", microservice),
                        Map.of("description", true),
                        Map.of("type", 1),
                        Map.of("version", "0"),
                        Map.of("version", 0.5),
                        Map.of("version", -1),
                        Map.of("name", "forbidden"))) {
            Map<String, Object> invalid =
                    new HashMap<>(Map.of("version", 0, "type", "CONFIGURATION", "value", "new"));
            invalid.putAll(change);
            request()
                    .contentType(ContentType.JSON)
                    .body(invalid)
                    .put(BASE + "/" + identifier)
                    .then()
                    .statusCode(400);
        }
        request()
                .contentType(ContentType.JSON)
                .body(body("changed", "CONFIGURATION", 9))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(400);
        for (String invalid : List.of("null", "[]", "{\"type\":\"CONFIGURATION\"}")) {
            request().contentType(ContentType.JSON).body(invalid).post(BASE).then().statusCode(400);
            request()
                    .contentType(ContentType.JSON)
                    .body(invalid)
                    .put(BASE + "/" + identifier)
                    .then()
                    .statusCode(400);
        }
        request()
                .get(BASE + "/" + identifier)
                .then()
                .statusCode(200)
                .body("value", equalTo("unchanged"));
    }

    @Test
    void keysAreLowercaseUniquePerMicroserviceAndRemainReservedWhenInactive() {
        String identifier = create(body("retry.attempts", "CONFIGURATION", 3));
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(200);
        request()
                .contentType(ContentType.JSON)
                .body(body("retry.attempts", "CONFIGURATION", 4))
                .post(BASE)
                .then()
                .statusCode(409)
                .body("code", equalTo("PARAMETER-KEY-0002"));
        for (String key : List.of("Retry.attempts", "retryAttempts", "RETRY", "rétry")) {
            request()
                    .contentType(ContentType.JSON)
                    .body(body(key, "CONFIGURATION", 5))
                    .post(BASE)
                    .then()
                    .statusCode(400);
        }
        for (String key : List.of("retry_attempts", "retry-max-attempts", "7")) {
            String accepted = create(body(key, "CONFIGURATION", 5));
            request().get(BASE + "/" + accepted).then().statusCode(200).body("key", equalTo(key));
        }
        String other = createMicroservice("other-service");
        Map<String, Object> otherBody = body("retry.attempts", "CONFIGURATION", 6);
        otherBody.put("microserviceIdentifier", other);
        create(otherBody);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parameter_keys", Integer.class))
                .isEqualTo(5);
    }

    @Test
    void updatePreservesThePublishedSnapshotAndDescriptionAloneKeepsPublishedStatus() {
        String identifier = create(body("snapshot", "CONFIGURATION", Map.of("attempts", 3)));
        jdbc.update(
                "UPDATE parameter_keys SET"
                    + " published_value=value,published_type_code=type_code,status_code='PUBLISHED'"
                    + " WHERE identifier=?",
                identifier);
        request()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "version",
                                0,
                                "description",
                                "Only description",
                                "type",
                                "CONFIGURATION",
                                "value",
                                Map.of("attempts", 3)))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"))
                .body("version", equalTo(1));
        request()
                .contentType(ContentType.JSON)
                .body(
                        Map.of(
                                "version",
                                1,
                                "type",
                                "EXPRESSION",
                                "value",
                                Map.of("operator", "EQUALS", "valueType", "LONG", "value", 10)))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(200)
                .body("status", equalTo("DRAFT"))
                .body("version", equalTo(2));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_type_code FROM parameter_keys WHERE identifier=?",
                                String.class,
                                identifier))
                .isEqualTo("CONFIGURATION");
        assertThat(
                        mapper.readTree(
                                jdbc.queryForObject(
                                        "SELECT published_value FROM parameter_keys WHERE"
                                                + " identifier=?",
                                        String.class,
                                        identifier)))
                .isEqualTo(mapper.readTree("{\"attempts\":3}"));
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 1, "type", "CONFIGURATION", "value", 1))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(409)
                .body("code", equalTo("GLOBAL-0009"));
    }

    @Test
    void updatingOnlyDescriptionWithDecimalValuesKeepsPublishedStatusUsingJacksonDefaults() {
        for (String type : List.of("CONFIGURATION", "EXPRESSION")) {
            Object value =
                    type.equals("CONFIGURATION")
                            ? 19.9
                            : Map.of("operator", "EQUALS", "valueType", "DECIMAL", "value", 19.9);
            String identifier =
                    create(body("decimal-" + type.toLowerCase(java.util.Locale.ROOT), type, value));
            jdbc.update(
                    "UPDATE parameter_keys SET"
                        + " published_value=value,published_type_code=type_code,status_code='PUBLISHED'"
                        + " WHERE identifier=?",
                    identifier);
            String published =
                    jdbc.queryForObject(
                            "SELECT published_value FROM parameter_keys WHERE identifier=?",
                            String.class,
                            identifier);
            request()
                    .contentType(ContentType.JSON)
                    .body(
                            Map.of(
                                    "version",
                                    0,
                                    "description",
                                    "Only description",
                                    "type",
                                    type,
                                    "value",
                                    value))
                    .put(BASE + "/" + identifier)
                    .then()
                    .statusCode(200)
                    .body("status", equalTo("PUBLISHED"))
                    .body("version", equalTo(1));
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT published_value FROM parameter_keys WHERE identifier=?",
                                    String.class,
                                    identifier))
                    .isEqualTo(published);
        }
    }

    @Test
    void lifecycleRetriesDoNotIncrementVersionOrCreateDuplicateAuditAndDeleteRequiresInactivity() {
        String identifier = create(body("lifecycle", "CONFIGURATION", "literal"));
        request()
                .patch(BASE + "/" + identifier + "/activate")
                .then()
                .statusCode(200)
                .body("version", equalTo(0));
        request()
                .delete(BASE + "/" + identifier)
                .then()
                .statusCode(400)
                .body("code", equalTo("PARAMETER-KEY-0106"));
        jdbc.update(
                "UPDATE parameter_keys SET"
                    + " published_value=value,published_type_code=type_code,status_code='PUBLISHED'"
                    + " WHERE identifier=?",
                identifier);
        request()
                .patch(BASE + "/" + identifier + "/inactivate")
                .then()
                .statusCode(200)
                .body("version", equalTo(1))
                .body("status", equalTo("DRAFT"));
        request()
                .patch(BASE + "/" + identifier + "/inactivate")
                .then()
                .statusCode(200)
                .body("version", equalTo(1));
        assertThat(
                        jdbc.queryForObject(
                                "SELECT published_value IS NULL AND published_type_code IS NULL"
                                        + " FROM parameter_keys",
                                Boolean.class))
                .isTrue();
        request()
                .patch(BASE + "/" + identifier + "/activate")
                .then()
                .statusCode(200)
                .body("version", equalTo(2))
                .body("status", equalTo("DRAFT"));
        request()
                .patch(BASE + "/" + identifier + "/activate")
                .then()
                .statusCode(200)
                .body("version", equalTo(2));
        request()
                .patch(BASE + "/" + identifier + "/inactivate")
                .then()
                .statusCode(200)
                .body("version", equalTo(3));
        request().delete(BASE + "/" + identifier).then().statusCode(204);
        request().get(BASE + "/" + identifier).then().statusCode(404);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_outbox WHERE"
                                    + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceType'))='PARAMETER_KEY'",
                                Integer.class))
                .isEqualTo(5);
    }

    @Test
    void paginatesAndFiltersIncludingInactiveRowsWithStableSortAndBatchedReferences() {
        String first = create(body("a", "CONFIGURATION", 1));
        create(
                body(
                        "b",
                        "EXPRESSION",
                        Map.of("operator", "EQUALS", "valueType", "LONG", "value", 10)));
        create(body("c", "CONFIGURATION", 3));
        request()
                .get(BASE + "?size=1&sort=key,desc")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(3))
                .body("content[0].key", equalTo("c"));
        request()
                .get(BASE + "?size=1&page=1&sort=key,desc")
                .then()
                .statusCode(200)
                .body("content[0].key", equalTo("b"));
        request()
                .queryParam("microserviceIdentifier", microservice)
                .queryParam("type", " expression ")
                .queryParam("status", " draft ")
                .get(BASE)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1));
        request()
                .queryParam("key", "A")
                .get(BASE)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
        request().patch(BASE + "/" + first + "/inactivate").then().statusCode(200);
        request()
                .get(BASE + "?active=false")
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(1))
                .body("content[0].identifier", equalTo(first));
        for (String query :
                List.of(
                        "sort=value",
                        "status=unknown",
                        "type=unknown",
                        "type=",
                        "page=2147483647&size=100",
                        "microserviceIdentifier=invalid",
                        "active=unknown")) {
            request().get(BASE + "?" + query).then().statusCode(400);
        }
        request()
                .queryParam("microserviceIdentifier", UUID.randomUUID())
                .get(BASE)
                .then()
                .statusCode(404);
        org.hibernate.stat.Statistics statistics =
                entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean original = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            assertThat(
                            queries.findAll(
                                            null,
                                            null,
                                            null,
                                            true,
                                            null,
                                            org.springframework.data.domain.PageRequest.of(0, 1))
                                    .getTotalElements())
                    .isEqualTo(2);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(3);
            assertThat(statistics.getEntityFetchCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(original);
        }
    }

    @Test
    void requiresOwnerForEveryAdministrativeOperationAndReturnsTranslatedBusinessErrors() {
        String identifier = create(body("secure", "CONFIGURATION", 1));
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_DEV"));
        authorization.forbiddenResource(resource -> resource.header("policy", "OWNER"));
        request().get(BASE).then().statusCode(403);
        request().get(BASE + "/" + identifier).then().statusCode(403);
        request()
                .contentType(ContentType.JSON)
                .body(body("denied", "CONFIGURATION", 1))
                .post(BASE)
                .then()
                .statusCode(403);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 0, "type", "CONFIGURATION", "value", 1))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(403);
        request().patch(BASE + "/" + identifier + "/activate").then().statusCode(403);
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(403);
        request().delete(BASE + "/" + identifier).then().statusCode(403);
        given().port(port).get(BASE).then().statusCode(401);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        request()
                .header("Accept-Language", "en-US")
                .delete(BASE + "/" + identifier)
                .then()
                .statusCode(400)
                .body("message", equalTo("Inactivate the dynamic parameter before deleting it."));
        request()
                .header("Accept-Language", "pt-BR")
                .delete(BASE + "/" + identifier)
                .then()
                .statusCode(400)
                .body("message", equalTo("Inative o parâmetro dinâmico antes de excluir."));
        request().get(BASE + "/invalid").then().statusCode(400);
        request().get(BASE + "/" + UUID.randomUUID()).then().statusCode(404);
    }

    @Test
    void usesCentralIntegrityHandlingAndRollsBackTheRejectedCreation() {
        create(body("duplicate", "CONFIGURATION", "first"));
        Long parent =
                jdbc.queryForObject(
                        "SELECT id FROM service_context_microservices WHERE identifier=?",
                        Long.class,
                        microservice);
        doReturn(false).when(repository).existsByMicroserviceIdAndKey(parent, "duplicate");
        String conflict =
                request()
                        .contentType(ContentType.JSON)
                        .body(body("duplicate", "CONFIGURATION", "rejected"))
                        .post(BASE)
                        .then()
                        .statusCode(409)
                        .body("code", equalTo("GLOBAL-0004"))
                        .extract()
                        .asString();
        assertThat(conflict).doesNotContain("SQL", "uk_parameter_key", "rejected");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parameter_keys", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_outbox WHERE"
                                    + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceType'))='PARAMETER_KEY'",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void auditFailureRollsBackCreateUpdateBothLifecycleTransitionsAndDelete() {
        String identifier = create(body("rollback", "CONFIGURATION", 1));
        doThrow(new IllegalStateException("Test audit failure"))
                .when(appender)
                .append(any(), any());
        request()
                .contentType(ContentType.JSON)
                .body(body("rejected", "CONFIGURATION", 2))
                .post(BASE)
                .then()
                .statusCode(500);
        Map<String, Object> before = detail(identifier);
        request()
                .contentType(ContentType.JSON)
                .body(Map.of("version", 0, "type", "CONFIGURATION", "value", 2))
                .put(BASE + "/" + identifier)
                .then()
                .statusCode(500);
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(500);
        assertThat(detail(identifier)).isEqualTo(before);
        reset(appender);
        request().patch(BASE + "/" + identifier + "/inactivate").then().statusCode(200);
        before = detail(identifier);
        doThrow(new IllegalStateException("Test audit failure"))
                .when(appender)
                .append(any(), any());
        request().patch(BASE + "/" + identifier + "/activate").then().statusCode(500);
        request().delete(BASE + "/" + identifier).then().statusCode(500);
        assertThat(detail(identifier)).isEqualTo(before);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM parameter_keys", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM audit_outbox WHERE"
                                    + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceType'))='PARAMETER_KEY'",
                                Integer.class))
                .isEqualTo(2);
    }

    @Test
    void configurationValuesNeverAppearInErrorLogsWithPlatformBodyLoggingDisabled() {
        assertThat(observability.getLogging().getRequestBody().isEnabled()).isFalse();
        String sentinel = "PARAMETER_KEY_SENSITIVE_SENTINEL_71";
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        List<String> renderedContexts = new CopyOnWriteArrayList<>();
        ListAppender<ILoggingEvent> logs =
                new ListAppender<>() {
                    @Override
                    protected void append(ILoggingEvent event) {
                        // Render in the request thread, while the converter can still read the
                        // cached body.
                        renderedContexts.add(new JsonMdcConverter().convert(event));
                        super.append(event);
                    }
                };
        logs.start();
        root.addAppender(logs);
        try {
            request()
                    .contentType(ContentType.JSON)
                    .body(
                            body(
                                    "invalid",
                                    "EXPRESSION",
                                    Map.of(
                                            "operator",
                                            "EQUALS",
                                            "valueType",
                                            "LONG",
                                            "value",
                                            sentinel)))
                    .post(BASE)
                    .then()
                    .statusCode(400);
            request()
                    .contentType(ContentType.JSON)
                    .body("{\"value\":" + sentinel)
                    .post(BASE)
                    .then()
                    .statusCode(400);
            assertThat(logs.list).isNotEmpty();
            assertThat(logs.list)
                    .anyMatch(
                            event ->
                                    event.getLevel()
                                            .isGreaterOrEqual(ch.qos.logback.classic.Level.WARN));
            assertThat(renderedContexts)
                    .isNotEmpty()
                    .allSatisfy(
                            context -> assertThat(context).doesNotContain(sentinel, "requestBody"));
            assertThat(logs.list)
                    .allSatisfy(
                            event -> {
                                assertThat(event.getFormattedMessage()).doesNotContain(sentinel);
                                assertThat(event.getMDCPropertyMap().toString())
                                        .doesNotContain(sentinel);
                                if (event.getThrowableProxy() != null)
                                    assertThat(event.getThrowableProxy().getMessage())
                                            .doesNotContain(sentinel);
                            });
        } finally {
            root.detachAppender(logs);
            logs.stop();
        }
    }

    @Test
    void exposesGoldenCatalogsAndProtectsTheGlobalMicroservice() {
        request()
                .get("/api/v1/parameter-key-type")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
        request()
                .get("/api/v1/parameter-key-status-type")
                .then()
                .statusCode(200)
                .body("size()", equalTo(2));
        String global = createMicroservice("platform");
        request()
                .patch("/api/v1/service-contexts/microservices/" + global + "/inactivate")
                .then()
                .statusCode(400)
                .body("code", equalTo("SERVICE-CONTEXT-0128"));
        Map<String, Object> globalBody = body("quarantine.days", "CONFIGURATION", 30);
        globalBody.put("microserviceIdentifier", global);
        String configuration = create(globalBody);
        request()
                .get(BASE + "/" + configuration)
                .then()
                .statusCode(200)
                .body("fullKey", equalTo("platform.quarantine.days"));
    }

    private String createMicroservice(String code) {
        return request()
                .contentType(ContentType.JSON)
                .body(Map.of("code", code, "name", code, "settings", Map.of()))
                .post("/api/v1/service-contexts/microservices")
                .then()
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    private String create(Map<String, Object> body) {
        return request()
                .contentType(ContentType.JSON)
                .body(body)
                .post(BASE)
                .then()
                .statusCode(201)
                .body("$", not(hasKey("value")))
                .extract()
                .path("identifier");
    }

    private Map<String, Object> body(String key, String type, Object value) {
        return new HashMap<>(
                Map.of(
                        "microserviceIdentifier",
                        microservice,
                        "key",
                        key,
                        "type",
                        type,
                        "value",
                        value));
    }

    private String rawBody(String key, String type, String value) {
        return "{\"microserviceIdentifier\":\""
                + microservice
                + "\",\"key\":\""
                + key
                + "\",\"type\":\""
                + type
                + "\",\"value\":"
                + value
                + "}";
    }

    private String expressionBody(String key, String operator, String type, String value) {
        return rawBody(
                key,
                "EXPRESSION",
                "{\"operator\":\""
                        + operator
                        + "\",\"valueType\":\""
                        + type
                        + "\",\"value\":"
                        + value
                        + "}");
    }

    private Map<String, Object> detail(String identifier) {
        return request()
                .get(BASE + "/" + identifier)
                .then()
                .statusCode(200)
                .extract()
                .as(new TypeRef<Map<String, Object>>() {});
    }

    private RequestSpecification request() {
        return given().port(port)
                .header("Authorization", "Bearer dynamic-parameter-review")
                .header("correlation-id", "dynamic-parameter-review")
                .accept(ContentType.JSON);
    }
}
