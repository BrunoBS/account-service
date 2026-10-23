Warning: truncated output (original token count: 25500)
Total output lines: 2039

package br.com.portalmanager.platform.workspace.entrypoint.sharing;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

class ResourceSharingApiIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{

    @LocalServerPort private int port;

    @Autowired private EntityManagerFactory entityManagerFactory;

    @Autowired private JdbcTemplate jdbc;

    @Autowired private AuthorizationMock authorization;

    private String sharedFeatureIdentifier;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        seedCatalogs();
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
        sharedFeatureIdentifier = createPlatformFeature(true);
    }

    @Test
    void sharedListsPaginateAndValidateBeforeLoadingTheirResults() {
        Scope owner = createScope("PagedOwner");
        Scope participantScope = createScope("PagedParticipant");
        String firstContract = createContract(owner);
        String secondContract = createContract(owner);
        String firstParticipant =
                requestParticipant(participantScope, firstContract)
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String secondParticipant =
                requestParticipant(participantScope, secondContract)
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        Scope anotherParticipantScope = createScope("AnotherPagedParticipant");
        String anotherParticipant =
                requestParticipant(anotherParticipantScope, firstContract)
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        authorized()
                .queryParam("size", 1)
                .get(owner.contractsPath())
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(firstContract)))
                .body("totalElements", equalTo(2))
                .body("totalPages", equalTo(2));
        authorized()
                .queryParam("page", 1)
                .queryParam("size", 1)
                .get(owner.contractsPath())
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(secondContract)));
        authorized()
                .queryParam("size", 1)
                .get(participantScope.basePath() + "/resource-sharing/participants")
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(secondParticipant)))
                .body("totalElements", equalTo(2));
        authorized()
                .queryParam("page", 1)
                .queryParam("size", 1)
                .get(participantScope.basePath() + "/resource-sharing/participants")
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(firstParticipant)));
        String ownerParticipantPath = owner.contractPath(firstContract) + "/participants";
        authorized()
                .queryParam("size", 1)
                .get(ownerParticipantPath)
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(firstParticipant)))
                .body("totalElements", equalTo(2));
        authorized()
                .queryParam("page", 1)
                .queryParam("size", 1)
                .get(ownerParticipantPath)
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(anotherParticipant)));
        authorized()
                .queryParam("participantName", " anotherpagedparticipant ")
                .queryParam("size", 1)
                .get(ownerParticipantPath)
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(anotherParticipant)))
                .body("totalElements", equalTo(1));
        authorized()
                .queryParam("participantName", "%")
                .get(ownerParticipantPath)
                .then()
                .statusCode(200)
                .body("totalElements", equalTo(0));
        authorized()
                .queryParam(
                        "participantApplicationIdentifier", anotherParticipantScope.application())
                .get(ownerParticipantPath)
                .then()
                .statusCode(200)
                .body("content.identifier", equalTo(List.of(anotherParticipant)));
        for (String path :
                List.of(
                        owner.contractsPath(),
                        ownerParticipantPath,
                        participantScope.basePath() + "/resource-sharing/participants")) {
            authorized()
                    .queryParam("page", 9)
                    .queryParam("size", 1)
                    .get(path)
                    .then()
                    .statusCode(200)
                    .body("content.size()", equalTo(0))
                    .body("totalElements", equalTo(2));
            for (String invalidSize : List.of("0", "101", "invalid")) {
                authorized().queryParam("size", invalidSize).get(path).then().statusCode(400);
            }
            authorized().queryParam("page", -1).get(path).then().statusCode(400);
        }
        authorized()
                .queryParam("participantApplicationIdentifier", " ")
                .get(ownerParticipantPath)
                .then()
                .statusCode(400);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean statisticsEnabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            authorized()
                    .queryParam("status", "UNKNOWN")
                    .get(ownerParticipantPath)
                    .then()
                    .statusCode(400);
            assertThat(statistics.getPrepareStatementCount()).isZero();
        } finally {
            statistics.setStatisticsEnabled(statisticsEnabled);
        }
    }

    @Test
    void sharedListQueryCountsDoNotGrowWithTheNumberOfPageItems() {
        Scope owner = createScope("QueryCountOwner");
        Scope participantScope = createScope("QueryCountParticipant");
        String firstContract = null;
        for (int index = 0; index < 4; index++) {
            String contract = createContract(owner);
            if (firstContract == null) firstContract = contract;
            String participant =
                    requestParticipant(participantScope, contract)
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            patch(
                            owner.contractPath(contract)
                                    + "/participants/"
                                    + participant
                                    + "/status",
                            statusChangeRequest("APPROVE", approvalRequest("MANUAL")))
                    .statusCode(200);
        }
        for (int index = 0; index < 3; index++) {
            Scope anotherParticipantScope = createScope("QueryCountAnother" + index);
            String participant =
                    requestParticipant(anotherParticipantScope, firstContract)
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            patch(
                            owner.contractPath(firstContract)
                                    + "/participants/"
                                    + participant
                                    + "/status",
                            statusChangeRequest("APPROVE", approvalRequest("MANUAL")))
                    .statusCode(200);
        }
        for (String path :
      …20500 tokens truncated…st.put("settings", Map.of());
        if (shareable != null) request.put("shareable", shareable);
        return post("/api/v1/service-contexts/features", request)
                .statusCode(201)
                .extract()
                .path("identifier");
    }

    private Map<String, Object> featureUpdate(String identifier, boolean shareable) {
        ExtractableResponse<Response> feature =
                get("/api/v1/service-contexts/features/" + identifier).statusCode(200).extract();
        return Map.of(
                "version",
                feature.path("version"),
                "code",
                feature.path("code"),
                "name",
                feature.path("name"),
                "microserviceIdentifier",
                feature.path("microserviceIdentifier"),
                "settings",
                Map.of("language", "JAVA", "infrastructure", "VM"),
                "shareable",
                shareable);
    }

    private String createContract(Scope owner) {
        Integer existing =
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resource_sharing_contracts contract JOIN applications"
                            + " application ON application.id = contract.owner_application_id WHERE"
                            + " application.identifier = ?",
                        Integer.class,
                        owner.application());
        return createContract(
                owner, existing == 0 ? sharedFeatureIdentifier : createPlatformFeature(true));
    }

    private String createContract(Scope owner, String featureIdentifier) {
        String identifier =
                post(
                                owner.contractsPath(),
                                Map.of(
                                        "description",
                                        "Contrato de teste",
                                        "featureIdentifier",
                                        featureIdentifier))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        assertThat(
                        jdbc.queryForObject(
                                """
                                SELECT COUNT(*) FROM resource_sharing_contracts contract
                                JOIN workspaces workspace ON workspace.id = contract.owner_workspace_id
                                JOIN applications application ON application.id = contract.owner_application_id
                                    AND application.workspace_id = workspace.id
                                WHERE contract.identifier = ? AND workspace.identifier = ? AND application.identifier = ?
                                """,
                                Integer.class,
                                identifier,
                                owner.workspace(),
                                owner.application()))
                .isEqualTo(1);
        return identifier;
    }

    private void insertMapping(String participant, String source, String destination) {
        insertEnvironment(source);
        insertEnvironment(destination);
        jdbc.update(
                """
                insert into resource_sharing_environment_mappings
                    (participant_id, source_environment_id, destination_environment_id, created_at, updated_at)
                select id, (select id from environments where identifier = ?), (select id from environments where identifier = ?), current_timestamp(6), current_timestamp(6)
                from resource_sharing_participants where identifier = ?
                """,
                source,
                destination,
                participant);
    }

    private void insertEnvironment(String identifier) {
        jdbc.update(
                "INSERT IGNORE INTO type_authorizations (code, label, description, sort_order,"
                    + " is_active, settings) VALUES ('DEV', 'Dev', 'Developer', 1, true, '{}')");
        jdbc.update(
                "INSERT IGNORE INTO environment_types (code, name, description, root_allowed,"
                    + " workspace_required, lifecycle_code, display_order, created_at, updated_at)"
                    + " VALUES ('DEFAULT', 'Default', 'Default', true, false, 'ACTIVE', 1, NOW(6),"
                    + " NOW(6))");
        jdbc.update(
                """
                INSERT IGNORE INTO environments (identifier, environment_type_code, name, description,
                    authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
                SELECT ?, code, ?, 'Shared test environment', 'DEV', '{}', 1, 'ACTIVE', NOW(6), NOW(6)
                FROM environment_types WHERE code = 'DEFAULT'
                """,
                identifier,
                identifier);
    }

    private Map<String, Object> statusChangeRequest(
            String action, Map<String, Object> configuration) {
        Map<String, Object> request = new LinkedHashMap<>(configuration);
        request.put("action", action);
        return request;
    }

    private Map<String, Object> approvalRequest(String publicationMode) {
        return approvalRequest(publicationMode, UUID.randomUUID().toString());
    }

    private Map<String, Object> approvalRequest(
            String publicationMode, String environmentIdentifier) {
        insertEnvironment(environmentIdentifier);
        return Map.of(
                "publicationModeCode",
                publicationMode,
                "environmentMappings",
                Map.of(
                        "mappings",
                        List.of(
                                Map.of(
                                        "sourceEnvironmentIdentifier",
                                        environmentIdentifier,
                                        "destinationEnvironmentIdentifiers",
                                        List.of(environmentIdentifier)))));
    }

    private ValidatableResponse requestParticipant(Scope participantScope, String contract) {
        return authorized()
                .post(
                        participantScope.basePath()
                                + "/resource-sharing/contracts/"
                                + contract
                                + "/participants")
                .then();
    }

    private Map<String, Object> workspaceRequest(String prefix, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "MANAGER");
        request.put(
                "name",
                prefix.substring(0, Math.min(prefix.length(), 25))
                        + " Workspace "
                        + UUID.randomUUID().toString().substring(0, 8));
        request.put("description", "Workspace usado nos testes Shared");
        request.put("requester", "requester");
        request.put("acronym", "SHR");
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", Map.of());
        request.put("emailGroup", "shared@portalmanager.com");
        request.put(
                "approvers",
                List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com")));
        request.put("tags", List.of());
        return request;
    }

    private Map<String, Object> applicationRequest(String prefix, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put(
                "name",
                prefix.substring(0, Math.min(prefix.length(), 25))
                        + " App "
                        + UUID.randomUUID().toString().substring(0, 8));
        request.put("alias", "shared-" + UUID.randomUUID().toString().substring(0, 8));
        request.put("acronym", "SHR");
        request.put("applicationScope", "SHARED");
        request.put("settings", Map.of("language", "JAVA", "infrastructure", "VM"));
        if (authorizerGroup != null) request.put("authorizerGroup", authorizerGroup);
        request.put("tags", List.of());
        return request;
    }

    private ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private ValidatableResponse put(String path, Object body) {
        RequestSpecification request = authorized().contentType(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().put(path).then();
    }

    private ValidatableResponse post(String path, Object body) {
        RequestSpecification request = authorized().contentType(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }

    private ValidatableResponse patch(String path) {
        return authorized().when().patch(path).then();
    }

    private ValidatableResponse patch(String path, Object body) {
        RequestSpecification request = authorized().contentType(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().patch(path).then();
    }

    private ValidatableResponse delete(String path) {
        return authorized().when().delete(path).then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given().port(port)
                .header("correlation-id", "shared-api-it")
                .header("Authorization", "Bearer shared-api-it")
                .accept(ContentType.JSON);
    }

    private record Scope(String workspace, String application) {
        String basePath() {
            return "/api/v1/workspaces/" + workspace + "/applications/" + application;
        }

        String contractsPath() {
            return basePath() + "/resource-sharing/contracts";
        }

        String contractPath(String contract) {
            return contractsPath() + "/" + contract;
        }
    }
}
