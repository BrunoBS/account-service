package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
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

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

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
        seedLifecycleTypes();
        seedPlatformMicroservices();
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldCreateMessageWithTranslationsInSingleRequest() {
        Map<String, Object> body = message(
                "workspace.batch.not-found",
                "WORKSPACE-0201"
        );
        body.put("translations", List.of(
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
        ));

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
        assertThat(viewCount("workspace-service.workspace.batch.not-found", "pt-BR"))
                .isEqualTo(1);
        assertThat(viewCount("workspace-service.workspace.batch.not-found", "en-US"))
                .isEqualTo(1);
    }

    @Test
    void shouldIgnoreNullOrEmptyTranslationsWhenCreatingMessage() {
        Map<String, Object> withNull = message(
                "workspace.null-translations",
                "WORKSPACE-0202"
        );
        withNull.put("translations", null);

        String nullIdentifier = post(withNull)
                .statusCode(201)
                .extract()
                .path("identifier");

        Map<String, Object> withEmpty = message(
                "workspace.empty-translations",
                "WORKSPACE-0203"
        );
        withEmpty.put("translations", List.of());

        String emptyIdentifier = post(withEmpty)
                .statusCode(201)
                .extract()
                .path("identifier");

        assertThat(translationCount(nullIdentifier)).isZero();
        assertThat(translationCount(emptyIdentifier)).isZero();
    }

    @Test
    void shouldRollbackMessageWhenInlineTranslationsContainDuplicateLocale() {
        Map<String, Object> body = message(
                "workspace.duplicate-inline-locale",
                "WORKSPACE-0204"
        );
        body.put("translations", List.of(
                translation("pt-br", "Título um", "Detalhe um", "Sugestão um"),
                translation("pt-BR", "Título dois", "Detalhe dois", "Sugestão dois")
        ));

        post(body)
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("locale"));

        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from messages where message_key = ?",
                Integer.class,
                "workspace.duplicate-inline-locale"
        );
        assertThat(count).isZero();
    }

    @Test
    void shouldCreateTranslationAndExposeOnlyActiveEntriesInView() {
        String messageIdentifier = createMessage(
                "workspace.not-found",
                "WORKSPACE-0001"
        );

        String translationIdentifier = postTranslation(
                messageIdentifier,
                translation("pt-br", "Workspace não encontrado",
                        "Mensagem customizada pelo catálogo.",
                        "Revise o identificador informado.")
        )
                .statusCode(201)
                .body("locale", equalTo("pt-BR"))
                .body("lifecycle", equalTo("ACTIVE"))
                .extract()
                .path("identifier");

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR"))
                .isEqualTo(1);

        patch("/api/v1/messages/" + messageIdentifier + "/inactivate")
                .statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"));

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR"))
                .isZero();

        patch("/api/v1/messages/" + messageIdentifier + "/activate")
                .statusCode(200);

        patch("/api/v1/messages/" + messageIdentifier
                + "/translations/" + translationIdentifier + "/inactivate")
                .statusCode(200);

        assertThat(viewCount("workspace-service.workspace.not-found", "pt-BR"))
                .isZero();
    }

    @Test
    void shouldFindTranslationByIdentifierAndFilterTranslations() {
        String messageIdentifier = createMessage(
                "workspace.translation-query",
                "WORKSPACE-0205"
        );

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

        patch("/api/v1/messages/" + messageIdentifier
                + "/translations/" + enIdentifier + "/inactivate")
                .statusCode(200);

        get("/api/v1/messages/" + messageIdentifier + "/translations?locale=pt-br")
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].locale", equalTo("pt-BR"));

        get("/api/v1/messages/" + messageIdentifier + "/translations?active=false")
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].locale", equalTo("en-US"));
    }

    @Test
    void shouldFilterMessagesByServiceLifecycleCodeAndMessageKey() {
        String activeIdentifier = createMessage(
                "workspace.query.active",
                "WORKSPACE-0206"
        );
        String inactiveIdentifier = createMessage(
                "workspace.query.inactive",
                "WORKSPACE-0207"
        );

        patch("/api/v1/messages/" + inactiveIdentifier + "/inactivate")
                .statusCode(200);

        get("/api/v1/messages?microserviceIdentifier=" + MICROSERVICE_IDENTIFIER + "&active=true&code=workspace-0206")
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].identifier", equalTo(activeIdentifier));

        get("/api/v1/messages?messageKey=workspace.query.inactive&active=false")
                .statusCode(200)
                .body("size()", equalTo(1))
                .body("[0].identifier", equalTo(inactiveIdentifier));
    }

    @Test
    void shouldKeepAdministrativeReadAvailableWhenServiceBecomesInactive() {
        String identifier = createMessage(
                "workspace.service.inactive",
                "WORKSPACE-0208"
        );
        postTranslation(
                identifier,
                translation(
                        "pt-BR",
                        "Service inactive",
                        "Message owned by a service that becomes inactive.",
                        "Reactivate the service to expose the runtime message."
                )
        ).statusCode(201);

        assertThat(viewCount("workspace-service.workspace.service.inactive", "pt-BR"))
                .isEqualTo(1);

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
                .body("size()", equalTo(1))
                .body("[0].identifier", equalTo(identifier))
                .body("[0].microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
                .body("[0]", not(hasKey("microserviceId")));

        get("/api/v1/messages/" + identifier)
                .statusCode(200)
                .body("microserviceIdentifier", equalTo(MICROSERVICE_IDENTIFIER))
                .body("$", not(hasKey("microserviceId")));

        assertThat(viewCount("workspace-service.workspace.service.inactive", "pt-BR"))
                .isZero();

        post(message("workspace.service.inactive.new", "WORKSPACE-0209"))
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("microserviceIdentifier"));

        Map<String, Object> update = message(
                "workspace.service.inactive",
                "WORKSPACE-0208"
        );
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
        String messageIdentifier = createMessage(
                "validation.email.invalid",
                "WORKSPACE-0109"
        );

        postTranslation(
                messageIdentifier,
                translation("pt-BR", "E-mail inválido", "Detalhe um", "Sugestão um")
        ).statusCode(201);

        postTranslation(
                messageIdentifier,
                translation("pt-br", "E-mail inválido", "Detalhe dois", "Sugestão dois")
        )
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("locale"));
    }

    @Test
    void shouldQuarantineMessageOnlyWhenInactive() {
        String identifier = createMessage(
                "workspace.delete.invalid",
                "WORKSPACE-0003"
        );

        delete("/api/v1/messages/" + identifier)
                .statusCode(400)
                .body("code", equalTo("MESSAGE-0002"));

        patch("/api/v1/messages/" + identifier + "/inactivate")
                .statusCode(200);

        delete("/api/v1/messages/" + identifier)
                .statusCode(204);

        get("/api/v1/messages/" + identifier)
                .statusCode(200)
                .body("lifecycle", equalTo("QUARANTINED"));
    }

    @Test
    void shouldUpdateUsingOptimisticVersion() {
        String identifier = createMessage(
                "workspace.restore.invalid",
                "WORKSPACE-0002"
        );

        Integer version = get("/api/v1/messages/" + identifier)
                .statusCode(200)
                .body("identifier", notNullValue())
                .extract()
                .path("version");

        Map<String, Object> update = message(
                "workspace.restore.invalid",
                "WORKSPACE-0002"
        );
        update.put("version", version);
        update.put("observation", "Mensagem administrativa atualizada");

        put("/api/v1/messages/" + identifier, update)
                .statusCode(200)
                .body("observation", equalTo("Mensagem administrativa atualizada"));
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

    private Map<String, Object> translation(
            String locale,
            String title,
            String detail,
            String suggestion
    ) {
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
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/messages")
                .then();
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
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(path)
                .then();
    }

    private ValidatableResponse patch(String path) {
        return authorized().when().patch(path).then();
    }

    private ValidatableResponse delete(String path) {
        return authorized().when().delete(path).then();
    }

    private void seedPlatformMicroservices() {
        jdbcTemplate.update("""
                INSERT IGNORE INTO platform_microservices
                    (identifier, code, name, description, lifecycle_code, created_at, updated_at)
                VALUES
                    (?, 'workspace-service', 'Workspace Service',
                     'Workspace and platform administration service',
                     'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, MICROSERVICE_IDENTIFIER);
    }

    private void seedLifecycleTypes() {
        jdbcTemplate.update("""
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
        return given()
                .port(port)
                .header("X-Correlation-Id", "message-api-it")
                .header("Authorization", "Bearer message-api-it")
                .accept(ContentType.JSON);
    }
}
