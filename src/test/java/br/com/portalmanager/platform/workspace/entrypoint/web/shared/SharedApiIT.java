package br.com.portalmanager.platform.workspace.entrypoint.web.shared;

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
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class SharedApiIT {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        seedCatalogs();
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void participantCanResubmitRevokedRequestAndLeaveAfterRejection() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, owner, contract)
                .statusCode(201).body("status", equalTo("PENDING")).extract().path("identifier");
        requestParticipation(participant, owner, contract).statusCode(409);

        post(owner.contractPath(contract) + "/participations/" + participation + "/approval",
                Map.of("publicationModeCode", "UNKNOWN", "environmentMappings", Map.of("mappings", List.of())))
                .statusCode(400);
        Map<String, Object> invalidMappings = new LinkedHashMap<>();
        invalidMappings.put("mappings", null);
        post(owner.contractPath(contract) + "/participations/" + participation + "/approval",
                Map.of("publicationModeCode", "AUTOMATIC", "environmentMappings", invalidMappings))
                .statusCode(400);
        Map<String, Object> blankSourceMapping = new LinkedHashMap<>();
        blankSourceMapping.put("sourceEnvironmentIdentifier", " ");
        blankSourceMapping.put("destinationEnvironmentIdentifiers", List.of(UUID.randomUUID().toString()));
        post(owner.contractPath(contract) + "/participations/" + participation + "/approval",
                Map.of("publicationModeCode", "AUTOMATIC",
                        "environmentMappings", Map.of("mappings", List.of(blankSourceMapping))))
                .statusCode(400);
        post(owner.contractPath(contract) + "/participations/" + participation + "/approval",
                Map.of("publicationModeCode", "AUTOMATIC", "environmentMappings", Map.of("mappings", List.of())))
                .statusCode(200).body("status", equalTo("APPROVED"));
        String sourceEnvironment = UUID.randomUUID().toString();
        insertMapping(participation, sourceEnvironment, UUID.randomUUID().toString());
        insertMapping(participation, sourceEnvironment, UUID.randomUUID().toString());
        assertThat(jdbc.queryForObject("select count(*) from shared_environment_mappings m join shared_participations p on p.id = m.participation_id where p.identifier = ? and m.source_environment_identifier = ?",
                Integer.class, participation, sourceEnvironment)).isEqualTo(2);
        jdbc.update("delete from shared_environment_mappings where participation_id = (select id from shared_participations where identifier = ?)",
                participation);
        post(owner.contractPath(contract) + "/participations/" + participation + "/revocation", null)
                .statusCode(200).body("status", equalTo("REVOKED"));

        post(participant.basePath() + "/participations/" + participation + "/resubmission", null)
                .statusCode(200).body("status", equalTo("PENDING"));
        post(owner.contractPath(contract) + "/participations/" + participation + "/rejection", null)
                .statusCode(200).body("status", equalTo("REJECTED"));

        delete(participant.basePath() + "/participations/" + participation).statusCode(204);
        assertThat(jdbc.queryForObject("select count(*) from shared_participations where identifier = ?",
                Integer.class, participation)).isZero();
    }

    @Test
    void participantCanLeaveApprovedParticipationAndDestinationScopeMustMatchContractOwner() {
        Scope owner = createScope("Owner");
        Scope otherOwner = createScope("OtherOwner");
        Scope participant = createScope("Participant");
        String contract = createContract(owner);

        post(participant.basePath() + "/shared-contracts/" + contract + "/participations/destinations/"
                + otherOwner.workspace() + "/applications/" + otherOwner.application(), null)
                .statusCode(404);

        String participation = requestParticipation(participant, owner, contract)
                .statusCode(201).extract().path("identifier");
        post(owner.contractPath(contract) + "/participations/" + participation + "/approval",
                Map.of("publicationModeCode", "MANUAL", "environmentMappings", Map.of("mappings", List.of())))
                .statusCode(200).body("status", equalTo("APPROVED"));

        delete(participant.basePath() + "/participations/" + participation).statusCode(204);
        assertThat(jdbc.queryForObject("select count(*) from shared_participations where identifier = ?",
                Integer.class, participation)).isZero();
        get(owner.contractPath(contract)).statusCode(200);
    }

    @Test
    void inactiveContractIsHiddenFromParticipantsAndDeletionCascades() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, owner, contract)
                .statusCode(201).body("status", equalTo("PENDING")).extract().path("identifier");

        patch(owner.contractPath(contract) + "/inactivate").statusCode(200)
                .body("lifecycle", equalTo("INACTIVE"));
        get(owner.contractPath(contract)).statusCode(200).body("identifier", equalTo(contract));
        get(participant.basePath() + "/shared-contracts/available").statusCode(200)
                .body("identifier", not(hasItem(contract)));
        get(participant.basePath() + "/participations/" + participation).statusCode(404);

        patch(owner.contractPath(contract) + "/activate").statusCode(200)
                .body("lifecycle", equalTo("ACTIVE"));
        get(participant.basePath() + "/participations/" + participation).statusCode(200)
                .body("status", equalTo("PENDING"));

        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        delete(owner.contractPath(contract)).statusCode(204);
        assertThat(jdbc.queryForObject("select count(*) from shared_participations where identifier = ?",
                Integer.class, participation)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from shared_contracts where identifier = ?",
                Integer.class, contract)).isZero();
    }

    @Test
    void authorizationUsesTheWorkspaceAndApplicationFromEachControllerPath() {
        Scope owner = createScope("Owner", "OWNER_SPACE", "OWNER_APP");
        Scope other = createScope("Other", "OTHER_SPACE", "OTHER_APP");
        Scope participant = createScope("Participant", "PART_SPACE", "PART_APP");
        String ownerContract = createContract(owner);
        String otherContract = createContract(other);

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_OWNER_SPACE", "DEV", "DEV", "OWNER_SPACE")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_OWNER_APP", "DEV", "DEV", "A-OWNER_APP"));

        get(owner.contractPath(ownerContract)).statusCode(200);
        get(other.contractPath(otherContract)).statusCode(anyOf(is(403), is(404)));
        authorization.verifyCalledWithPolicy("DEV");

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PART_SPACE", "DEV", "DEV", "PART_SPACE")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_PART_APP", "DEV", "DEV", "A-PART_APP"));
        get(participant.basePath() + "/participations").statusCode(200);
        get(owner.basePath() + "/participations").statusCode(anyOf(is(403), is(404)));
        authorization.verifyCalledWithPolicy("DEV");
    }

    private void seedCatalogs() {
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle', 3, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend application', 1, true, '{}')");
    }

    private Scope createScope(String prefix) {
        return createScope(prefix, null, null);
    }

    private Scope createScope(String prefix, String workspaceAuthorizer, String applicationAuthorizer) {
        String workspace = post("/api/v1/workspaces", workspaceRequest(prefix, workspaceAuthorizer))
                .statusCode(201).extract().path("identifier");
        String application = post("/api/v1/workspaces/" + workspace + "/applications",
                applicationRequest(prefix, applicationAuthorizer))
                .statusCode(201).extract().path("identifier");
        return new Scope(workspace, application);
    }

    private String createContract(Scope owner) {
        return post(owner.contractsPath(), Map.of("name", "Shared " + UUID.randomUUID(), "description", "Contrato de teste"))
                .statusCode(201).extract().path("identifier");
    }

    private void insertMapping(String participation, String source, String destination) {
        jdbc.update("""
                insert into shared_environment_mappings
                    (participation_id, source_environment_identifier, destination_environment_identifier, created_at, updated_at)
                select id, ?, ?, current_timestamp(6), current_timestamp(6)
                from shared_participations where identifier = ?
                """, source, destination, participation);
    }

    private ValidatableResponse requestParticipation(Scope participant, Scope owner, String contract) {
        return post(participant.basePath() + "/shared-contracts/" + contract + "/participations/destinations/"
                + owner.workspace() + "/applications/" + owner.application(), null);
    }

    private Map<String, Object> workspaceRequest(String prefix, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "MANAGER");
        request.put("name", prefix + " Workspace " + UUID.randomUUID());
        request.put("description", "Workspace usado nos testes Shared");
        request.put("requester", "requester");
        request.put("acronym", "SHR");
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", Map.of());
        request.put("emailGroup", "shared@portalmanager.com");
        request.put("approvers", List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com")));
        request.put("tags", List.of());
        return request;
    }

    private Map<String, Object> applicationRequest(String prefix, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", prefix + " Application " + UUID.randomUUID());
        request.put("alias", "shared-" + UUID.randomUUID().toString().substring(0, 8));
        request.put("acronym", "SHR");
        request.put("applicationScope", "BACKEND");
        if (authorizerGroup != null) request.put("authorizerGroup", authorizerGroup);
        request.put("tags", List.of());
        return request;
    }

    private ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private ValidatableResponse post(String path, Object body) {
        var request = authorized().contentType(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }

    private ValidatableResponse patch(String path) {
        return authorized().when().patch(path).then();
    }

    private ValidatableResponse delete(String path) {
        return authorized().when().delete(path).then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given().port(port).header("correlation-id", "shared-api-it")
                .header("Authorization", "Bearer shared-api-it").accept(ContentType.JSON);
    }

    private record Scope(String workspace, String application) {
        String basePath() { return "/api/v1/workspaces/" + workspace + "/applications/" + application; }
        String contractsPath() { return basePath() + "/shared/contracts"; }
        String contractPath(String contract) { return contractsPath() + "/" + contract; }
    }
}
