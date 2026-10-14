package br.com.portalmanager.platform.workspace.entrypoint.web.message;

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
import java.util.LinkedHashMap;
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
class MessageApiIT {

    private static final String MICROSERVICE_IDENTIFIER = "11111111-1111-1111-1111-111111111111";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
        SchemaDefaultFixture.seed(jdbcTemplate);
        seedLifecycleTypes();
        seedPlatformMicroservices();
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldCreateMessageWithTranslationsInSingleRequest() {
        Map<String, Object> body = message("workspace.batch.not-found", "WORKSPACE-0201");
        body.put(
            "translations",
            List.of(
                translation(
                    "pt-br",
                    "Workspace não encontrado",
                    "Mensagem customizada em português.",
                    "Revise o identificador informado."
                ),
                translation(
                    "en-us",
                    "Workspace not found",
                    "Customized message in English.",
                    "Review the provided identifier."
                )
            )
        );

        String identifier = post(body)
            .statusCode(201)
            .body("microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
            .extract()
            .path("identifier");

        Long messageId = jdbcTemplate.queryForObject(
            "select id from messages where identifier = ?",
            Long.class,
            identifier
        );

        Integer translationCount = jdbcTemplate.queryForObject(
            "select count(*) from message_translations where message_id = ?",
            Integer.class,
            messageId
        );

        Long microserviceId = jdbcTemplate.queryForObject(
            "select id from platform_microservices where identifier = ?",
            Long.class,
            MICROSERVICE_IDENTIFIER
        );
        Long persistedMicroserviceId = jdbcTemplate.queryForObject(
            "select microservice_id from messages where identifier = ?",
            Long.class,
            identifier
        );

        assertThat(persistedMicroserviceId).isEqualTo(microserviceId);
        assertThat(translationCount).isEqualTo(2);
        assertThat(viewCount("workspace-service.workspace.batch.not-found", "pt-BR")).isEqualTo(1);
        assertThat(viewCount("workspace-service.workspace.batch.not-found", "en-US")).isEqualTo(1);
    }

    @Test
    void shouldIgnoreNullOrEmptyTranslationsWhenCreatingMessage() {
        Map<String, Object> withNull = message("workspace.null-translations", "WORKSPACE-0202");
        withNull.put("translations", null);

        String nullIdentifier = post(withNull).statusCode(201).extract().path("identifier");

        Map<String, Object> withEmpty = message("workspace.empty-translations", "WORKSPACE-0203");
        withEmpty.put("translations", List.of());

        String emptyIdentifier = post(withEmpty).statusCode(201).extract().path("identifier");

        assertThat(translationCount(nullIdentifier)).isZero();
        assertThat(translationCount(emptyIdentifier)).isZero();
    }

    @Test
    void shouldRollbackMessageWhenInlineTranslationsContainDuplicateLocale() {
        Map<String, Object> body = message("workspace.duplicate-inline-locale", "WORKSPACE-0204");
        body.put(
            "translations",
            List.of(
                translation("pt-br", "Título um", "Detalhe um", "Sugestão um"),
                translation("pt-BR", "Título dois", "Detalhe dois", "Sugestão dois")
            )
        );

        post(body).statusCode(400).body("code", equalTo("GLOBAL-0001")).body("details.field", hasItem("locale"));

        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from messages where message_key = ?",
            Integer.class,
            "workspace.duplicate-inline-locale"
        );
        assertThat(count).isZero();
    }

    @Test
    void shouldCreateTranslationAndExposeOnlyActiveEntriesInView() {
        String messageIdentifier = createMessage("workspace.not-found", "WORKSPACE-0001");

        String translationIdentifier = postTranslation(
            messageIdentifier,
            translation(
                "pt-br",
                "Workspace não encontrado",
                "Mensagem customizada pelo catálogo.",
                "Revise o identificador informado."
            )
        )
            .statusCode(201)
            .body("locale", equalTo("pt-BR"))
            .body("lifecycle", equalTo("ACTIVE"))
            .extract()
            .path("identifier");

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR")).isEqualTo(1);

        patch("/api/v1/messages/" + messageIdentifier + "/inactivate")
            .statusCode(200)
            .body("lifecycle", equalTo("INACTIVE"));

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR")).isZero();

        patch("/api/v1/messages/" + messageIdentifier + "/activate").statusCode(200);

        patch(
            "/api/v1/messages/" + messageIdentifier + "/translations/" + translationIdentifier + "/inactivate"
        ).statusCode(200);

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR")).isZero();
    }

