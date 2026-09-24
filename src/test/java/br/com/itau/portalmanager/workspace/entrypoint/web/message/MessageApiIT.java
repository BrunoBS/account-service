package br.com.itau.portalmanager.workspace.entrypoint.web.message;

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
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class MessageApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
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
                .body("locale", equalTo("pt_BR"))
                .body("lifecycle", equalTo("ACTIVE"))
                .extract()
                .path("identifier");

        assertThat(viewCount("workspace-service", "workspace.not-found", "pt_BR"))
                .isEqualTo(1);

        patch("/api/v1/messages/" + messageIdentifier + "/inactivate")
                .statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"));

        assertThat(viewCount("workspace-service", "workspace.not-found", "pt_BR"))
                .isZero();

        patch("/api/v1/messages/" + messageIdentifier + "/activate")
                .statusCode(200);

        patch("/api/v1/messages/" + messageIdentifier
                + "/translations/" + translationIdentifier + "/inactivate")
                .statusCode(200);

        assertThat(viewCount("workspace-service", "workspace.not-found", "pt_BR"))
                .isZero();
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
                translation("pt_BR", "E-mail inválido", "Detalhe um", "Sugestão um")
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
    void shouldDeleteMessageOnlyWhenInactive() {
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
                .statusCode(404)
                .body("code", equalTo("MESSAGE-0001"));
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
                .body("service", equalTo("workspace-service"))
                .body("lifecycle", equalTo("ACTIVE"))
                .extract()
                .path("identifier");
    }

    private Map<String, Object> message(String key, String code) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", " WORKSPACE-SERVICE ");
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

    private int viewCount(String service, String key, String locale) {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                  from vw_platform_messages
                 where service = ?
                   and message_key = ?
                   and locale = ?
                """,
                Integer.class,
                service,
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

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "message-api-it")
                .header("Authorization", "Bearer message-api-it")
                .accept(ContentType.JSON);
    }
}
