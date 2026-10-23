package br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MessageApiIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{

    private static final String MICROSERVICE_IDENTIFIER = "11111111-1111-1111-1111-111111111111";

    @LocalServerPort private int port;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
        SchemaDefaultFixture.seed(jdbcTemplate);
        seedLifecycleTypes();
        seedPlatformMicroservices();
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldCreateMessageWithTranslatorsInSingleRequest() {
        Map<String, Object> body = message("workspace.batch.not-found", "WORKSPACE-0201");
        body.put(
                "translators",
                List.of(
                        translator(
                                "pt-br",
                                "Workspace não encontrado",
                                "Mensagem customizada em português.",
                                "Revise o identificador informado."),
                        translator(
                                "en-us",
                                "Workspace not found",
                                "Customized message in English.",
                                "Review the provided identifier.")));

        String identifier =
                post(body)
                        .statusCode(201)
                        .body("microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
                        .extract()
                        .path("identifier");

        Long messageId =
                jdbcTemplate.queryForObject(
                        "select id from messages where identifier = ?", Long.class, identifier);

        Integer translatorCount =
                jdbcTemplate.queryForObject(
                        "select count(*) from message_translators where message_id = ?",
                        Integer.class,
                        messageId);

        Long microserviceId =
                jdbcTemplate.queryForObject(
                        "select id from service_context_microservices where identifier = ?",
                        Long.class,
                        MICROSERVICE_IDENTIFIER);
        Long persistedMicroserviceId =
                jdbcTemplate.queryForObject(
                        "select microservice_id from messages where identifier = ?",
                        Long.class,
                        identifier);

        assertThat(persistedMicroserviceId).isEqualTo(microserviceId);
        assertThat(translatorCount).isEqualTo(2);
        assertThat(viewCount("workspace-service.workspace.batch.not-found", "pt-BR")).isEqualTo(1);
        assertThat(viewCount("workspace-service.workspace.batch.not-found", "en-US")).isEqualTo(1);
    }

    @Test
    void shouldIgnoreNullOrEmptyTranslatorsWhenCreatingMessage() {
        Map<String, Object> withNull = message("workspace.null-translators", "WORKSPACE-0202");
        withNull.put("translators", null);

        String nullIdentifier = post(withNull).statusCode(201).extract().path("identifier");

        Map<String, Object> withEmpty = message("workspace.empty-translators", "WORKSPACE-0203");
        withEmpty.put("translators", List.of());

        String emptyIdentifier = post(withEmpty).statusCode(201).extract().path("identifier");

        assertThat(translatorCount(nullIdentifier)).isZero();
        assertThat(translatorCount(emptyIdentifier)).isZero();
    }

    @Test
    void shouldRollbackMessageWhenInlineTranslatorsContainDuplicateLocale() {
        Map<String, Object> body = message("workspace.duplicate-inline-locale", "WORKSPACE-0204");
        body.put(
                "translators",
                List.of(
                        translator("pt-br", "Título um", "Detalhe um", "Sugestão um"),
                        translator("pt-BR", "Título dois", "Detalhe dois", "Sugestão dois")));

        post(body)
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("locale"));

        Integer count =
                jdbcTemplate.queryForObject(
                        "select count(*) from messages where message_key = ?",
                        Integer.class,
                        "workspace.duplicate-inline-locale");
        assertThat(count).isZero();
    }

    @Test
    void shouldCreateTranslatorAndExposeOnlyActiveEntriesInView() {
        String messageIdentifier = createMessage("workspace.not-found", "WORKSPACE-0001");

        String translatorIdentifier =
                postTranslator(
                                messageIdentifier,
                                translator(
                                        "pt-br",
                                        "Workspace não encontrado",
                                        "Mensagem customizada pelo catálogo.",
                                        "Revise o identificador informado."))
                        .statusCode(201)
                        .body("locale", equalTo("pt-BR"))
                        .body("lifecycle", equalTo("ACTIVE"))
                        .extract()
                        .path("identifier");

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR")).isEqualTo(1);

        patch("/api/v1/dynamic-bundles/messages/" + messageIdentifier + "/inactivate")
                .statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"));

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR")).isZero();

        patch("/api/v1/dynamic-bundles/messages/" + messageIdentifier + "/activate")
                .statusCode(200);

        patch(
                        "/api/v1/dynamic-bundles/messages/"
                                + messageIdentifier
                                + "/translators/"
                                + translatorIdentifier
                                + "/inactivate")
                .statusCode(200);

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR")).isZero();
    }

    @Test
    void shouldFindTranslatorByIdentifierAndFilterTranslators() {
        String messageIdentifier = createMessage("workspace.translator-query", "WORKSPACE-0205");

        String ptIdentifier =
                postTranslator(
                                messageIdentifier,
                                translator("pt-br", "Título PT", "Detalhe PT", "Sugestão PT"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");

        String enIdentifier =
                postTranslator(
                                messageIdentifier,
                                translator("en-us", "Title EN", "Detail EN", "Suggestion EN"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");

        get("/api/v1/dynamic-bundles/messages/"
                        + messageIdentifier
                        + "/translators/"
                        + ptIdentifier)
                .statusCode(200)
                .body("identifier", equalTo(ptIdentifier))
                .body("locale", equalTo("pt-BR"));

        patch(
                        "/api/v1/dynamic-bundles/messages/"
                                + messageIdentifier
                                + "/translators/"
                                + enIdentifier
                                + "/inactivate")
                .statusCode(200);

        get("/api/v1/dynamic-bundles/messages/" + messageIdentifier + "/translators?locale=pt-br")
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].locale", equalTo("pt-BR"));

        get("/api/v1/dynamic-bundles/messages/"
                        + messageIdentifier
                        + "/translators?lifecycle=INACTIVE")
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].locale", equalTo("en-US"));
    }

    @Test
    void shouldFilterMessagesByServiceLifecycleCodeAndMessageKey() {
        String activeIdentifier = createMessage("workspace.query.active", "WORKSPACE-0206");
        String inactiveIdentifier = createMessage("workspace.query.inactive", "WORKSPACE-0207");

        patch("/api/v1/dynamic-bundles/messages/" + inactiveIdentifier + "/inactivate")
                .statusCode(200);

        get("/api/v1/dynamic-bundles/messages?microserviceIdentifier="
                        + MICROSERVICE_IDENTIFIER
                        + "&lifecycle=ACTIVE&code=workspace-0206")
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].identifier", equalTo(activeIdentifier));

        get("/api/v1/dynamic-bundles/messages?messageKey=workspace.query.inactive&lifecycle=INACTIVE")
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].identifier", equalTo(inactiveIdentifier));
    }

    @Test
    void shouldKeepAdministrativeReadAvailableWhenServiceBecomesInactive() {
        String identifier = createMessage("workspace.service.inactive", "WORKSPACE-0208");
        postTranslator(
                        identifier,
                        translator(
                                "pt-BR",
                                "Service inactive",
                                "Message owned by a service that becomes inactive.",
                                "Reactivate the service to expose the runtime message."))
                .statusCode(201);

        assertThat(viewCount("workspace-service.workspace.service.inactive", "pt-BR")).isEqualTo(1);

        Integer version =
                get("/api/v1/dynamic-bundles/messages/" + identifier)
                        .statusCode(200)
                        .extract()
                        .path("version");

        jdbcTemplate.update(
                "update service_context_microservices set lifecycle_code = 'INACTIVE' where"
                    + " identifier = ?",
                MICROSERVICE_IDENTIFIER);

        get("/api/v1/dynamic-bundles/messages?microserviceIdentifier=" + MICROSERVICE_IDENTIFIER)
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].identifier", equalTo(identifier))
                .body("content[0].microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
                .body("content[0]", not(hasKey("microserviceId")));

        get("/api/v1/dynamic-bundles/messages/" + identifier)
                .statusCode(200)
                .body("microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
                .body("$", not(hasKey("microserviceId")));

        assertThat(viewCount("workspace-service.workspace.service.inactive", "pt-BR")).isZero();

        post(message("workspace.service.inactive.new", "WORKSPACE-0209"))
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("microserviceIdentifier"));

        Map<String, Object> update = message("workspace.service.inactive", "WORKSPACE-0208");
        update.put("version", version);

        put("/api/v1/dynamic-bundles/messages/" + identifier, update)
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("microserviceIdentifier"));
    }

    @Test
    void shouldRejectDuplicateMessageKeyAndCodeInsideSameService() {
        createMessage("validation.name.required", "WORKSPACE-0102");

        post(message("validation.name.required", "WORKSPACE-0199"))
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("messageKey"));

        post(message("validation.other.required", "WORKSPACE-0102"))
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("code"));
    }

    @Test
    void shouldRejectDuplicateTranslatorLocaleAfterNormalization() {
        String messageIdentifier = createMessage("validation.email.invalid", "WORKSPACE-0109");

        postTranslator(
                        messageIdentifier,
                        translator("pt-BR", "E-mail inválido", "Detalhe um", "Sugestão um"))
                .statusCode(201);

        postTranslator(
                        messageIdentifier,
                        translator("pt-br", "E-mail inválido", "Detalhe dois", "Sugestão dois"))
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("locale"));
    }

    @Test
    void shouldPhysicallyDeleteMessageOnlyWhenInactive() {
        String identifier = createMessage("workspace.delete.invalid", "WORKSPACE-0003");

        delete("/api/v1/dynamic-bundles/messages/" + identifier)
                .statusCode(400)
                .body("code", equalTo("MESSAGE-0002"));

        patch("/api/v1/dynamic-bundles/messages/" + identifier + "/inactivate").statusCode(200);

        delete("/api/v1/dynamic-bundles/messages/" + identifier).statusCode(204);

        get("/api/v1/dynamic-bundles/messages/" + identifier).statusCode(404);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM messages WHERE identifier=?",
                                Integer.class,
                                identifier))
                .isZero();
    }

    @Test
    void shouldUpdateUsingOptimisticVersion() {
        String identifier = createMessage("workspace.restore.invalid", "WORKSPACE-0002");

        Integer version =
                get("/api/v1/dynamic-bundles/messages/" + identifier)
                        .statusCode(200)
                        .body("identifier", notNullValue())
                        .extract()
                        .path("version");

        Map<String, Object> update = message("workspace.restore.invalid", "WORKSPACE-0002");
        update.put("version", version);
        update.put("observation", "Mensagem administrativa atualizada");

        put("/api/v1/dynamic-bundles/messages/" + identifier, update)
                .statusCode(200)
                .body("observation", equalTo("Mensagem administrativa atualizada"));
    }

    @Test
    void shouldDeleteInactiveMessageWithActiveAndInactiveTranslatorsAndAllowKeyReuse() {
        String identifier = createMessage("review.delete", "REVIEW-001");
        String path = "/api/v1/dynamic-bundles/messages/" + identifier;
        String activeTranslator =
                postTranslator(identifier, translator("pt-BR", "Title", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String inactiveTranslator =
                postTranslator(identifier, translator("en-US", "Title", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        patch(path + "/translators/" + inactiveTranslator + "/inactivate").statusCode(200);
        delete(path).statusCode(400);
        assertThat(translatorCount(identifier)).isEqualTo(2);
        patch(path + "/inactivate").statusCode(200);
        delete(path).statusCode(204);
        get(path).statusCode(404);
        patch(path + "/activate").statusCode(404);
        delete(path).statusCode(404);
        Map<String, Object> update = message("review.delete", "REVIEW-001");
        update.put("version", 1);
        put(path, update).statusCode(404);
        postTranslator(identifier, translator("en-US", "Title", "Detail", "Suggestion"))
                .statusCode(404);
        for (String childIdentifier : List.of(activeTranslator, inactiveTranslator)) {
            String child = path + "/translators/" + childIdentifier;
            get(child).statusCode(404);
            patch(child + "/inactivate").statusCode(404);
            patch(child + "/activate").statusCode(404);
            delete(child).statusCode(404);
        }
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM messages WHERE identifier=?",
                                Integer.class,
                                identifier))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT COUNT(*) FROM message_translators", Integer.class))
                .isZero();
        createMessage("review.delete", "REVIEW-001");
    }

    @Test
    void shouldPreserveTranslatorsWhenParentIsInactiveButRejectNewLinksAndActivation() {
        String identifier = createMessage("review.parent.inactive", "REVIEW-002");
        String translatorIdentifier =
                postTranslator(identifier, translator("pt-BR", "Title", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String child =
                "/api/v1/dynamic-bundles/messages/"
                        + identifier
                        + "/translators/"
                        + translatorIdentifier;
        patch("/api/v1/dynamic-bundles/messages/" + identifier + "/inactivate").statusCode(200);
        get(child).statusCode(200).body("lifecycle", equalTo("ACTIVE"));
        postTranslator(identifier, translator("en-US", "Title", "Detail", "Suggestion"))
                .statusCode(400);
        Map<String, Object> update = translator("pt-BR", "Edited", "Detail", "Suggestion");
        update.put("version", 0);
        put(child, update)
                .statusCode(200)
                .body("title", equalTo("Edited"))
                .body("version", equalTo(1));
        patch(child + "/inactivate").statusCode(200);
        patch(child + "/activate").statusCode(400);
        patch("/api/v1/dynamic-bundles/messages/" + identifier + "/activate").statusCode(200);
        patch(child + "/activate").statusCode(200);
    }

    @Test
    void shouldPhysicallyDeleteInactiveTranslatorAndRejectForeignIdentifiers() {
        String identifier = createMessage("review.child.delete", "REVIEW-003");
        String other = createMessage("review.other.message", "REVIEW-004");
        String translatorIdentifier =
                postTranslator(identifier, translator("pt-BR", "Title", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String child =
                "/api/v1/dynamic-bundles/messages/"
                        + identifier
                        + "/translators/"
                        + translatorIdentifier;
        get("/api/v1/dynamic-bundles/messages/" + other + "/translators/" + translatorIdentifier)
                .statusCode(404);
        patch(
                        "/api/v1/dynamic-bundles/messages/"
                                + other
                                + "/translators/"
                                + translatorIdentifier
                                + "/inactivate")
                .statusCode(404);
        delete("/api/v1/dynamic-bundles/messages/" + other + "/translators/" + translatorIdentifier)
                .statusCode(404);
        delete(child).statusCode(400);
        patch(child + "/inactivate").statusCode(200);
        delete(child).statusCode(204);
        patch(child + "/activate").statusCode(404);
        patch(child + "/inactivate").statusCode(404);
        Map<String, Object> update = translator("pt-BR", "Changed", "Detail", "Suggestion");
        update.put("version", 2);
        put(child, update).statusCode(404);
        get(child).statusCode(404);
        delete(child).statusCode(404);
        get("/api/v1/dynamic-bundles/messages/" + identifier)
                .statusCode(200)
                .body("lifecycle", equalTo("ACTIVE"));
        assertThat(translatorCount(identifier)).isZero();
        postTranslator(identifier, translator("pt-BR", "New title", "Detail", "Suggestion"))
                .statusCode(201);
    }

    @Test
    void shouldDeleteInactiveTranslatorUnderInactiveMessageWithoutDeletingSiblings() {
        String identifier = createMessage("review.isolated.delete", "REVIEW-010");
        String resource = "/api/v1/dynamic-bundles/messages/" + identifier;
        String child =
                postTranslator(identifier, translator("pt-BR", "Title", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String sibling =
                postTranslator(identifier, translator("en-US", "Sibling", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        patch(resource + "/inactivate").statusCode(200);
        delete(resource + "/translators/" + child).statusCode(400);
        patch(resource + "/translators/" + child + "/inactivate").statusCode(200);
        delete(resource + "/translators/" + child).statusCode(204);
        get(resource).statusCode(200).body("lifecycle", equalTo("INACTIVE"));
        get(resource + "/translators/" + child).statusCode(404);
        get(resource + "/translators/" + sibling).statusCode(200).body("title", equalTo("Sibling"));
        assertThat(translatorCount(identifier)).isEqualTo(1);
    }

    @Test
    void shouldRejectUnknownFieldsAndWrongTypesBeforeBindingInCreateAndUpdate() {
        String identifier = createMessage("review.schema", "REVIEW-005");
        for (Map.Entry<String, Object> invalid :
                Map.<String, Object>of(
                                "httpStatus",
                                "400",
                                "code",
                                123,
                                "microserviceIdentifier",
                                "invalid-uuid",
                                "unknown",
                                true)
                        .entrySet()) {
            Map<String, Object> body = message("review.invalid", "REVIEW-006");
            body.put(invalid.getKey(), invalid.getValue());
            post(body).statusCode(400);
            body.put("version", 0);
            put("/api/v1/dynamic-bundles/messages/" + identifier, body).statusCode(400);
        }
        Map<String, Object> nested = translator("pt-BR", "Title", "Detail", "Suggestion");
        nested.put("unknown", true);
        Map<String, Object> inline = message("review.nested", "REVIEW-007");
        inline.put("translators", List.of(nested));
        post(inline).statusCode(400);
        postTranslator(identifier, nested).statusCode(400);
        nested.remove("unknown");
        nested.put("title", 123);
        post(inline).statusCode(400);
        postTranslator(identifier, nested).statusCode(400);
        inline.put("translators", List.of(false));
        post(inline).statusCode(400);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldRejectCreateVersionAndRequireCurrentVersionsOnUpdates() {
        Map<String, Object> body = message("review.versions", "REVIEW-008");
        body.put("version", 0);
        post(body).statusCode(400).body("details.field", hasItem("version"));
        body.remove("version");
        String identifier = post(body).statusCode(201).extract().path("identifier");
        String path = "/api/v1/dynamic-bundles/messages/" + identifier;
        put(path, body).statusCode(400).body("details.field", hasItem("version"));
        body.put("version", 0);
        put(path, body).statusCode(200).body("version", equalTo(1));
        put(path, body).statusCode(409);
        body.put("version", 1);
        body.put("translators", List.of(translator("pt-BR", "Title", "Detail", "Suggestion")));
        put(path, body).statusCode(400);
        Map<String, Object> translated = translator("pt-BR", "Title", "Detail", "Suggestion");
        translated.put("version", 0);
        postTranslator(identifier, translated)
                .statusCode(400)
                .body("details.field", hasItem("version"));
        translated.remove("version");
        String translatorIdentifier =
                postTranslator(identifier, translated).statusCode(201).extract().path("identifier");
        String child = path + "/translators/" + translatorIdentifier;
        put(child, translated).statusCode(400);
        translated.put("version", 0);
        put(child, translated).statusCode(200).body("version", equalTo(1));
        put(child, translated).statusCode(409);
        translated.put("version", 1);
        translated.put("unknown", true);
        put(child, translated).statusCode(400);
        translated.remove("unknown");
        translated.put("title", 123);
        put(child, translated).statusCode(400);
    }

    @Test
    void shouldSupportLocaleScriptsAndRejectInvalidSuffix() {
        String identifier = createMessage("review.locales", "REVIEW-009");
        postTranslator(identifier, translator(" zh_hant_tw ", "Title", "Detail", "Suggestion"))
                .statusCode(201)
                .body("locale", equalTo("zh-Hant-TW"));
        postTranslator(identifier, translator("pt-BR-!", "Title", "Detail", "Suggestion"))
                .statusCode(400)
                .body("details.field", hasItem("locale"));
        get("/api/v1/dynamic-bundles/messages/" + identifier + "/translators?locale=zh-hant-tw")
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].locale", equalTo("zh-Hant-TW"));
        assertThat(translatorCount(identifier)).isEqualTo(1);
    }

    @Test
    void shouldPaginateWithStableOrderingAndRejectUnsupportedSorting() {
        String first = createMessage("review.page.a", "REVIEW-010");
        createMessage("review.page.b", "REVIEW-011");
        createMessage("review.page.c", "REVIEW-012");
        get("/api/v1/dynamic-bundles/messages?size=1&page=0")
                .statusCode(200)
                .body("content.size()", equalTo(1))
                .body("content[0].identifier", equalTo(first))
                .body("page.totalElements", equalTo(3));
        get("/api/v1/dynamic-bundles/messages?size=1&page=1")
                .statusCode(200)
                .body("content[0].messageKey", equalTo("review.page.b"));
        get("/api/v1/dynamic-bundles/messages?sort=microserviceId,asc").statusCode(400);
        get("/api/v1/dynamic-bundles/messages?sort=translators.locale,asc").statusCode(400);
        postTranslator(first, translator("pt-BR", "Title", "Detail", "Suggestion")).statusCode(201);
        postTranslator(first, translator("en-US", "Title", "Detail", "Suggestion")).statusCode(201);
        get("/api/v1/dynamic-bundles/messages/" + first + "/translators?size=1")
                .statusCode(200)
                .body("content[0].locale", equalTo("en-US"))
                .body("page.totalElements", equalTo(2));
        get("/api/v1/dynamic-bundles/messages/" + first + "/translators?sort=message.id,asc")
                .statusCode(400);
    }

    @Test
    void shouldDenyEveryEndpointWithoutAdministrativeAuthorization() {
        String identifier = createMessage("review.authorization", "REVIEW-013");
        String translatorIdentifier =
                postTranslator(identifier, translator("pt-BR", "Title", "Detail", "Suggestion"))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String path = "/api/v1/dynamic-bundles/messages/" + identifier;
        String child = path + "/translators/" + translatorIdentifier;
        authorizationMock.reset();
        authorizationMock.forbidden();
        get("/api/v1/dynamic-bundles/messages").statusCode(403);
        get(path).statusCode(403);
        get(path + "/translators").statusCode(403);
        get(child).statusCode(403);
        post(message("review.forbidden", "REVIEW-014")).statusCode(403);
        postTranslator(identifier, translator("en-US", "Title", "Detail", "Suggestion"))
                .statusCode(403);
        Map<String, Object> body = message("review.authorization", "REVIEW-013");
        body.put("version", 0);
        put(path, body).statusCode(403);
        Map<String, Object> translated = translator("pt-BR", "Title", "Detail", "Suggestion");
        translated.put("version", 0);
        put(child, translated).statusCode(403);
        for (String resource : List.of(path, child)) {
            patch(resource + "/activate").statusCode(403);
            patch(resource + "/inactivate").statusCode(403);
            delete(resource).statusCode(403);
        }
        authorizationMock.verifyCalledWithPolicy("ADM");
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT version FROM messages WHERE identifier=?",
                                Integer.class,
                                identifier))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "SELECT version FROM message_translators WHERE identifier=?",
                                Integer.class,
                                translatorIdentifier))
                .isZero();
    }

    @Test
    void shouldAcceptAdministrativeSessionFromAuthorizationServiceAndRejectMissingToken() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("USER"));
        post(message("review.authorized", "REVIEW-015")).statusCode(201);
        authorizationMock.verifyCalledWithPolicy("ADM");
        given().port(port)
                .header("correlation-id", "message-no-token")
                .get("/api/v1/dynamic-bundles/messages")
                .then()
                .statusCode(401);
    }

    private String createMessage(String key, String code) {
        return post(message(key, code))
                .statusCode(201)
                .body("microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
                .body("lifecycle", equalTo("ACTIVE"))
                .extract()
                .path("identifier");
    }

    private Map<String, Object> message(String key, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("microserviceIdentifier", " " + MICROSERVICE_IDENTIFIER + " ");
        body.put("messageKey", key);
        body.put("code", code);
        body.put("httpStatus", 400);
        body.put("observation", "Mensagem administrada em runtime");
        return body;
    }

    private Map<String, Object> translator(
            String locale, String title, String detail, String suggestion) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", locale);
        body.put("title", title);
        body.put("detail", detail);
        body.put("suggestion", suggestion);
        return body;
    }

    private int translatorCount(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                  from message_translators mt
                  join messages m on m.id = mt.message_id
                 where m.identifier = ?
                """,
                Integer.class,
                messageIdentifier);
    }

    private int viewCount(String key, String locale) {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                  from vw_dynamic_bundle_messages
                 where message_key = ?
                   and locale = ?
                """,
                Integer.class,
                key,
                locale);
    }

    private ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/dynamic-bundles/messages")
                .then();
    }

    private ValidatableResponse postTranslator(String identifier, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/dynamic-bundles/messages/" + identifier + "/translators")
                .then();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return authorized().contentType(ContentType.JSON).body(body).when().put(path).then();
    }

    private ValidatableResponse patch(String path) {
        return authorized().when().patch(path).then();
    }

    private ValidatableResponse delete(String path) {
        return authorized().when().delete(path).then();
    }

    private void seedPlatformMicroservices() {
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO service_context_microservices
                    (identifier, code, name, description, settings, lifecycle_code, created_at, updated_at)
                VALUES
                    (?, 'workspace-service', 'Workspace Service',
                     'Workspace and platform administration service', '{}',
                     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """,
                MICROSERVICE_IDENTIFIER);
    }

    private void seedLifecycleTypes() {
        jdbcTemplate.update(
                """
                INSERT IGNORE INTO type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                VALUES
                    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                    ('QUARANTINED', 'Quarantined',
                     'Quarantined lifecycle state', 3, true, '{}')
                """);
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given().port(port)
                .header("correlation-id", "message-api-it")
                .header("Authorization", "Bearer message-api-it")
                .accept(ContentType.JSON);
    }
}
