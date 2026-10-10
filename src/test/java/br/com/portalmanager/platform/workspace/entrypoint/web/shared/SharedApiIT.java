package br.com.portalmanager.platform.workspace.entrypoint.web.shared;

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

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class SharedApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    private String sharedFeatureIdentifier;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> true);
        registry.add("spring.jpa.properties.hibernate.session.events.log", () -> false);
        registry.add("platform.audit.enabled", () -> true);
        registry.add("platform.audit.service-name", () -> "workspace-service");
    }

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        seedCatalogs();
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        sharedFeatureIdentifier = createPlatformFeature(true);
    }

    @Test
    void sharedListsPaginateAndValidateBeforeLoadingTheirResults() {
        Scope owner = createScope("PagedOwner");
        Scope participant = createScope("PagedParticipant");
        String firstContract = createContract(owner);
        String secondContract = createContract(owner);
        String firstParticipation = requestParticipation(participant, firstContract)
            .statusCode(201)
            .extract()
            .path("identifier");
        String secondParticipation = requestParticipation(participant, secondContract)
            .statusCode(201)
            .extract()
            .path("identifier");
        Scope anotherParticipant = createScope("AnotherPagedParticipant");
        String anotherParticipation = requestParticipation(anotherParticipant, firstContract)
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
            .get(participant.basePath() + "/shared-participations")
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(secondParticipation)))
            .body("totalElements", equalTo(2));
        authorized()
            .queryParam("page", 1)
            .queryParam("size", 1)
            .get(participant.basePath() + "/shared-participations")
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(firstParticipation)));
        String ownerParticipationPath = owner.contractPath(firstContract) + "/participations";
        authorized()
            .queryParam("size", 1)
            .get(ownerParticipationPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(firstParticipation)))
            .body("totalElements", equalTo(2));
        authorized()
            .queryParam("page", 1)
            .queryParam("size", 1)
            .get(ownerParticipationPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(anotherParticipation)));
        authorized()
            .queryParam("participantName", " anotherpagedparticipant ")
            .queryParam("size", 1)
            .get(ownerParticipationPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(anotherParticipation)))
            .body("totalElements", equalTo(1));
        authorized()
            .queryParam("participantName", "%")
            .get(ownerParticipationPath)
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(0));
        authorized()
            .queryParam("participantApplicationIdentifier", anotherParticipant.application())
            .get(ownerParticipationPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(anotherParticipation)));
        for (String path : List.of(
            owner.contractsPath(),
            ownerParticipationPath,
            participant.basePath() + "/shared-participations"
        )) {
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
            .get(ownerParticipationPath)
            .then()
            .statusCode(400);
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        authorized().queryParam("status", "UNKNOWN").get(ownerParticipationPath).then().statusCode(400);
        assertThat(statistics.getPrepareStatementCount()).isZero();
    }

    @Test
    void sharedListQueryCountsDoNotGrowWithTheNumberOfPageItems() {
        Scope owner = createScope("QueryCountOwner");
        Scope participant = createScope("QueryCountParticipant");
        String firstContract = null;
        for (int index = 0; index < 4; index++) {
            String contract = createContract(owner);
            if (firstContract == null) firstContract = contract;
            String participation = requestParticipation(participant, contract)
                .statusCode(201)
                .extract()
                .path("identifier");
            patch(
                owner.contractPath(contract) + "/participations/" + participation + "/status",
                statusChangeRequest("APPROVE", approvalRequest("MANUAL"))
            ).statusCode(200);
        }
        for (int index = 0; index < 3; index++) {
            Scope anotherParticipant = createScope("QueryCountAnother" + index);
            String participation = requestParticipation(anotherParticipant, firstContract)
                .statusCode(201)
                .extract()
                .path("identifier");
            patch(
                owner.contractPath(firstContract) + "/participations/" + participation + "/status",
                statusChangeRequest("APPROVE", approvalRequest("MANUAL"))
            ).statusCode(200);
        }
        for (String path : List.of(
            owner.contractsPath(),
            participant.basePath() + "/shared-participations",
            owner.contractPath(firstContract) + "/participations",
            participant.basePath() + "/shared-contracts/available"
        )) {
            Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
            statistics.clear();
            authorized().queryParam("size", 1).get(path).then().statusCode(200).body("content.size()", equalTo(1));
            long singleItemStatements = statistics.getPrepareStatementCount();
            statistics.clear();
            authorized().queryParam("size", 4).get(path).then().statusCode(200).body("content.size()", equalTo(4));
            long fourItemStatements = statistics.getPrepareStatementCount();
            assertThat(fourItemStatements)
                .as("SQL statements for %s", path)
                .isLessThanOrEqualTo(singleItemStatements + 1);
            System.out.printf(
                "[SHARED-QUERY-COUNT] %s single=%d four=%d%n",
                path,
                singleItemStatements,
                fourItemStatements
            );
        }
    }

    @RepeatedTest(5)
    void approvalAndParticipationDeletionRemainConsistentWhenConcurrent() throws Exception {
        Scope owner = createScope("ConcurrentApprovalOwner");
        Scope participant = createScope("ConcurrentDeletionParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        Map<String, Object> approval = statusChangeRequest("APPROVE", approvalRequest("MANUAL"));
        List<Integer> statuses = concurrentStatuses(
            () ->
                authorized()
                    .contentType(ContentType.JSON)
                    .body(approval)
                    .patch(owner.contractPath(contract) + "/participations/" + participation + "/status")
                    .statusCode(),
            () ->
                authorized()
                    .delete(participant.basePath() + "/shared-participations/" + participation)
                    .statusCode()
        );
        assertThat(statuses).isIn(List.of(200, 204), List.of(404, 204));
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM shared_participations WHERE identifier = ?",
                Integer.class,
                participation
            )
        ).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM shared_environment_mappings", Integer.class)).isZero();
        assertThat(participationAuditCount(participation, "SHARED_PARTICIPATION_APPROVED")).isEqualTo(
            statuses.getFirst() == 200 ? 1 : 0
        );
        assertThat(participationAuditCount(participation, "SHARED_PARTICIPATION_DELETED")).isEqualTo(1);
    }

    @RepeatedTest(5)
    void configurationAndRevocationRemainConsistentWhenConcurrent() throws Exception {
        Scope owner = createScope("ConcurrentConfigurationOwner");
        Scope participant = createScope("ConcurrentRevocationParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        String participationPath = owner.contractPath(contract) + "/participations/" + participation;
        patch(participationPath + "/status", statusChangeRequest("APPROVE", approvalRequest("AUTOMATIC"))).statusCode(
            200
        );
        Map<String, Object> configuration = approvalRequest("MANUAL");
        List<Integer> statuses = concurrentStatuses(
            () ->
                authorized()
                    .contentType(ContentType.JSON)
                    .body(configuration)
                    .put(participationPath + "/configuration")
                    .statusCode(),
            () ->
                authorized()
                    .contentType(ContentType.JSON)
                    .body(Map.of("action", "REVOKE"))
                    .patch(participationPath + "/status")
                    .statusCode()
        );
        assertThat(statuses).isIn(List.of(200, 200), List.of(409, 200));
        get(participationPath)
            .statusCode(200)
            .body("status", equalTo("REVOKED"))
            .body("publicationMode", equalTo(statuses.getFirst() == 200 ? "MANUAL" : "AUTOMATIC"))
            .body("mappings.size()", equalTo(1));
        assertThat(participationAuditCount(participation, "SHARED_PARTICIPATION_CONFIGURATION_UPDATED")).isEqualTo(
            statuses.getFirst() == 200 ? 1 : 0
        );
        assertThat(participationAuditCount(participation, "SHARED_PARTICIPATION_REVOKED")).isEqualTo(1);
    }

    @RepeatedTest(5)
    void concurrentConfigurationsUseTheCurrentMappingsAfterWaitingForTheLock() throws Exception {
        Scope owner = createScope("ConcurrentMappingsOwner");
        Scope participant = createScope("ConcurrentMappingsParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        String participationPath = owner.contractPath(contract) + "/participations/" + participation;
        patch(participationPath + "/status", statusChangeRequest("APPROVE", approvalRequest("AUTOMATIC"))).statusCode(
            200
        );
        String manualEnvironment = UUID.randomUUID().toString();
        String automaticEnvironment = UUID.randomUUID().toString();
        Map<String, Object> manualConfiguration = approvalRequest("MANUAL", manualEnvironment);
        Map<String, Object> automaticConfiguration = approvalRequest("AUTOMATIC", automaticEnvironment);
        List<Integer> statuses = concurrentStatuses(
            () ->
                authorized()
                    .contentType(ContentType.JSON)
                    .body(manualConfiguration)
                    .put(participationPath + "/configuration")
                    .statusCode(),
            () ->
                authorized()
                    .contentType(ContentType.JSON)
                    .body(automaticConfiguration)
                    .put(participationPath + "/configuration")
                    .statusCode()
        );
        assertThat(statuses).containsExactly(200, 200);
        ExtractableResponse<Response> detail = get(participationPath)
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("mappings.size()", equalTo(1))
            .extract();
        String expectedEnvironment = "MANUAL".equals(detail.path("publicationMode"))
            ? manualEnvironment
            : automaticEnvironment;
        assertThat((String) detail.path("mappings[0].sourceEnvironmentIdentifier")).isEqualTo(expectedEnvironment);
        assertThat((String) detail.path("mappings[0].destinationEnvironmentIdentifier")).isEqualTo(expectedEnvironment);
        assertThat(participationAuditCount(participation, "SHARED_PARTICIPATION_CONFIGURATION_UPDATED")).isEqualTo(2);
    }

    private List<Integer> concurrentStatuses(Callable<Integer> firstRequest, Callable<Integer> secondRequest)
        throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> firstResponse = executor.submit(() -> {
                start.await();
                return firstRequest.call();
            });
            Future<Integer> secondResponse = executor.submit(() -> {
                start.await();
                return secondRequest.call();
            });
            start.countDown();
            return List.of(firstResponse.get(15, TimeUnit.SECONDS), secondResponse.get(15, TimeUnit.SECONDS));
        }
    }

    private int participationAuditCount(String participationIdentifier, String eventType) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.resourceIdentifier')) = ? AND JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.eventType')) = ?",
            Integer.class,
            participationIdentifier,
            eventType
        );
    }

    @Test
    void validatesOwnerTypeAndReferencesOnContractUpdatesWithoutPartialChanges() {
        Scope owner = createScope("ContractUpdateOwner");
        Scope otherOwner = createScope("ContractUpdateOtherOwner");
        String contract = createContract(owner);
        Map<String, Object> update = Map.of(
            "featureIdentifier",
            sharedFeatureIdentifier,
            "description",
            "Must not persist"
        );
        for (String type : List.of("BACKEND", "FRONTEND")) {
            jdbc.update(
                "UPDATE applications SET application_scope_code = ? WHERE identifier = ?",
                type,
                owner.application()
            );
            put(owner.contractPath(contract), update).statusCode(400);
        }
        jdbc.update(
            "UPDATE applications SET application_scope_code = 'SHARED' WHERE identifier = ?",
            owner.application()
        );
        put(otherOwner.contractPath(contract), update).statusCode(404);
        put(owner.contractPath(contract), Map.of("description", "Must not persist")).statusCode(400);
        put(
            owner.contractPath(contract),
            Map.of("featureIdentifier", sharedFeatureIdentifier, "description", "x".repeat(501))
        ).statusCode(400);
        put(
            owner.contractPath(contract),
            Map.of("featureIdentifier", createPlatformFeature(true), "description", "Must not persist")
        ).statusCode(409);
        patch("/api/v1/platform/features/" + sharedFeatureIdentifier + "/inactivate").statusCode(200);
        put(owner.contractPath(contract), update).statusCode(404);
        patch("/api/v1/platform/features/" + sharedFeatureIdentifier + "/activate").statusCode(200);
        get(owner.contractPath(contract)).statusCode(200).body("description", equalTo("Contrato de teste"));
        put(
            owner.contractPath(contract),
            Map.of("featureIdentifier", sharedFeatureIdentifier, "description", " Valid update ")
        )
            .statusCode(200)
            .body("description", equalTo("Valid update"));
        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        put(owner.contractPath(contract), update).statusCode(404);
        jdbc.update(
            "UPDATE applications SET application_scope_code = 'BACKEND' WHERE identifier = ?",
            owner.application()
        );
        patch(owner.contractPath(contract) + "/activate").statusCode(400);
        jdbc.update(
            "UPDATE applications SET application_scope_code = 'SHARED' WHERE identifier = ?",
            owner.application()
        );
        patch("/api/v1/platform/features/" + sharedFeatureIdentifier + "/inactivate").statusCode(200);
        patch(owner.contractPath(contract) + "/activate").statusCode(404);
        patch("/api/v1/platform/features/" + sharedFeatureIdentifier + "/activate").statusCode(200);
        patch(owner.contractPath(contract) + "/activate").statusCode(200);
    }

    @Test
    void applicationCannotChangeTypeWhileActiveOrInactiveContractsExist() {
        Scope owner = createScope("ApplicationIntegrityOwner");
        String contract = createContract(owner);
        put(owner.basePath(), applicationUpdate(owner, "BACKEND")).statusCode(409);
        put(owner.basePath(), applicationUpdate(owner, "FRONTEND")).statusCode(409);
        put(owner.basePath(), applicationUpdate(owner, "SHARED")).statusCode(200);
        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        put(owner.basePath(), applicationUpdate(owner, "BACKEND")).statusCode(409);
        delete(owner.contractPath(contract)).statusCode(204);
        put(owner.basePath(), applicationUpdate(owner, "BACKEND"))
            .statusCode(200)
            .body("applicationScope", equalTo("BACKEND"));
        post(owner.contractsPath(), Map.of("featureIdentifier", sharedFeatureIdentifier)).statusCode(400);
    }

    @RepeatedTest(5)
    void applicationTypeChangeCannotRaceContractCreation() throws Exception {
        Scope owner = createScope("ConcurrentApplicationTypeOwner");
        Map<String, Object> typeChange = applicationUpdate(owner, "BACKEND");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> creation = executor.submit(() -> {
                start.await();
                return authorized()
                    .contentType(ContentType.JSON)
                    .body(Map.of("featureIdentifier", sharedFeatureIdentifier))
                    .post(owner.contractsPath())
                    .statusCode();
            });
            Future<Integer> editing = executor.submit(() -> {
                start.await();
                return authorized().contentType(ContentType.JSON).body(typeChange).put(owner.basePath()).statusCode();
            });
            start.countDown();
            assertThat(List.of(creation.get(15, TimeUnit.SECONDS), editing.get(15, TimeUnit.SECONDS))).isIn(
                List.of(201, 409),
                List.of(400, 200)
            );
        }
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM shared_contracts contract JOIN applications application ON application.id = contract.owner_application_id WHERE application.identifier = ? AND application.application_scope_code <> 'SHARED'",
                Integer.class,
                owner.application()
            )
        ).isZero();
    }

    @RepeatedTest(5)
    void concurrentDuplicateContractRequestsCreateOnlyOneContract() throws Exception {
        Scope owner = createScope("ConcurrentDuplicateOwner");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.Callable<Integer> request = () -> {
                start.await();
                return authorized()
                    .contentType(ContentType.JSON)
                    .body(Map.of("featureIdentifier", sharedFeatureIdentifier))
                    .post(owner.contractsPath())
                    .statusCode();
            };
            Future<Integer> first = executor.submit(request);
            Future<Integer> second = executor.submit(request);
            start.countDown();
            assertThat(
                List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))
            ).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM shared_contracts contract JOIN applications application ON application.id = contract.owner_application_id WHERE application.identifier = ?",
                Integer.class,
                owner.application()
            )
        ).isEqualTo(1);
    }

    private Map<String, Object> applicationUpdate(Scope owner, String applicationScope) {
        ExtractableResponse<Response> application = get(owner.basePath()).statusCode(200).extract();
        return Map.of(
            "version",
            application.path("version"),
            "name",
            application.path("name"),
            "alias",
            application.path("alias"),
            "acronym",
            application.path("acronym"),
            "applicationScope",
            applicationScope,
            "settings",
            Map.of(),
            "tags",
            List.of()
        );
    }

    @Test
    void contractsAreUniqueByOwnerAndFeatureAndReturnTheCurrentFeatureName() {
        Scope owner = createScope("FeatureIdentityOwner");
        Scope participant = createScope("FeatureIdentityParticipant");
        String contract = createContract(owner, sharedFeatureIdentifier);
        post(owner.contractsPath(), Map.of("featureIdentifier", sharedFeatureIdentifier)).statusCode(409);
        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        post(owner.contractsPath(), Map.of("featureIdentifier", sharedFeatureIdentifier)).statusCode(409);
        patch(owner.contractPath(contract) + "/activate").statusCode(200);
        Map<String, Object> featureRequest = new LinkedHashMap<>(featureUpdate(sharedFeatureIdentifier, true));
        featureRequest.put("name", "Renamed shared feature");
        put("/api/v1/platform/features/" + sharedFeatureIdentifier, featureRequest).statusCode(200);
        get(owner.contractPath(contract)).statusCode(200).body("name", equalTo("Renamed shared feature"));
        get(participant.basePath() + "/shared-contracts/available/" + contract)
            .statusCode(200)
            .body("name", equalTo("Renamed shared feature"));
        requestParticipation(participant, contract)
            .statusCode(201)
            .body("contract.name", equalTo("Renamed shared feature"));
        put(
            owner.contractPath(contract),
            Map.of("featureIdentifier", sharedFeatureIdentifier, "description", "Updated description")
        )
            .statusCode(200)
            .body("name", equalTo("Renamed shared feature"))
            .body("description", equalTo("Updated description"));
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'shared_contracts' AND column_name = 'name'",
                Integer.class
            )
        ).isZero();
    }

    @Test
    void onlySharedApplicationsCanCreateContracts() {
        for (String applicationScope : List.of("BACKEND", "FRONTEND", "SHARED")) {
            Scope owner = createScope("OwnerType" + applicationScope);
            jdbc.update(
                "UPDATE applications SET application_scope_code = ? WHERE identifier = ?",
                applicationScope,
                owner.application()
            );
            post(owner.contractsPath(), Map.of("featureIdentifier", sharedFeatureIdentifier)).statusCode(
                "SHARED".equals(applicationScope) ? 201 : 400
            );
            assertThat(
                jdbc.queryForObject(
                    "SELECT COUNT(*) FROM shared_contracts contract JOIN applications application ON application.id = contract.owner_application_id WHERE application.identifier = ?",
                    Integer.class,
                    owner.application()
                )
            ).isEqualTo("SHARED".equals(applicationScope) ? 1 : 0);
        }
    }

    @RepeatedTest(5)
    void featureCannotBeDisabledConcurrentlyWithContractCreation() throws Exception {
        Scope owner = createScope("ConcurrentFeatureOwner");
        String feature = createPlatformFeature(true);
        Map<String, Object> disableRequest = featureUpdate(feature, false);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Integer> creation = executor.submit(() -> {
                start.await();
                return authorized()
                    .contentType(ContentType.JSON)
                    .body(Map.of("featureIdentifier", feature))
                    .post(owner.contractsPath())
                    .statusCode();
            });
            Future<Integer> disabling = executor.submit(() -> {
                start.await();
                return authorized()
                    .contentType(ContentType.JSON)
                    .body(disableRequest)
                    .put("/api/v1/platform/features/" + feature)
                    .statusCode();
            });
            start.countDown();
            List<Integer> statuses = List.of(creation.get(15, TimeUnit.SECONDS), disabling.get(15, TimeUnit.SECONDS));
            assertThat(statuses).isIn(List.of(201, 409), List.of(400, 200));
        }
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM shared_contracts contract JOIN platform_features feature ON feature.id = contract.feature_id WHERE feature.identifier = ? AND feature.shareable = false",
                Integer.class,
                feature
            )
        ).isZero();
    }

    @Test
    void featuresDefaultToNotShareableAndCannotDisableSharingWhileAnyContractExists() {
        Scope owner = createScope("FeatureOwner");
        String feature = createPlatformFeature(null);
        String featurePath = "/api/v1/platform/features/" + feature;
        get(featurePath).statusCode(200).body("shareable", equalTo(false));
        post(owner.contractsPath(), Map.of("featureIdentifier", feature)).statusCode(400);
        post(owner.contractsPath(), Map.of()).statusCode(400);
        post(owner.contractsPath(), Map.of("featureIdentifier", UUID.randomUUID().toString())).statusCode(404);
        put(featurePath, featureUpdate(feature, true)).statusCode(200).body("shareable", equalTo(true));
        String firstContract = createContract(owner, feature);
        Scope secondOwner = createScope("SecondFeatureOwner");
        String secondContract = createContract(secondOwner, feature);
        get(owner.contractPath(firstContract)).statusCode(200).body("featureIdentifier", equalTo(feature));
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM shared_contracts contract JOIN platform_features feature ON feature.id = contract.feature_id WHERE contract.identifier = ? AND feature.identifier = ?",
                Integer.class,
                firstContract,
                feature
            )
        ).isEqualTo(1);
        put(owner.contractPath(firstContract), Map.of("featureIdentifier", sharedFeatureIdentifier)).statusCode(409);
        put(featurePath, featureUpdate(feature, false)).statusCode(409);
        put(featurePath, featureUpdate(feature, true)).statusCode(200).body("shareable", equalTo(true));
        get(featurePath).statusCode(200).body("shareable", equalTo(true));
        patch(owner.contractPath(firstContract) + "/inactivate").statusCode(200);
        patch(secondOwner.contractPath(secondContract) + "/inactivate").statusCode(200);
        put(featurePath, featureUpdate(feature, false)).statusCode(409);
        delete(owner.contractPath(firstContract)).statusCode(204);
        put(featurePath, featureUpdate(feature, false)).statusCode(409);
        delete(secondOwner.contractPath(secondContract)).statusCode(204);
        put(featurePath, featureUpdate(feature, false)).statusCode(200).body("shareable", equalTo(false));
    }

    @Test
    void inactiveFeaturesDisappearFromDiscoveryAndBlockNewParticipationWithoutHidingHistory() {
        Scope owner = createScope("FeatureLifecycleOwner");
        Scope participant = createScope("FeatureLifecycleParticipant");
        Scope otherParticipant = createScope("NewFeatureParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        get(participant.basePath() + "/shared-contracts/available")
            .statusCode(200)
            .body("content.featureIdentifier", hasItem(sharedFeatureIdentifier));
        jdbc.update(
            "UPDATE platform_features SET lifecycle_code = 'INACTIVE' WHERE identifier = ?",
            sharedFeatureIdentifier
        );
        get(participant.basePath() + "/shared-contracts/available")
            .statusCode(200)
            .body("content.size()", equalTo(0));
        get(participant.basePath() + "/shared-contracts/available/" + contract).statusCode(404);
        requestParticipation(otherParticipant, contract).statusCode(404);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest("APPROVE", approvalRequest("MANUAL"))
        ).statusCode(404);
        get(participant.basePath() + "/shared-participations/" + participation)
            .statusCode(200)
            .body("contract.featureIdentifier", equalTo(sharedFeatureIdentifier));
    }

    @Test
    void detailExcludesEnvironmentsWithInactiveAncestorsAndForeignWorkspaceMappings() {
        Scope owner = createScope("EnvironmentOwner");
        Scope participant = createScope("EnvironmentParticipant");
        Scope unrelated = createScope("UnrelatedEnvironment");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        String participationPath = owner.contractPath(contract) + "/participations/" + participation;
        String parentEnvironment = UUID.randomUUID().toString();
        String childEnvironment = UUID.randomUUID().toString();
        String foreignEnvironment = UUID.randomUUID().toString();
        String destinationEnvironment = UUID.randomUUID().toString();
        insertEnvironment(parentEnvironment);
        insertEnvironment(childEnvironment);
        insertEnvironment(foreignEnvironment);
        insertEnvironment(destinationEnvironment);
        jdbc.update(
            "UPDATE environments SET parent_environment_id = (SELECT id FROM (SELECT id FROM environments WHERE identifier = ?) parent) WHERE identifier = ?",
            parentEnvironment,
            childEnvironment
        );
        jdbc.update("UPDATE environments SET lifecycle_code = 'INACTIVE' WHERE identifier = ?", parentEnvironment);
        jdbc.update(
            "UPDATE environments SET workspace_id = (SELECT id FROM workspaces WHERE identifier = ?) WHERE identifier = ?",
            unrelated.workspace(),
            foreignEnvironment
        );
        get(participationPath)
            .statusCode(200)
            .body("sourceEnvironments.identifier", not(hasItem(parentEnvironment)))
            .body("sourceEnvironments.identifier", not(hasItem(childEnvironment)))
            .body("sourceEnvironments.identifier", not(hasItem(foreignEnvironment)));
        patch(
            participationPath + "/status",
            statusChangeRequest(
                "APPROVE",
                Map.of(
                    "publicationModeCode",
                    "MANUAL",
                    "environmentMappings",
                    Map.of(
                        "mappings",
                        List.of(
                            Map.of(
                                "sourceEnvironmentIdentifier",
                                foreignEnvironment,
                                "destinationEnvironmentIdentifiers",
                                List.of(destinationEnvironment)
                            )
                        )
                    )
                )
            )
        ).statusCode(404);
        get(participationPath).statusCode(200).body("status", equalTo("PENDING")).body("mappings.size()", equalTo(0));
    }

    @Test
    void ownerCanReadRejectedAndRevokedHistoryWithInactiveParticipantsAndContracts() {
        Scope owner = createScope("HistoryOwner");
        Scope participant = createScope("InactiveHistoryParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        String participationPath = owner.contractPath(contract) + "/participations/" + participation;
        patch(participationPath + "/status", Map.of("action", "REJECT")).statusCode(200);
        authorized()
            .queryParam("status", "REJECTED")
            .get(owner.contractPath(contract) + "/participations")
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(participation)));
        get(participationPath).statusCode(200).body("status", equalTo("REJECTED"));
        post(participant.basePath() + "/shared-participations/" + participation + "/request", null).statusCode(200);
        patch(participationPath + "/status", statusChangeRequest("APPROVE", approvalRequest("MANUAL"))).statusCode(200);
        patch(participationPath + "/status", Map.of("action", "REVOKE")).statusCode(200);
        jdbc.update("UPDATE workspaces SET lifecycle_code = 'INACTIVE' WHERE identifier = ?", participant.workspace());
        get(participationPath)
            .statusCode(200)
            .body("status", equalTo("REVOKED"))
            .body("sourceEnvironments.size()", equalTo(0));
        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        get(owner.contractPath(contract) + "/participations")
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(participation)));
        get(participationPath).statusCode(200).body("contract.lifecycle", equalTo("INACTIVE"));
        authorized()
            .queryParam("status", "UNKNOWN")
            .get(owner.contractPath(contract) + "/participations")
            .then()
            .statusCode(400);
    }

    @Test
    void auditSeparatesApprovalConfigurationAndDeletionAndRollsBackFailedOperations() {
        Scope owner = createScope("AuditOwner");
        Scope participant = createScope("AuditParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        String participationPath = owner.contractPath(contract) + "/participations/" + participation;
        patch(participationPath + "/status", statusChangeRequest("APPROVE", approvalRequest("AUTOMATIC"))).statusCode(
            200
        );
        Map<String, Object> updatedConfiguration = approvalRequest("MANUAL");
        put(participationPath + "/configuration", updatedConfiguration).statusCode(200);
        int eventsBeforeFailure = jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class);
        patch(participationPath + "/status", statusChangeRequest("APPROVE", updatedConfiguration)).statusCode(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(
            eventsBeforeFailure
        );
        delete(participant.basePath() + "/shared-participations/" + participation).statusCode(204);
        assertThat(
            jdbc.queryForList(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.eventType')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.resourceIdentifier')) = ? ORDER BY id",
                String.class,
                participation
            )
        ).containsExactly(
            "SHARED_PARTICIPATION_REQUESTED",
            "SHARED_PARTICIPATION_APPROVED",
            "SHARED_PARTICIPATION_CONFIGURATION_UPDATED",
            "SHARED_PARTICIPATION_DELETED"
        );
        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        delete(owner.contractPath(contract)).statusCode(204);
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.eventType')) = 'SHARED_CONTRACT_DELETED' AND JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.resourceIdentifier')) = ?",
                Integer.class,
                contract
            )
        ).isEqualTo(1);
    }

    @Test
    void participantHistoryRemainsReadableWhenOwnerWorkspaceIsInactive() {
        Scope owner = createScope("InactiveHistoryOwner");
        Scope participant = createScope("HistoryParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        jdbc.update("UPDATE workspaces SET lifecycle_code = 'INACTIVE' WHERE identifier = ?", owner.workspace());
        get(participant.basePath() + "/shared-participations")
            .statusCode(200)
            .body("content.identifier", hasItem(participation));
        get(participant.basePath() + "/shared-participations/" + participation)
            .statusCode(200)
            .body("contract.ownerWorkspaceIdentifier", equalTo(owner.workspace()));
        delete(participant.basePath() + "/shared-participations/" + participation).statusCode(204);
    }

    @Test
    void ownerNameFilterCanReadAnInactiveParticipantApplication() {
        Scope owner = createScope("NameFilterOwner");
        Scope participant = createScope("NameFilterParticipant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        jdbc.update(
            "UPDATE applications SET lifecycle_code = 'INACTIVE' WHERE identifier = ?",
            participant.application()
        );
        authorized()
            .queryParam("participantName", "NameFilterParticipant")
            .get(owner.contractPath(contract) + "/participations")
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(participation)));
    }

    @Test
    void configurationUpdatesOnlyApprovedParticipationsAndReplacesMappingsAtomically() {
        Scope owner = createScope("ConfigOwner");
        Scope participant = createScope("ConfigParticipant");
        Scope otherOwner = createScope("OtherConfigOwner");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        String participationPath = owner.contractPath(contract) + "/participations/" + participation;
        Map<String, Object> initialConfiguration = approvalRequest("AUTOMATIC");
        put(participationPath + "/configuration", initialConfiguration).statusCode(409);
        patch(participationPath + "/status", statusChangeRequest("APPROVE", initialConfiguration)).statusCode(200);
        patch(participationPath + "/status", statusChangeRequest("APPROVE", initialConfiguration)).statusCode(409);
        put(participationPath + "/configuration", null).statusCode(400);
        put(participationPath + "/configuration", Map.of("publicationModeCode", "MANUAL")).statusCode(400);
        put(participationPath + "/configuration", approvalRequest("UNKNOWN")).statusCode(400);
        Map<String, Object> updatedConfiguration = approvalRequest("MANUAL");
        put(
            otherOwner.contractPath(contract) + "/participations/" + participation + "/configuration",
            updatedConfiguration
        ).statusCode(404);
        put(participationPath + "/configuration", updatedConfiguration)
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("publicationMode", equalTo("MANUAL"))
            .body("mappings.size()", equalTo(1));
        put(participationPath + "/configuration", updatedConfiguration)
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("mappings.size()", equalTo(1));
        Map<String, Object> invalidConfiguration = Map.of(
            "publicationModeCode",
            "AUTOMATIC",
            "environmentMappings",
            Map.of(
                "mappings",
                List.of(
                    Map.of(
                        "sourceEnvironmentIdentifier",
                        UUID.randomUUID().toString(),
                        "destinationEnvironmentIdentifiers",
                        List.of(UUID.randomUUID().toString())
                    )
                )
            )
        );
        put(participationPath + "/configuration", invalidConfiguration).statusCode(404);
        get(participationPath)
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("publicationMode", equalTo("MANUAL"))
            .body("mappings.size()", equalTo(1));
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM shared_environment_mappings mapping JOIN shared_participations participation ON participation.id = mapping.participation_id WHERE participation.identifier = ?",
                Integer.class,
                participation
            )
        ).isEqualTo(1);
        patch(participationPath + "/status", Map.of("action", "REVOKE")).statusCode(200);
        put(participationPath + "/configuration", initialConfiguration).statusCode(409);
    }

    @Test
    void keepsPublicUuidsWhilePersistingInternalParticipationAndEnvironmentReferences() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        String contractIdentifier = createContract(owner);
        String participationIdentifier = requestParticipation(participant, contractIdentifier)
            .statusCode(201)
            .body("participantWorkspaceIdentifier", equalTo(participant.workspace()))
            .body("participantApplicationIdentifier", equalTo(participant.application()))
            .extract()
            .path("identifier");
        String environmentIdentifier = UUID.randomUUID().toString();
        insertEnvironment(environmentIdentifier);
        String participationPath =
            owner.contractPath(contractIdentifier) + "/participations/" + participationIdentifier;
        get(participationPath)
            .statusCode(200)
            .body("status", equalTo("PENDING"))
            .body("mappings.size()", equalTo(0))
            .body("sourceEnvironments.identifier", hasItem(environmentIdentifier));
        get(owner.contractPath(contractIdentifier) + "/participations")
            .statusCode(200)
            .body("content[0].containsKey('sourceEnvironments')", equalTo(false));
        patch(
            owner.contractPath(contractIdentifier) + "/participations/" + participationIdentifier + "/status",
            statusChangeRequest(
                "APPROVE",
                Map.of(
                    "publicationModeCode",
                    "AUTOMATIC",
                    "environmentMappings",
                    Map.of(
                        "mappings",
                        List.of(
                            Map.of(
                                "sourceEnvironmentIdentifier",
                                environmentIdentifier,
                                "destinationEnvironmentIdentifiers",
                                List.of(environmentIdentifier)
                            )
                        )
                    )
                )
            )
        )
            .statusCode(200)
            .body("mappings[0].sourceEnvironmentIdentifier", equalTo(environmentIdentifier))
            .body("mappings[0].destinationEnvironmentIdentifier", equalTo(environmentIdentifier));
        get(participationPath)
            .statusCode(200)
            .body("status", equalTo("APPROVED"))
            .body("sourceEnvironments.identifier", hasItem(environmentIdentifier))
            .body("mappings[0].sourceEnvironmentIdentifier", equalTo(environmentIdentifier))
            .body("mappings[0].destinationEnvironmentIdentifier", equalTo(environmentIdentifier));
        get(participationPath + "/source-environments").statusCode(404);
        get(participationPath + "/environment-mappings").statusCode(404);
        assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM shared_participations participation
                JOIN workspaces workspace ON workspace.id = participation.participant_workspace_id
                JOIN applications application ON application.id = participation.participant_application_id
                    AND application.workspace_id = workspace.id
                JOIN shared_environment_mappings mapping ON mapping.participation_id = participation.id
                JOIN environments source ON source.id = mapping.source_environment_id
                JOIN environments destination ON destination.id = mapping.destination_environment_id
                WHERE participation.identifier = ? AND workspace.identifier = ? AND application.identifier = ?
                    AND source.identifier = ? AND destination.identifier = ?
                """,
                Integer.class,
                participationIdentifier,
                participant.workspace(),
                participant.application(),
                environmentIdentifier,
                environmentIdentifier
            )
        ).isEqualTo(1);
    }

    @Test
    void participantCanRequestAgainAfterRevocationAndDeleteAfterRejection() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract)
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .extract()
            .path("identifier");
        requestParticipation(participant, contract).statusCode(409);

        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest("APPROVE", approvalRequest("UNKNOWN"))
        ).statusCode(400);
        patch(owner.contractPath(contract) + "/participations/" + participation + "/status", null).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest("APPROVE", Map.of("environmentMappings", Map.of("mappings", List.of())))
        ).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest("APPROVE", Map.of("publicationModeCode", "AUTOMATIC"))
        ).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest(
                "APPROVE",
                Map.of("publicationModeCode", "AUTOMATIC", "environmentMappings", Map.of("mappings", List.of()))
            )
        ).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            Map.of("action", "UNKNOWN")
        ).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            approvalRequest("AUTOMATIC")
        ).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            Map.of("action", "REVOKE")
        ).statusCode(409);
        Map<String, Object> invalidMappings = new LinkedHashMap<>();
        invalidMappings.put("mappings", null);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest(
                "APPROVE",
                Map.of("publicationModeCode", "AUTOMATIC", "environmentMappings", invalidMappings)
            )
        ).statusCode(400);
        Map<String, Object> blankSourceMapping = new LinkedHashMap<>();
        blankSourceMapping.put("sourceEnvironmentIdentifier", " ");
        blankSourceMapping.put("destinationEnvironmentIdentifiers", List.of(UUID.randomUUID().toString()));
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest(
                "APPROVE",
                Map.of(
                    "publicationModeCode",
                    "AUTOMATIC",
                    "environmentMappings",
                    Map.of("mappings", List.of(blankSourceMapping))
                )
            )
        ).statusCode(400);
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest("APPROVE", approvalRequest("AUTOMATIC"))
        )
            .statusCode(200)
            .body("status", equalTo("APPROVED"));
        String sourceEnvironment = UUID.randomUUID().toString();
        insertMapping(participation, sourceEnvironment, UUID.randomUUID().toString());
        insertMapping(participation, sourceEnvironment, UUID.randomUUID().toString());
        assertThat(
            jdbc.queryForObject(
                "select count(*) from shared_environment_mappings m join shared_participations p on p.id = m.participation_id where p.identifier = ? and m.source_environment_id = (select id from environments where identifier = ?)",
                Integer.class,
                participation,
                sourceEnvironment
            )
        ).isEqualTo(2);
        jdbc.update(
            "delete from shared_environment_mappings where participation_id = (select id from shared_participations where identifier = ?)",
            participation
        );
        patch(owner.contractPath(contract) + "/participations/" + participation + "/status", Map.of("action", "REVOKE"))
            .statusCode(200)
            .body("status", equalTo("REVOKED"));

        post(participant.basePath() + "/shared-participations/" + participation + "/request", null)
            .statusCode(200)
            .body("status", equalTo("PENDING"));
        patch(owner.contractPath(contract) + "/participations/" + participation + "/status", Map.of("action", "REJECT"))
            .statusCode(200)
            .body("status", equalTo("REJECTED"));

        delete(participant.basePath() + "/shared-participations/" + participation).statusCode(204);
        assertThat(
            jdbc.queryForObject(
                "select count(*) from shared_participations where identifier = ?",
                Integer.class,
                participation
            )
        ).isZero();
    }

    @Test
    void participantCanRequestWithoutOwnerParametersAndDeleteApprovedParticipation() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        String contract = createContract(owner);

        requestParticipation(participant, UUID.randomUUID().toString()).statusCode(404);
        requestParticipation(owner, contract).statusCode(400);
        String participation = requestParticipation(participant, contract).statusCode(201).extract().path("identifier");
        patch(
            owner.contractPath(contract) + "/participations/" + participation + "/status",
            statusChangeRequest("APPROVE", approvalRequest("MANUAL"))
        )
            .statusCode(200)
            .body("status", equalTo("APPROVED"));

        delete(participant.basePath() + "/shared-participations/" + participation).statusCode(204);
        assertThat(
            jdbc.queryForObject(
                "select count(*) from shared_participations where identifier = ?",
                Integer.class,
                participation
            )
        ).isZero();
        get(owner.contractPath(contract)).statusCode(200);
    }

    @Test
    void inactiveContractRetainsParticipationHistoryAndDeletionCascades() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        String contract = createContract(owner);
        String participation = requestParticipation(participant, contract)
            .statusCode(201)
            .body("status", equalTo("PENDING"))
            .extract()
            .path("identifier");

        get(participant.basePath() + "/shared-contracts/available")
            .statusCode(200)
            .body("content.identifier", hasItem(contract));
        get(participant.basePath() + "/shared-contracts/available/" + contract)
            .statusCode(200)
            .body("identifier", equalTo(contract));
        get(owner.basePath() + "/shared-contracts/available")
            .statusCode(200)
            .body("content.identifier", not(hasItem(contract)));
        get(owner.basePath() + "/shared-contracts/available/" + contract).statusCode(404);

        patch(owner.contractPath(contract) + "/inactivate")
            .statusCode(200)
            .body("lifecycle", equalTo("INACTIVE"));
        get(owner.contractPath(contract)).statusCode(200).body("identifier", equalTo(contract));
        get(participant.basePath() + "/shared-contracts/available")
            .statusCode(200)
            .body("content.identifier", not(hasItem(contract)));
        get(participant.basePath() + "/shared-contracts/available/" + contract).statusCode(404);
        get(participant.basePath() + "/shared-participations/" + participation)
            .statusCode(200)
            .body("contract.identifier", equalTo(contract))
            .body("contract.lifecycle", equalTo("INACTIVE"))
            .body("contract.ownerWorkspaceIdentifier", equalTo(owner.workspace()))
            .body("contract.ownerApplicationIdentifier", equalTo(owner.application()));
        get(participant.basePath() + "/shared-participations")
            .statusCode(200)
            .body("content.identifier", hasItem(participation))
            .body("content.contract.lifecycle", hasItem("INACTIVE"));

        patch(owner.contractPath(contract) + "/activate")
            .statusCode(200)
            .body("lifecycle", equalTo("ACTIVE"));
        get(participant.basePath() + "/shared-participations/" + participation)
            .statusCode(200)
            .body("status", equalTo("PENDING"));

        patch(owner.contractPath(contract) + "/inactivate").statusCode(200);
        delete(owner.contractPath(contract)).statusCode(204);
        assertThat(
            jdbc.queryForObject(
                "select count(*) from shared_participations where identifier = ?",
                Integer.class,
                participation
            )
        ).isZero();
        assertThat(
            jdbc.queryForObject("select count(*) from shared_contracts where identifier = ?", Integer.class, contract)
        ).isZero();
    }

    @Test
    void participantFiltersApplyToStatusAndContractWithinTheApplicationScope() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        Scope otherParticipant = createScope("OtherParticipant");
        String firstContract = createContract(owner);
        String secondContract = createContract(owner);
        String pendingParticipation = requestParticipation(participant, firstContract)
            .statusCode(201)
            .extract()
            .path("identifier");
        String approvedParticipation = requestParticipation(participant, secondContract)
            .statusCode(201)
            .extract()
            .path("identifier");
        String otherParticipation = requestParticipation(otherParticipant, firstContract)
            .statusCode(201)
            .extract()
            .path("identifier");
        patch(
            owner.contractPath(secondContract) + "/participations/" + approvedParticipation + "/status",
            statusChangeRequest("APPROVE", approvalRequest("MANUAL"))
        ).statusCode(200);
        String listPath = participant.basePath() + "/shared-participations";
        get(listPath)
            .statusCode(200)
            .body("content.size()", equalTo(2))
            .body("content.identifier", not(hasItem(otherParticipation)))
            .body("content.contract.name", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.notNullValue()));
        authorized()
            .queryParam("status", "APPROVED")
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(approvedParticipation)));
        authorized()
            .queryParam("contractIdentifier", firstContract)
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(pendingParticipation)));
        authorized()
            .queryParam("status", "APPROVED")
            .queryParam("contractIdentifier", firstContract)
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(0));
        authorized().queryParam("status", "UNKNOWN").get(listPath).then().statusCode(400);
        authorized().queryParam("contractIdentifier", " ").get(listPath).then().statusCode(400);
        patch(
            owner.contractPath(firstContract) + "/participations/" + pendingParticipation + "/status",
            Map.of("action", "REJECT")
        ).statusCode(200);
        authorized()
            .queryParam("status", "REJECTED")
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(pendingParticipation)));
    }

    @Test
    void discoveryPagesOnlyActiveContractsWithActiveOwnersAndExcludesTheCaller() {
        Scope owner = createScope("Owner");
        Scope participant = createScope("Participant");
        Scope inactiveApplicationOwner = createScope("InactiveApplication");
        Scope inactiveWorkspaceOwner = createScope("InactiveWorkspace");
        String firstContract = createContract(owner);
        String secondContract = createContract(owner);
        String inactiveContract = createContract(owner);
        createContract(participant);
        String inactiveApplicationContract = createContract(inactiveApplicationOwner);
        String inactiveWorkspaceContract = createContract(inactiveWorkspaceOwner);
        patch(owner.contractPath(inactiveContract) + "/inactivate").statusCode(200);
        jdbc.update(
            "UPDATE applications SET lifecycle_code = 'INACTIVE' WHERE identifier = ?",
            inactiveApplicationOwner.application()
        );
        jdbc.update(
            "UPDATE workspaces SET lifecycle_code = 'INACTIVE' WHERE identifier = ?",
            inactiveWorkspaceOwner.workspace()
        );
        String listPath = participant.basePath() + "/shared-contracts/available";
        get(listPath)
            .statusCode(200)
            .body("page", equalTo(0))
            .body("size", equalTo(20))
            .body("totalElements", equalTo(2))
            .body("totalPages", equalTo(1))
            .body("content.identifier", equalTo(List.of(firstContract, secondContract)));
        authorized()
            .queryParam("page", 0)
            .queryParam("size", 1)
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(firstContract)))
            .body("totalElements", equalTo(2))
            .body("totalPages", equalTo(2));
        authorized()
            .queryParam("page", 1)
            .queryParam("size", 1)
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(secondContract)));
        authorized()
            .queryParam("page", 2)
            .queryParam("size", 1)
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.size()", equalTo(0))
            .body("totalElements", equalTo(2));
        authorized()
            .queryParam("ownerWorkspaceIdentifier", owner.workspace())
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(firstContract, secondContract)))
            .body("totalElements", equalTo(2));
        authorized()
            .queryParam("ownerApplicationIdentifier", owner.application())
            .queryParam("size", 1)
            .get(listPath)
            .then()
            .statusCode(200)
            .body("content.identifier", equalTo(List.of(firstContract)))
            .body("totalElements", equalTo(2));
        authorized()
            .queryParam("ownerWorkspaceIdentifier", owner.workspace())
            .queryParam("ownerApplicationIdentifier", owner.application())
            .get(listPath)
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(2));
        authorized()
            .queryParam("ownerWorkspaceIdentifier", participant.workspace())
            .queryParam("ownerApplicationIdentifier", owner.application())
            .get(listPath)
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(0));
        authorized()
            .queryParam("ownerApplicationIdentifier", UUID.randomUUID().toString())
            .get(listPath)
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(0));
        authorized()
            .queryParam("ownerWorkspaceIdentifier", inactiveWorkspaceOwner.workspace())
            .get(listPath)
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(0));
        authorized()
            .queryParam("ownerApplicationIdentifier", inactiveApplicationOwner.application())
            .get(listPath)
            .then()
            .statusCode(200)
            .body("totalElements", equalTo(0));
        authorized().queryParam("ownerWorkspaceIdentifier", " ").get(listPath).then().statusCode(400);
        authorized().queryParam("ownerApplicationIdentifier", " ").get(listPath).then().statusCode(400);
        requestParticipation(participant, inactiveContract).statusCode(404);
        requestParticipation(participant, inactiveApplicationContract).statusCode(404);
        requestParticipation(participant, inactiveWorkspaceContract).statusCode(404);
        authorized().queryParam("page", -1).get(listPath).then().statusCode(400);
        authorized().queryParam("size", 0).get(listPath).then().statusCode(400);
        authorized().queryParam("size", 101).get(listPath).then().statusCode(400);
        authorized().queryParam("page", "invalid").get(listPath).then().statusCode(400);
        get(participant.basePath() + "/shared-contracts/available/" + inactiveApplicationContract).statusCode(404);
        get(participant.basePath() + "/shared-contracts/available/" + inactiveWorkspaceContract).statusCode(404);
        get(participant.basePath() + "/shared-contracts/available/" + firstContract).statusCode(200);
    }

    @Test
    void authorizationUsesTheWorkspaceAndApplicationFromEachControllerPath() {
        Scope owner = createScope("Owner", "OWNER_SPACE", "OWNER_APP");
        Scope other = createScope("Other", "OTHER_SPACE", "OTHER_APP");
        Scope participant = createScope("Participant", "PART_SPACE", "PART_APP");
        String ownerContract = createContract(owner);
        String otherContract = createContract(other);

        authorization.reset();
        authorization.allow(session ->
            session
                .groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_OWNER_SPACE", "DEV", "DEV", "OWNER_SPACE")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_OWNER_APP", "DEV", "DEV", "A-OWNER_APP")
        );

        get(owner.contractPath(ownerContract)).statusCode(200);
        get(other.contractPath(otherContract)).statusCode(anyOf(is(403), is(404)));
        authorization.verifyCalledWithPolicy("DEV");

        authorization.reset();
        authorization.allow(session ->
            session
                .groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PART_SPACE", "DEV", "DEV", "PART_SPACE")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_PART_APP", "DEV", "DEV", "A-PART_APP")
        );
        get(participant.basePath() + "/shared-participations").statusCode(200);
        get(owner.basePath() + "/shared-participations").statusCode(anyOf(is(403), is(404)));
        authorization.verifyCalledWithPolicy("DEV");
    }

    private void seedCatalogs() {
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle', 3, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend application', 1, true, '{}'), ('FRONTEND', 'Frontend', 'Frontend application', 2, true, '{}'), ('SHARED', 'Shared', 'Shared application', 3, true, '{}')"
        );
    }

    private Scope createScope(String prefix) {
        return createScope(prefix, null, null);
    }

    private Scope createScope(String prefix, String workspaceAuthorizer, String applicationAuthorizer) {
        String workspace = post("/api/v1/workspaces", workspaceRequest(prefix, workspaceAuthorizer))
            .statusCode(201)
            .extract()
            .path("identifier");
        String application = post(
            "/api/v1/workspaces/" + workspace + "/applications",
            applicationRequest(prefix, applicationAuthorizer)
        )
            .statusCode(201)
            .extract()
            .path("identifier");
        return new Scope(workspace, application);
    }

    private String createPlatformFeature(Boolean shareable) {
        String microservice = post(
            "/api/v1/platform/microservices",
            Map.of("code", "service-" + UUID.randomUUID(), "name", "Service " + UUID.randomUUID(), "settings", Map.of())
        )
            .statusCode(201)
            .extract()
            .path("identifier");
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("code", "feature-" + UUID.randomUUID());
        request.put("name", "Feature " + UUID.randomUUID());
        request.put("microserviceIdentifier", microservice);
        request.put("settings", Map.of());
        if (shareable != null) request.put("shareable", shareable);
        return post("/api/v1/platform/features", request).statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> featureUpdate(String identifier, boolean shareable) {
        ExtractableResponse<Response> feature = get("/api/v1/platform/features/" + identifier)
            .statusCode(200)
            .extract();
        return Map.of(
            "name",
            feature.path("name"),
            "microserviceIdentifier",
            feature.path("microserviceIdentifier"),
            "settings",
            Map.of(),
            "shareable",
            shareable
        );
    }

    private String createContract(Scope owner) {
        Integer existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM shared_contracts contract JOIN applications application ON application.id = contract.owner_application_id WHERE application.identifier = ?",
            Integer.class,
            owner.application()
        );
        return createContract(owner, existing == 0 ? sharedFeatureIdentifier : createPlatformFeature(true));
    }

    private String createContract(Scope owner, String featureIdentifier) {
        String identifier = post(
            owner.contractsPath(),
            Map.of("description", "Contrato de teste", "featureIdentifier", featureIdentifier)
        )
            .statusCode(201)
            .extract()
            .path("identifier");
        assertThat(
            jdbc.queryForObject(
                """
                SELECT COUNT(*) FROM shared_contracts contract
                JOIN workspaces workspace ON workspace.id = contract.owner_workspace_id
                JOIN applications application ON application.id = contract.owner_application_id
                    AND application.workspace_id = workspace.id
                WHERE contract.identifier = ? AND workspace.identifier = ? AND application.identifier = ?
                """,
                Integer.class,
                identifier,
                owner.workspace(),
                owner.application()
            )
        ).isEqualTo(1);
        return identifier;
    }

    private void insertMapping(String participation, String source, String destination) {
        insertEnvironment(source);
        insertEnvironment(destination);
        jdbc.update(
            """
            insert into shared_environment_mappings
                (participation_id, source_environment_id, destination_environment_id, created_at, updated_at)
            select id, (select id from environments where identifier = ?), (select id from environments where identifier = ?), current_timestamp(6), current_timestamp(6)
            from shared_participations where identifier = ?
            """,
            source,
            destination,
            participation
        );
    }

    private void insertEnvironment(String identifier) {
        jdbc.update(
            "INSERT IGNORE INTO type_authorizations (code, label, description, sort_order, is_active, settings) VALUES ('DEV', 'Dev', 'Developer', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO environment_types (identifier, code, name, description, root_allowed, workspace_required, lifecycle_code, display_order, created_at, updated_at) VALUES (UUID(), 'DEFAULT', 'Default', 'Default', true, false, 'ACTIVE', 1, NOW(6), NOW(6))"
        );
        jdbc.update(
            """
            INSERT IGNORE INTO environments (identifier, environment_type_id, name, description,
                authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
            SELECT ?, id, ?, 'Shared test environment', 'DEV', '{}', 1, 'ACTIVE', NOW(6), NOW(6)
            FROM environment_types WHERE code = 'DEFAULT'
            """,
            identifier,
            identifier
        );
    }

    private Map<String, Object> statusChangeRequest(String action, Map<String, Object> configuration) {
        Map<String, Object> request = new LinkedHashMap<>(configuration);
        request.put("action", action);
        return request;
    }

    private Map<String, Object> approvalRequest(String publicationMode) {
        return approvalRequest(publicationMode, UUID.randomUUID().toString());
    }

    private Map<String, Object> approvalRequest(String publicationMode, String environmentIdentifier) {
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
                        List.of(environmentIdentifier)
                    )
                )
            )
        );
    }

    private ValidatableResponse requestParticipation(Scope participant, String contract) {
        return authorized()
            .post(participant.basePath() + "/shared-contracts/" + contract + "/participations")
            .then();
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
        request.put("applicationScope", "SHARED");
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
        return given()
            .port(port)
            .header("correlation-id", "shared-api-it")
            .header("Authorization", "Bearer shared-api-it")
            .accept(ContentType.JSON);
    }

    private record Scope(String workspace, String application) {
        String basePath() {
            return "/api/v1/workspaces/" + workspace + "/applications/" + application;
        }

        String contractsPath() {
            return basePath() + "/shared-contracts";
        }

        String contractPath(String contract) {
            return contractsPath() + "/" + contract;
        }
    }
}