    @Test
    void shouldFindTranslationByIdentifierAndFilterTranslations() {
        String messageIdentifier = createMessage("workspace.translation-query", "WORKSPACE-0205");

        String ptIdentifier = postTranslation(
            messageIdentifier,
            translation("pt-br", "Título PT", "Detalhe PT", "Sugestão PT")
        )
            .statusCode(201)
            .extract()
            .path("identifier");

        String enIdentifier = postTranslation(
            messageIdentifier,
            translation("en-us", "Title EN", "Detail EN", "Suggestion EN")
        )
            .statusCode(201)
            .extract()
            .path("identifier");

        get("/api/v1/messages/" + messageIdentifier + "/translations/" + ptIdentifier)
            .statusCode(200)
            .body("identifier", equalTo(ptIdentifier))
            .body("locale", equalTo("pt-BR"));

        patch("/api/v1/messages/" + messageIdentifier + "/translations/" + enIdentifier + "/inactivate").statusCode(
            200
        );

        get("/api/v1/messages/" + messageIdentifier + "/translations?locale=pt-br")
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].locale", equalTo("pt-BR"));

        get("/api/v1/messages/" + messageIdentifier + "/translations?lifecycle=INACTIVE")
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].locale", equalTo("en-US"));
    }

    @Test
    void shouldFilterMessagesByServiceLifecycleCodeAndMessageKey() {
        String activeIdentifier = createMessage("workspace.query.active", "WORKSPACE-0206");
        String inactiveIdentifier = createMessage("workspace.query.inactive", "WORKSPACE-0207");

        patch("/api/v1/messages/" + inactiveIdentifier + "/inactivate").statusCode(200);

        get("/api/v1/messages?microserviceIdentifier=" + MICROSERVICE_IDENTIFIER + "&lifecycle=ACTIVE&code=workspace-0206")
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].identifier", equalTo(activeIdentifier));

        get("/api/v1/messages?messageKey=workspace.query.inactive&lifecycle=INACTIVE")
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].identifier", equalTo(inactiveIdentifier));
    }

    @Test
    void shouldKeepAdministrativeReadAvailableWhenServiceBecomesInactive() {
        String identifier = createMessage("workspace.service.inactive", "WORKSPACE-0208");
        postTranslation(
            identifier,
            translation(
                "pt-BR",
                "Service inactive",
                "Message owned by a service that becomes inactive.",
                "Reactivate the service to expose the runtime message."
            )
        ).statusCode(201);

        assertThat(viewCount("workspace-service.workspace.service.inactive", "pt-BR")).isEqualTo(1);

        Integer version = get("/api/v1/messages/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        jdbcTemplate.update(
            "update platform_microservices set lifecycle_code = 'INACTIVE' where identifier = ?",
            MICROSERVICE_IDENTIFIER
        );

        get("/api/v1/messages?microserviceIdentifier=" + MICROSERVICE_IDENTIFIER)
            .statusCode(200)
            .body("content.size()", equalTo(1))
            .body("content[0].identifier", equalTo(identifier))
            .body("content[0].microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
            .body("content[0]", not(hasKey("microserviceId")));

        get("/api/v1/messages/" + identifier)
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

        put("/api/v1/messages/" + identifier, update)
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
    void shouldRejectDuplicateTranslationLocaleAfterNormalization() {
        String messageIdentifier = createMessage("validation.email.invalid", "WORKSPACE-0109");

        postTranslation(
            messageIdentifier,
            translation("pt-BR", "E-mail inválido", "Detalhe um", "Sugestão um")
        ).statusCode(201);

        postTranslation(messageIdentifier, translation("pt-br", "E-mail inválido", "Detalhe dois", "Sugestão dois"))
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("locale"));
    }

    @Test
    void shouldPhysicallyDeleteMessageOnlyWhenInactive() {
        String identifier = createMessage("workspace.delete.invalid", "WORKSPACE-0003");

        delete("/api/v1/messages/" + identifier)
            .statusCode(400)
            .body("code", equalTo("MESSAGE-0002"));

        patch("/api/v1/messages/" + identifier + "/inactivate").statusCode(200);

        delete("/api/v1/messages/" + identifier).statusCode(204);

        get("/api/v1/messages/" + identifier).statusCode(404);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages WHERE identifier=?", Integer.class, identifier)).isZero();
    }

    @Test
    void shouldUpdateUsingOptimisticVersion() {
        String identifier = createMessage("workspace.restore.invalid", "WORKSPACE-0002");

        Integer version = get("/api/v1/messages/" + identifier)
            .statusCode(200)
            .body("identifier", notNullValue())
            .extract()
            .path("version");

        Map<String, Object> update = message("workspace.restore.invalid", "WORKSPACE-0002");
        update.put("version", version);
        update.put("observation", "Mensagem administrativa atualizada");

        put("/api/v1/messages/" + identifier, update)
            .statusCode(200)
            .body("observation", equalTo("Mensagem administrativa atualizada"));
    }

    @Test
    void shouldDeleteInactiveMessageWithActiveAndInactiveTranslationsAndAllowKeyReuse() {
        String identifier = createMessage("review.delete", "REVIEW-001");
        String path = "/api/v1/messages/" + identifier;
        String activeTranslation = postTranslation(identifier, translation("pt-BR", "Title", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        String inactiveTranslation = postTranslation(identifier, translation("en-US", "Title", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        patch(path + "/translations/" + inactiveTranslation + "/inactivate").statusCode(200);
        delete(path).statusCode(400);
        assertThat(translationCount(identifier)).isEqualTo(2);
        patch(path + "/inactivate").statusCode(200);
        delete(path).statusCode(204);
        get(path).statusCode(404);
        patch(path + "/activate").statusCode(404);
        delete(path).statusCode(404);
        Map<String, Object> update = message("review.delete", "REVIEW-001");
        update.put("version", 1);
        put(path, update).statusCode(404);
        postTranslation(identifier, translation("en-US", "Title", "Detail", "Suggestion")).statusCode(404);
        for (String childIdentifier : List.of(activeTranslation, inactiveTranslation)) {
            String child = path + "/translations/" + childIdentifier;
            get(child).statusCode(404);
            patch(child + "/inactivate").statusCode(404);
            patch(child + "/activate").statusCode(404);
            delete(child).statusCode(404);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages WHERE identifier=?", Integer.class, identifier)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM message_translations", Integer.class)).isZero();
        createMessage("review.delete", "REVIEW-001");
    }

    @Test
    void shouldPreserveTranslationsWhenParentIsInactiveButRejectNewLinksAndActivation() {
        String identifier = createMessage("review.parent.inactive", "REVIEW-002");
        String translationIdentifier = postTranslation(identifier, translation("pt-BR", "Title", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        String child = "/api/v1/messages/" + identifier + "/translations/" + translationIdentifier;
        patch("/api/v1/messages/" + identifier + "/inactivate").statusCode(200);
        get(child).statusCode(200).body("lifecycle", equalTo("ACTIVE"));
        postTranslation(identifier, translation("en-US", "Title", "Detail", "Suggestion")).statusCode(400);
        Map<String, Object> update = translation("pt-BR", "Edited", "Detail", "Suggestion");
        update.put("version", 0);
        put(child, update).statusCode(200).body("title", equalTo("Edited")).body("version", equalTo(1));
        patch(child + "/inactivate").statusCode(200);
        patch(child + "/activate").statusCode(400);
        patch("/api/v1/messages/" + identifier + "/activate").statusCode(200);
        patch(child + "/activate").statusCode(200);
    }

    @Test
    void shouldPhysicallyDeleteInactiveTranslationAndRejectForeignIdentifiers() {
        String identifier = createMessage("review.child.delete", "REVIEW-003");
        String other = createMessage("review.other.message", "REVIEW-004");
        String translationIdentifier = postTranslation(identifier, translation("pt-BR", "Title", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        String child = "/api/v1/messages/" + identifier + "/translations/" + translationIdentifier;
        get("/api/v1/messages/" + other + "/translations/" + translationIdentifier).statusCode(404);
        patch("/api/v1/messages/" + other + "/translations/" + translationIdentifier + "/inactivate").statusCode(404);
        delete("/api/v1/messages/" + other + "/translations/" + translationIdentifier).statusCode(404);
        delete(child).statusCode(400);
        patch(child + "/inactivate").statusCode(200);
        delete(child).statusCode(204);
        patch(child + "/activate").statusCode(404);
        patch(child + "/inactivate").statusCode(404);
        Map<String, Object> update = translation("pt-BR", "Changed", "Detail", "Suggestion");
        update.put("version", 2);
        put(child, update).statusCode(404);
        get(child).statusCode(404);
        delete(child).statusCode(404);
        get("/api/v1/messages/" + identifier).statusCode(200).body("lifecycle", equalTo("ACTIVE"));
        assertThat(translationCount(identifier)).isZero();
        postTranslation(identifier, translation("pt-BR", "New title", "Detail", "Suggestion")).statusCode(201);
    }

    @Test
    void shouldDeleteInactiveTranslationUnderInactiveMessageWithoutDeletingSiblings() {
        String identifier = createMessage("review.isolated.delete", "REVIEW-010");
        String resource = "/api/v1/messages/" + identifier;
        String child = postTranslation(identifier, translation("pt-BR", "Title", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        String sibling = postTranslation(identifier, translation("en-US", "Sibling", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        patch(resource + "/inactivate").statusCode(200);
        delete(resource + "/translations/" + child).statusCode(400);
        patch(resource + "/translations/" + child + "/inactivate").statusCode(200);
        delete(resource + "/translations/" + child).statusCode(204);
        get(resource).statusCode(200).body("lifecycle", equalTo("INACTIVE"));
        get(resource + "/translations/" + child).statusCode(404);
        get(resource + "/translations/" + sibling).statusCode(200).body("title", equalTo("Sibling"));
        assertThat(translationCount(identifier)).isEqualTo(1);
    }

    @Test
    void shouldRejectUnknownFieldsAndWrongTypesBeforeBindingInCreateAndUpdate() {
        String identifier = createMessage("review.schema", "REVIEW-005");
        for (Map.Entry<String, Object> invalid : Map.<String, Object>of(
            "httpStatus", "400", "code", 123, "microserviceIdentifier", "invalid-uuid", "unknown", true).entrySet()) {
            Map<String, Object> body = message("review.invalid", "REVIEW-006");
            body.put(invalid.getKey(), invalid.getValue());
            post(body).statusCode(400);
            body.put("version", 0);
            put("/api/v1/messages/" + identifier, body).statusCode(400);
        }
        Map<String, Object> nested = translation("pt-BR", "Title", "Detail", "Suggestion");
        nested.put("unknown", true);
        Map<String, Object> inline = message("review.nested", "REVIEW-007");
        inline.put("translations", List.of(nested));
        post(inline).statusCode(400);
        postTranslation(identifier, nested).statusCode(400);
        nested.remove("unknown");
        nested.put("title", 123);
        post(inline).statusCode(400);
        postTranslation(identifier, nested).statusCode(400);
        inline.put("translations", List.of(false));
        post(inline).statusCode(400);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM messages", Integer.class)).isEqualTo(1);
    }

    @Test
    void shouldRejectCreateVersionAndRequireCurrentVersionsOnUpdates() {
        Map<String, Object> body = message("review.versions", "REVIEW-008");
        body.put("version", 0);
        post(body).statusCode(400).body("details.field", hasItem("version"));
        body.remove("version");
        String identifier = post(body).statusCode(201).extract().path("identifier");
        String path = "/api/v1/messages/" + identifier;
        put(path, body).statusCode(400).body("details.field", hasItem("version"));
        body.put("version", 0);
        put(path, body).statusCode(200).body("version", equalTo(1));
        put(path, body).statusCode(409);
        body.put("version", 1);
        body.put("translations", List.of(translation("pt-BR", "Title", "Detail", "Suggestion")));
        put(path, body).statusCode(400);
        Map<String, Object> translated = translation("pt-BR", "Title", "Detail", "Suggestion");
        translated.put("version", 0);
        postTranslation(identifier, translated).statusCode(400).body("details.field", hasItem("version"));
        translated.remove("version");
        String translationIdentifier = postTranslation(identifier, translated).statusCode(201).extract().path("identifier");
        String child = path + "/translations/" + translationIdentifier;
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
        postTranslation(identifier, translation(" zh_hant_tw ", "Title", "Detail", "Suggestion"))
            .statusCode(201).body("locale", equalTo("zh-Hant-TW"));
        postTranslation(identifier, translation("pt-BR-!", "Title", "Detail", "Suggestion"))
            .statusCode(400).body("details.field", hasItem("locale"));
        get("/api/v1/messages/" + identifier + "/translations?locale=zh-hant-tw")
            .statusCode(200).body("content.size()", equalTo(1)).body("content[0].locale", equalTo("zh-Hant-TW"));
        assertThat(translationCount(identifier)).isEqualTo(1);
    }

    @Test
    void shouldPaginateWithStableOrderingAndRejectUnsupportedSorting() {
        String first = createMessage("review.page.a", "REVIEW-010");
        createMessage("review.page.b", "REVIEW-011");
        createMessage("review.page.c", "REVIEW-012");
        get("/api/v1/messages?size=1&page=0").statusCode(200)
            .body("content.size()", equalTo(1)).body("content[0].identifier", equalTo(first))
            .body("page.totalElements", equalTo(3));
        get("/api/v1/messages?size=1&page=1").statusCode(200).body("content[0].messageKey", equalTo("review.page.b"));
        get("/api/v1/messages?sort=microserviceId,asc").statusCode(400);
        get("/api/v1/messages?sort=translations.locale,asc").statusCode(400);
        postTranslation(first, translation("pt-BR", "Title", "Detail", "Suggestion")).statusCode(201);
        postTranslation(first, translation("en-US", "Title", "Detail", "Suggestion")).statusCode(201);
        get("/api/v1/messages/" + first + "/translations?size=1").statusCode(200)
            .body("content[0].locale", equalTo("en-US")).body("page.totalElements", equalTo(2));
        get("/api/v1/messages/" + first + "/translations?sort=message.id,asc").statusCode(400);
    }

    @Test
    void shouldDenyEveryEndpointWithoutAdministrativeAuthorization() {
        String identifier = createMessage("review.authorization", "REVIEW-013");
        String translationIdentifier = postTranslation(identifier, translation("pt-BR", "Title", "Detail", "Suggestion"))
            .statusCode(201).extract().path("identifier");
        String path = "/api/v1/messages/" + identifier;
        String child = path + "/translations/" + translationIdentifier;
        authorizationMock.reset();
        authorizationMock.forbidden();
        get("/api/v1/messages").statusCode(403);
        get(path).statusCode(403);
        get(path + "/translations").statusCode(403);
        get(child).statusCode(403);
        post(message("review.forbidden", "REVIEW-014")).statusCode(403);
        postTranslation(identifier, translation("en-US", "Title", "Detail", "Suggestion")).statusCode(403);
        Map<String, Object> body = message("review.authorization", "REVIEW-013");
        body.put("version", 0);
        put(path, body).statusCode(403);
        Map<String, Object> translated = translation("pt-BR", "Title", "Detail", "Suggestion");
        translated.put("version", 0);
        put(child, translated).statusCode(403);
        for (String resource : List.of(path, child)) {
            patch(resource + "/activate").statusCode(403);
            patch(resource + "/inactivate").statusCode(403);
            delete(resource).statusCode(403);
        }
        authorizationMock.verifyCalledWithPolicy("ADM");
        assertThat(jdbcTemplate.queryForObject("SELECT version FROM messages WHERE identifier=?", Integer.class, identifier)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT version FROM message_translations WHERE identifier=?", Integer.class, translationIdentifier)).isZero();
    }

    @Test
    void shouldAcceptAdministrativeSessionFromAuthorizationServiceAndRejectMissingToken() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("USER"));
        post(message("review.authorized", "REVIEW-015")).statusCode(201);
        authorizationMock.verifyCalledWithPolicy("ADM");
        given().port(port).header("correlation-id", "message-no-token").get("/api/v1/messages")
            .then().statusCode(401);
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

    private Map<String, Object> translation(String locale, String title, String detail, String suggestion) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("locale", locale);
        body.put("title", title);
        body.put("detail", detail);
        body.put("suggestion", suggestion);
        return body;
    }

    private int translationCount(String messageIdentifier) {
        return jdbcTemplate.queryForObject(
            """
            select count(*)
              from message_translations mt
              join messages m on m.id = mt.message_id
             where m.identifier = ?
            """,
            Integer.class,
            messageIdentifier
        );
    }

    private int viewCount(String key, String locale) {
        return jdbcTemplate.queryForObject(
            """
            select count(*)
              from vw_platform_messages
             where message_key = ?
               and locale = ?
            """,
            Integer.class,
            key,
            locale
        );
    }

    private ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return authorized().contentType(ContentType.JSON).body(body).when().post("/api/v1/messages").then();
    }

    private ValidatableResponse postTranslation(String identifier, Map<String, Object> body) {
        return authorized()
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/v1/messages/" + identifier + "/translations")
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
            INSERT IGNORE INTO platform_microservices
                (identifier, code, name, description, settings, lifecycle_code, created_at, updated_at)
            VALUES
                (?, 'workspace-service', 'Workspace Service',
                 'Workspace and platform administration service', '{}',
                 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            MICROSERVICE_IDENTIFIER
        );
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
            """
        );
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
            .port(port)
            .header("correlation-id", "message-api-it")
            .header("Authorization", "Bearer message-api-it")
            .accept(ContentType.JSON);
    }
}
