package br.com.portalmanager.platform.workspace.entrypoint.environment;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class EnvironmentApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentFinder environmentFinder;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environment.EnvironmentValidator environmentValidator;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeFinder typeFinder;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.repository.environmenttype.EnvironmentTypeRepository typeRepository;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityCommandService compatibilityCommands;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.usecase.validation.environment.EnvironmentTopologyValidator topologyValidator;

    @Autowired
    private br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository environmentRepository;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        for (String code : new String[] { "ACTIVE", "INACTIVE", "QUARANTINED" })
            jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
                code,
                code,
                code
            );
        for (String code : new String[] { "DEFAULT", "CUSTOM", "SHARD", "CELL" }) {
            boolean rootAllowed = code.equals("DEFAULT") || code.equals("CUSTOM");
            boolean workspaceRequired = !code.equals("DEFAULT");
            jdbc.update(
                """
                INSERT IGNORE INTO environment_types
                  (code, name, description, root_allowed, allows_children, workspace_required,
                   lifecycle_code, display_order, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE', 1, NOW(6), NOW(6))
                """,
                code,
                code,
                code + " environment",
                rootAllowed,
                !code.equals("CELL"),
                workspaceRequired
            );
        }
        jdbc.update(
            """
            INSERT IGNORE INTO environment_type_compatibilities
              (identifier, parent_type_code, child_type_code, lifecycle_code, created_at, updated_at)
            SELECT UUID(), p.code, c.code, 'ACTIVE', NOW(6), NOW(6)
            FROM environment_types p JOIN environment_types c
            WHERE (p.code = 'DEFAULT' AND c.code = 'SHARD')
               OR (p.code = 'CUSTOM' AND c.code = 'SHARD')
               OR (p.code = 'SHARD' AND c.code = 'CELL')
            """
        );
        for (String code : new String[] { "DEV", "TST", "ADM" })
            jdbc.update(
                "INSERT IGNORE INTO type_authorizations (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
                code,
                code,
                code
            );
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Manager', 1, true, '{}')"
        );
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
    }

    @Test
    void keepsDefaultsGlobalAndCustomEnvironmentsScopedToWorkspace() {
        String a = workspace();
        String b = workspace();
        String defaults = "/api/v1/environment-defaults";
        String customA = "/api/v1/workspaces/" + a + "/environments";
        String customB = "/api/v1/workspaces/" + b + "/environments";
        String name = "Shared " + UUID.randomUUID();
        String global = post(defaults, input(name))
            .statusCode(201)
            .body("environmentTypeCode", equalTo("DEFAULT"))
            .body("sortOrder", greaterThanOrEqualTo(1))
            .extract()
            .path("identifier");
        String first = post(customA, withType(name, "CUSTOM"))
            .statusCode(201)
            .body("environmentTypeCode", equalTo("CUSTOM"))
            .body("sortOrder", greaterThanOrEqualTo(4))
            .extract()
            .path("identifier");
        String second = post(customB, withType(name, "CUSTOM")).statusCode(201).extract().path("identifier");
        post(defaults, input(name)).statusCode(400).body("details.field", hasItem("name"));
        post(customA, withType(name, "CUSTOM")).statusCode(400).body("details.field", hasItem("name"));
        get(customA + "/" + second).statusCode(404);
        get(customB + "/" + first).statusCode(404);
        get(defaults + "/" + first).statusCode(404);
        get(defaults + "/" + global).statusCode(200);
        post(customA + "/" + first + "/inactivate", null).statusCode(204);
        get(customA + "/" + first).statusCode(404);
        get(customA + "?lifecycle=INACTIVE")
            .statusCode(200)
            .body("identifier", hasItem(first));
        post(customA + "/" + first + "/restore", null)
            .statusCode(200)
            .body("lifecycle", equalTo("ACTIVE"));
        post(defaults + "/" + global + "/inactivate", null).statusCode(204);
        post(defaults + "/" + global + "/restore", null).statusCode(200);
    }

    @Test
    void createsWorkspaceHierarchyAndKeepsChildrenIsolated() {
        String a = workspace();
        String b = workspace();
        String global = post("/api/v1/environment-defaults", input("Global " + UUID.randomUUID()))
            .statusCode(201)
            .extract()
            .path("identifier");
        String baseA = "/api/v1/workspaces/" + a + "/environments";
        String baseB = "/api/v1/workspaces/" + b + "/environments";
        String shardA = post(baseA, child("SHARD A", "SHARD", global))
            .statusCode(201)
            .body("environmentTypeCode", equalTo("SHARD"))
            .body("parentIdentifier", equalTo(global))
            .extract()
            .path("identifier");
        String shardB = post(baseB, child("SHARD B", "SHARD", global))
            .statusCode(201)
            .extract()
            .path("identifier");
        String cellA = post(baseA, child("CELL A", "CELL", shardA))
            .statusCode(201)
            .extract()
            .path("identifier");
        post(baseA, child("Nested under terminal cell", "CELL", cellA))
            .statusCode(400)
            .body("details.field", hasItem("parentIdentifier"));

        String defaultTypeIdentifier = jdbc.queryForObject(
            "SELECT code FROM environment_types WHERE code = 'DEFAULT'",
            String.class
        );
        Long defaultTypeVersion = jdbc.queryForObject(
            "SELECT version FROM environment_types WHERE code = 'DEFAULT'",
            Long.class
        );
        put(
            "/api/v1/environment-types/" + defaultTypeIdentifier,
            Map.of(
                "version", defaultTypeVersion,
                "code", "DEFAULT",
                "name", "Default",
                "description", "Default environment",
                "rootAllowed", true,
                "allowsChildren", false,
                "workspaceRequired", false,
                "displayOrder", 1
            )
        )
            .statusCode(400)
            .body("details.code", hasItem("ENVIRONMENT-140"));

        get(baseA + "/" + global + "/children")
            .statusCode(200)
            .body("identifier", hasItem(shardA))
            .body("identifier", not(hasItem(shardB)));
        get(baseB + "/" + shardA).statusCode(404);
        post(baseB, child("Cross workspace", "CELL", shardA)).statusCode(404);
        post(baseA, child("Direct cell", "CELL", global)).statusCode(400);
        post(baseA, withType("Root shard", "SHARD")).statusCode(400);
        post("/api/v1/environment-defaults", withType("Global custom", "CUSTOM")).statusCode(404);
        get(baseA + "/tree")
            .statusCode(200)
            .body("environment.identifier", hasItem(global));

        post("/api/v1/environment-defaults/" + global + "/inactivate", null).statusCode(204);
        get(baseA + "/" + shardA).statusCode(404);
        get(baseA + "/" + cellA).statusCode(404);
        get(baseA + "/" + global + "/children").statusCode(404);
    }

    @Test
    void preventsQuarantiningAnInactiveEnvironmentThatHasChildren() {
        String workspace = workspace();
        String base = "/api/v1/workspaces/" + workspace + "/environments";
        String parent = post(base, withType("Parent " + UUID.randomUUID(), "CUSTOM"))
            .statusCode(201)
            .extract()
            .path("identifier");
        String child = post(base, child("Child " + UUID.randomUUID(), "SHARD", parent))
            .statusCode(201)
            .extract()
            .path("identifier");

        post(base + "/" + parent + "/inactivate", null).statusCode(204);
        given()
            .port(port)
            .header("correlation-id", "environment-api-it")
            .header("Authorization", "Bearer environment-api-it")
            .accept(ContentType.JSON)
            .when()
            .delete(base + "/" + parent)
            .then()
            .statusCode(409)
            .body("code", equalTo("ENVIRONMENT-131"));

        post(base + "/" + parent + "/restore", null).statusCode(200);
        get(base + "/" + child).statusCode(200);
    }

    @Test
    void validatesWholeRequestSchemaAndPersistsSettingsAsJson() {
        String defaults = "/api/v1/environment-defaults";
        Map<String, Object> wrongSettingsType = new HashMap<>(input("Invalid settings " + UUID.randomUUID()));
        wrongSettingsType.put("settings", "not-an-object");
        post(defaults, wrongSettingsType)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("settings"));

        Map<String, Object> unknownProperty = new HashMap<>(input("Unknown field " + UUID.randomUUID()));
        unknownProperty.put("unexpected", true);
        post(defaults, unknownProperty).statusCode(400).body("code", equalTo("GLOBAL-0001"));

        Map<String, Object> body = new HashMap<>(input("JSON settings " + UUID.randomUUID()));
        body.put("settings", Map.of("region", Map.of("code", "south-1"), "enabled", true));
        String identifier = post(defaults, body)
            .statusCode(201)
            .body("settings.region.code", equalTo("south-1"))
            .body("settings.enabled", equalTo(true))
            .extract()
            .path("identifier");
        get(defaults + "/" + identifier).body("settings.region.code", equalTo("south-1"));
        org.assertj.core.api.Assertions.assertThat(
            jdbc.queryForObject(
                "SELECT DATA_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=database() AND TABLE_NAME='environments' AND COLUMN_NAME='settings'",
                String.class
            )
        ).isEqualTo("json");
    }

    @Test
    void preventsConcurrentCompatibilityChangesFromCreatingACycle() throws Exception {
        String base = "/api/v1/environment-types";
        prepareWorkspaceTypesForCycle();

        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() ->
                postAfter(start, base + "/compatibilities", "SHARD", "CELL")
            );
            Future<Integer> second = executor.submit(() ->
                postAfter(start, base + "/compatibilities", "CELL", "SHARD")
            );
            start.countDown();

            org.assertj.core.api.Assertions.assertThat(
                (first.get() == 201 && second.get() == 400) || (first.get() == 400 && second.get() == 201)
            ).isTrue();
        }
    }

    @Test
    void serializesDisablingChildrenWithChildCreation() throws Exception {
        String parentIdentifier = post(
            "/api/v1/environment-defaults",
            input("Concurrent parent " + UUID.randomUUID().toString().substring(0, 8))
        )
            .statusCode(201)
            .extract()
            .path("identifier");
        String workspaceIdentifier = workspace();
        String base = "/api/v1/environment-types";
        String defaultTypeIdentifier = jdbc.queryForObject(
            "SELECT code FROM environment_types WHERE code = 'DEFAULT'",
            String.class
        );
        Long defaultTypeVersion = jdbc.queryForObject(
            "SELECT version FROM environment_types WHERE code = 'DEFAULT'",
            Long.class
        );
        Map<String, Object> disableChildren = Map.of(
            "version", defaultTypeVersion,
            "code", "DEFAULT",
            "name", "Default",
            "description", "Default environment",
            "rootAllowed", true,
            "allowsChildren", false,
            "workspaceRequired", false,
            "displayOrder", 1
        );
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> createChild = executor.submit(() -> {
                start.await();
                return post(
                    "/api/v1/workspaces/" + workspaceIdentifier + "/environments",
                    child("Concurrent child", "SHARD", parentIdentifier)
                )
                    .extract()
                    .statusCode();
            });
            Future<Integer> disableType = executor.submit(() -> putAfter(
                start,
                base + "/" + defaultTypeIdentifier,
                disableChildren
            ));
            start.countDown();
            int createChildStatus = createChild.get();
            int disableTypeStatus = disableType.get();
            org.assertj.core.api.Assertions.assertThat(createChildStatus).isEqualTo(201);
            org.assertj.core.api.Assertions.assertThat(disableTypeStatus).isEqualTo(400);
            Boolean allowsChildren = jdbc.queryForObject(
                "SELECT allows_children FROM environment_types WHERE code = 'DEFAULT'",
                Boolean.class
            );
            Integer children = jdbc.queryForObject(
                "SELECT COUNT(*) FROM environments WHERE parent_environment_id = (SELECT id FROM environments WHERE identifier = ?)",
                Integer.class,
                parentIdentifier
            );
            org.assertj.core.api.Assertions.assertThat(allowsChildren == (children == 1)).isTrue();
        }
    }

    @Test
    void serializesRootRuleChangesWithRootCreation() throws Exception {
        String identifier = typeIdentifier("DEFAULT");
        Long version = jdbc.queryForObject("SELECT version FROM environment_types WHERE code='DEFAULT'", Long.class);
        Map<String, Object> update = new HashMap<>(typeInput("DEFAULT", "Default", 1));
        update.put("version", version);
        update.put("workspaceRequired", false);
        update.put("rootAllowed", false);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> create = executor.submit(() -> {
                start.await();
                return post("/api/v1/environment-defaults", input("Concurrent root")).extract().statusCode();
            });
            Future<Integer> change = executor.submit(() -> putAfter(start, "/api/v1/environment-types/" + identifier, update));
            start.countDown();
            int createStatus = create.get();
            int changeStatus = change.get();
            org.assertj.core.api.Assertions.assertThat(createStatus).isEqualTo(201);
            org.assertj.core.api.Assertions.assertThat(changeStatus).isEqualTo(400);
            Integer invalidRoots = jdbc.queryForObject(
                "SELECT COUNT(*) FROM environments e JOIN environment_types t ON t.code=e.environment_type_code WHERE e.parent_environment_id IS NULL AND t.root_allowed=false",
                Integer.class
            );
            org.assertj.core.api.Assertions.assertThat(invalidRoots).isZero();
        }
    }

    @Test
    void validatesTypeVersionIdentityAndAuthorization() {
        String base = "/api/v1/environment-types";
        String identifier = typeIdentifier("CELL");
        Map<String, Object> request = new HashMap<>(typeInput("CELL", "Cell", 4));

        put(base + "/" + identifier, request).statusCode(400).body("details.field", hasItem("version"));
        request.put("version", 9999);
        put(base + "/" + identifier, request).statusCode(409);
        request.put("version", jdbc.queryForObject("SELECT version FROM environment_types WHERE code='CELL'", Long.class));
        request.put("code", "SHARD");
        put(base + "/" + identifier, request).statusCode(400).body("details.field", hasItem("code"));
        authorization.reset();
        authorization.forbidden();
        put(base + "/" + identifier, request).statusCode(403);
        authorization.verifyCalledWithPolicy("OWNER");
    }

    @Test
    void rejectsClientVersionOnTypeCreationAndLetsHibernateInitializeTheVersion() {
        jdbc.update("DELETE FROM environment_type_compatibilities WHERE child_type_code=(SELECT code FROM environment_types WHERE code='CELL') OR parent_type_code=(SELECT code FROM environment_types WHERE code='CELL')");
        jdbc.update("DELETE FROM environment_types WHERE code='CELL'");
        Map<String, Object> request = new HashMap<>(typeInput("CELL", "Cell", 4));
        request.put("version", 999);
        post("/api/v1/environment-types", request).statusCode(400)
            .body("details.field", hasItem("version"))
            .body("details.code", hasItem("ENVIRONMENT-134"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM environment_types WHERE code='CELL'", Integer.class
        )).isZero();
        request.remove("version");
        String identifier = post("/api/v1/environment-types", request).statusCode(201)
            .body("version", equalTo(0)).extract().path("code");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT version FROM environment_types WHERE code=?", Long.class, identifier
        )).isZero();
        request.put("version", 0);
        put("/api/v1/environment-types/" + identifier, request).statusCode(200)
            .body("version", equalTo(1));
    }

    @Test
    void keepsTypeCodeImmutableAndCompatibilityReferencesStable() {
        String code = "CELL";
        Long version = jdbc.queryForObject("SELECT version FROM environment_types WHERE code=?", Long.class, code);
        Map<String, Object> request = new HashMap<>(typeInput(code, "Cell", 4));
        request.put("version", version);
        put("/api/v1/environment-types/" + code, request).statusCode(200).body("code", equalTo(code));
        request.put("code", "RENAMED_CELL");
        put("/api/v1/environment-types/" + code, request).statusCode(400).body("details.field", hasItem("code"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM environment_type_compatibilities WHERE child_type_code=? OR parent_type_code=?", Integer.class, code, code
        )).isPositive();
    }

    @Test
    void managesTypesWithWorkspaceRequirementAsData() {
        String base = "/api/v1/environment-types";
        post(base, Map.of())
            .statusCode(400)
            .body(
                "details.field",
                hasItems("code", "name", "description", "rootAllowed", "allowsChildren", "workspaceRequired", "displayOrder")
            );
        post(base + "/compatibilities", Map.of())
            .statusCode(400)
            .body("details.field", hasItems("parentTypeCode", "childTypeCode"));
        post(
            base + "/compatibilities",
            Map.of("parentTypeCode", "SHARD", "childTypeCode", "CELL")
        )
            .statusCode(201)
            .body("parentTypeCode", equalTo("SHARD"))
            .body("childTypeCode", equalTo("CELL"));
        post(base, typeInput("REGION", "Region", 5))
            .statusCode(400)
            .body("details.field", hasItem("code"));
        get(base)
            .statusCode(200)
            .body("code", hasItems("DEFAULT", "CUSTOM", "SHARD", "CELL"));
        String shardIdentifier = jdbc.queryForObject(
            "SELECT code FROM environment_types WHERE code = 'SHARD'",
            String.class
        );
        Long shardVersion = jdbc.queryForObject(
            "SELECT version FROM environment_types WHERE code = 'SHARD'",
            Long.class
        );
        Map<String, Object> shardUpdate = new HashMap<>(Map.of(
                "version", shardVersion,
                "code", "SHARD",
                "name", "Shard Nodes",
                "description", "Updated shard environment type",
                "rootAllowed", false,
                "allowsChildren", true,
                "workspaceRequired", true,
                "displayOrder", 3
            ));
        put(base + "/" + shardIdentifier, shardUpdate)
            .statusCode(200)
            .body("name", equalTo("Shard Nodes"))
            .body("allowsChildren", equalTo(true));
        String workspace = workspace();
        post("/api/v1/workspaces/" + workspace + "/environments", withType("Root shard", "SHARD")).statusCode(400);
        String shardTypeIdentifier = typeIdentifier("SHARD");
        String cellTypeIdentifier = typeIdentifier("CELL");
        String compatibilityIdentifier = post(
            base + "/compatibilities",
            Map.of("parentTypeCode", shardTypeIdentifier, "childTypeCode", cellTypeIdentifier)
        )
            .statusCode(201)
            .body("childTypeCode", equalTo(cellTypeIdentifier))
            .extract().path("identifier");
        get(base + "/compatibilities/" + compatibilityIdentifier)
            .statusCode(200)
            .body("identifier", equalTo(compatibilityIdentifier))
            .body("parentTypeCode", equalTo(shardTypeIdentifier))
            .body("childTypeCode", equalTo(cellTypeIdentifier));
        get(base + "/compatibilities/missing-compatibility").statusCode(404);
        get(base + "/compatibilities")
            .statusCode(200)
            .body("childTypeCode", hasItem(cellTypeIdentifier));
        post(
            base + "/compatibilities",
            Map.of("parentTypeCode", cellTypeIdentifier, "childTypeCode", shardTypeIdentifier)
        )
            .statusCode(400)
            .body("details.field", hasItem("parentTypeCode"));
    }

    @Test
    void rejectsDuplicateTypeCodesIncludingInactiveTypes() {
        Map<String, Object> request = typeInput("CELL", "Cell", 4);
        post("/api/v1/environment-types", request).statusCode(400)
            .body("details.field", hasItem("code"));
        jdbc.update("UPDATE environment_types SET lifecycle_code='INACTIVE' WHERE code='CELL'");
        post("/api/v1/environment-types", request).statusCode(400)
            .body("details.field", hasItem("code"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM environment_types WHERE code='CELL'", Integer.class
        )).isEqualTo(1);
    }

    @Test
    void concurrentTypeCreationKeepsOneCodeAndReturnsASafeError() throws Exception {
        jdbc.update("DELETE FROM environment_type_compatibilities WHERE child_type_code=(SELECT code FROM environment_types WHERE code='CELL') OR parent_type_code=(SELECT code FROM environment_types WHERE code='CELL')");
        jdbc.update("DELETE FROM environment_types WHERE code='CELL'");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<io.restassured.response.Response> create = () -> {
                start.await();
                return post("/api/v1/environment-types", typeInput("CELL", "Cell", 4)).extract().response();
            };
            Future<io.restassured.response.Response> first = executor.submit(create);
            Future<io.restassured.response.Response> second = executor.submit(create);
            start.countDown();
            io.restassured.response.Response firstResponse = first.get(30, java.util.concurrent.TimeUnit.SECONDS);
            io.restassured.response.Response secondResponse = second.get(30, java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(firstResponse.statusCode(), secondResponse.statusCode()))
                .contains(201).filteredOn(status -> status != 201).singleElement().isIn(400, 409);
            io.restassured.response.Response rejected = firstResponse.statusCode() == 201 ? secondResponse : firstResponse;
            org.assertj.core.api.Assertions.assertThat(rejected.asString().toLowerCase(java.util.Locale.ROOT))
                .doesNotContain("insert into", "sqlstate", "constraint", "duplicate entry", "environment_types");
            org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM environment_types WHERE code='CELL'", Integer.class
            )).isEqualTo(1);
        }
    }

    @Test
    void createsRootsWhileTheSameTypeIsReadLockedAndAnUnrelatedTypeIsWriteLocked() throws Exception {
        CountDownLatch locksAcquired = new CountDownLatch(1);
        CountDownLatch releaseLocks = new CountDownLatch(1);
        org.springframework.transaction.support.TransactionTemplate transaction =
            new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.setIsolationLevel(org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<?> holder = executor.submit(() -> transaction.executeWithoutResult(status -> {
                typeFinder.findForEnvironmentCreation(typeIdentifier("DEFAULT"), false, null);
                typeRepository.findByCodeForUpdate(typeIdentifier("CELL")).orElseThrow();
                locksAcquired.countDown();
                try {
                    if (!releaseLocks.await(20, java.util.concurrent.TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting for test to release locks");
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }));
            try {
                org.assertj.core.api.Assertions.assertThat(locksAcquired.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                Future<Integer> create = executor.submit(() ->
                    post("/api/v1/environment-defaults", input("Parallel root")).extract().statusCode()
                );
                org.assertj.core.api.Assertions.assertThat(create.get(10, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(201);
            } finally {
                releaseLocks.countDown();
            }
            holder.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
    }

    @Test
    void inactiveTypesPreserveExistingLinksAndOnlyBlockNewLinks() {
        String defaults = "/api/v1/environment-defaults";
        String root = post(defaults, input("Existing default")).statusCode(201).extract().path("identifier");
        String base = "/api/v1/workspaces/" + workspace() + "/environments";
        String shard = post(base, child("Existing shard", "SHARD", root)).statusCode(201).extract().path("identifier");
        String cell = post(base, child("Existing cell", "CELL", shard)).statusCode(201).extract().path("identifier");
        String defaultType = typeIdentifier("DEFAULT");
        String cellType = typeIdentifier("CELL");
        Integer compatibilityCount = jdbc.queryForObject("SELECT COUNT(*) FROM environment_type_compatibilities WHERE lifecycle_code='ACTIVE'", Integer.class);
        post("/api/v1/environment-types/" + defaultType + "/inactivate", null).statusCode(204);
        post("/api/v1/environment-types/" + cellType + "/inactivate", null).statusCode(204);
        get(defaults + "/" + root).statusCode(200);
        get(base + "/" + cell).statusCode(200);
        post(defaults, input("New default")).statusCode(404);
        post(base, child("New shard under inactive parent type", "SHARD", root)).statusCode(404);
        post(base, child("New inactive cell", "CELL", shard)).statusCode(404);
        given().port(port).header("correlation-id", "environment-api-it")
            .header("Authorization", "Bearer environment-api-it").accept(ContentType.JSON)
            .delete("/api/v1/environment-types/" + cellType).then().statusCode(400);
        post("/api/v1/environment-types/compatibilities", Map.of(
            "parentTypeCode", typeIdentifier("SHARD"), "childTypeCode", cellType
        )).statusCode(400);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT COUNT(*) FROM environment_type_compatibilities WHERE lifecycle_code='ACTIVE'", Integer.class
        )).isEqualTo(compatibilityCount);
        Map<String, Object> update = new HashMap<>(withType("Edited existing cell", "CELL"));
        update.put("version", get(base + "/" + cell).extract().path("version"));
        put(base + "/" + cell, update).statusCode(200).body("environmentTypeCode", equalTo("CELL"));
        Map<String, Object> parentUpdate = new HashMap<>(withType("Edited existing shard", "SHARD"));
        parentUpdate.put("version", get(base + "/" + shard).extract().path("version"));
        put(base + "/" + shard, parentUpdate).statusCode(200);
        post(base + "/" + cell + "/inactivate", null).statusCode(204);
        post(base + "/" + cell + "/restore", null).statusCode(200).body("environmentTypeCode", equalTo("CELL"));
        String other = post(base, withType("Other root", "CUSTOM")).statusCode(201).extract().path("identifier");
        Map<String, Object> switchType = new HashMap<>(withType("Other root", "CELL"));
        switchType.put("version", get(base + "/" + other).extract().path("version"));
        put(base + "/" + other, switchType).statusCode(404);
        post("/api/v1/environment-types/" + defaultType + "/restore", null).statusCode(200);
        post("/api/v1/environment-types/" + cellType + "/restore", null).statusCode(200);
        post(base, child("New active cell", "CELL", shard)).statusCode(201);
    }

    @Test
    void serializesTypeInactivationWithNewLinksAndRejectsLaterLinks() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        String identifier = typeIdentifier("DEFAULT");
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> create = executor.submit(() -> {
                start.await();
                return post("/api/v1/environment-defaults", input("Root racing inactivation")).extract().statusCode();
            });
            Future<Integer> inactivate = executor.submit(() -> {
                start.await();
                return post("/api/v1/environment-types/" + identifier + "/inactivate", null).extract().statusCode();
            });
            start.countDown();
            org.assertj.core.api.Assertions.assertThat(create.get(30, java.util.concurrent.TimeUnit.SECONDS)).isIn(201, 404);
            org.assertj.core.api.Assertions.assertThat(inactivate.get(30, java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(204);
        }
        get("/api/v1/environment-types/" + identifier).statusCode(200).body("lifecycle", equalTo("INACTIVE"));
        post("/api/v1/environment-defaults", input("Root after inactivation")).statusCode(404);
    }

    @Test
    void inactiveCompatibilitiesPreserveExistingEnvironmentsAndBlockNewLinks() {
        String root = post("/api/v1/environment-defaults", input("Compatibility root")).statusCode(201).extract().path("identifier");
        String environments = "/api/v1/workspaces/" + workspace() + "/environments";
        String shard = post(environments, child("Existing compatibility shard", "SHARD", root)).statusCode(201).extract().path("identifier");
        String compatibility = compatibilityIdentifier("DEFAULT", "SHARD");
        String compatibilities = "/api/v1/environment-types/compatibilities";
        post(compatibilities + "/" + compatibility + "/inactivate", null).statusCode(204);
        post(environments, child("New forbidden shard", "SHARD", root)).statusCode(400);
        Map<String, Object> update = new HashMap<>(withType("Edited existing shard", "SHARD"));
        update.put("version", get(environments + "/" + shard).extract().path("version"));
        put(environments + "/" + shard, update).statusCode(200);
        post(environments + "/" + shard + "/inactivate", null).statusCode(204);
        post(environments + "/" + shard + "/restore", null).statusCode(200);
        requestDelete(compatibilities + "/" + compatibility).statusCode(400);
        post(compatibilities, Map.of("parentTypeCode", typeIdentifier("DEFAULT"), "childTypeCode", typeIdentifier("SHARD")))
            .statusCode(201).body("identifier", equalTo(compatibility));
        post(environments, child("New allowed shard", "SHARD", root)).statusCode(201);
    }

    @Test
    void restoresTheSameCompatibilityAndRejectsSelfLinksAndReactivationCycles() {
        String compatibilities = "/api/v1/environment-types/compatibilities";
        prepareWorkspaceTypesForCycle();
        Map<String, Object> pair = Map.of("parentTypeCode", "SHARD", "childTypeCode", "CELL");
        String identifier = post(compatibilities, pair).statusCode(201).extract().path("identifier");
        post(compatibilities, pair).statusCode(201).body("identifier", equalTo(identifier));
        post(compatibilities + "/" + identifier + "/inactivate", null).statusCode(204);
        post(compatibilities + "/" + identifier + "/inactivate", null).statusCode(400);
        post(compatibilities, Map.of("parentTypeCode", "CELL", "childTypeCode", "SHARD")).statusCode(201);
        post(compatibilities, pair).statusCode(400).body("details.field", hasItem("childTypeCode"));
        post(compatibilities, Map.of("parentTypeCode", typeIdentifier("CELL"), "childTypeCode", typeIdentifier("CELL"))).statusCode(400);
        requestDelete(compatibilities + "/" + identifier).statusCode(204);
        get(compatibilities + "/" + identifier).statusCode(404);
    }

    @Test
    void validatesCyclesAndTypeActivityAgainstCurrentDataDespiteAnEarlierSnapshot() {
        prepareWorkspaceTypesForCycle();
        String parentType = "SHARD";
        String childType = "CELL";
        org.springframework.transaction.support.TransactionTemplate transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            typeRepository.findAll();
            jdbc.queryForObject("SELECT COUNT(*) FROM environment_type_compatibilities", Integer.class);
            post("/api/v1/environment-types/compatibilities", Map.of("parentTypeCode", childType, "childTypeCode", parentType)).statusCode(201);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> compatibilityCommands.create(parentType, childType))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.ValidationException.class);
            status.setRollbackOnly();
        });
        transaction.executeWithoutResult(status -> {
            typeRepository.findAll();
            post("/api/v1/environment-types/" + childType + "/inactivate", null).statusCode(204);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> compatibilityCommands.create(parentType, childType))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.ValidationException.class);
            status.setRollbackOnly();
        });
    }

    @Test
    void validatesNewEnvironmentLinksAgainstCurrentCompatibilityState() {
        String root = post("/api/v1/environment-defaults", input("Snapshot compatibility root")).statusCode(201).extract().path("identifier");
        String workspaceIdentifier = workspace();
        org.springframework.transaction.support.TransactionTemplate transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT COUNT(*) FROM environment_type_compatibilities", Integer.class);
            post("/api/v1/environment-types/compatibilities/" + compatibilityIdentifier("DEFAULT", "SHARD") + "/inactivate", null).statusCode(204);
            br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment parent = environmentRepository.findByIdentifier(root).orElseThrow();
            br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType childType = typeFinder.findForEnvironmentCreation(typeIdentifier("SHARD"), true, parent.getEnvironmentType().getCode());
            Long workspaceId = jdbc.queryForObject("SELECT id FROM workspaces WHERE identifier=?", Long.class, workspaceIdentifier);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> topologyValidator.validateForNewLink(childType, workspaceId, parent))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.ValidationException.class);
            status.setRollbackOnly();
        });
    }

    @Test
    void validatesEnvironmentVersionAndKeepsParentImmutable() {
        String base = "/api/v1/workspaces/" + workspace() + "/environments";
        Map<String, Object> create = new HashMap<>(withType("Versioned environment", "CUSTOM"));
        create.put("version", 9);
        post(base, create).statusCode(400).body("details.field", hasItem("version"));
        create.remove("version");
        String identifier = post(base, create).statusCode(201).body("version", equalTo(0)).extract().path("identifier");
        put(base + "/" + identifier, create).statusCode(400).body("details.field", hasItem("version"));
        create.put("version", 99);
        put(base + "/" + identifier, create).statusCode(409);
        create.put("version", 0);
        create.put("parentIdentifier", identifier);
        put(base + "/" + identifier, create).statusCode(400).body("details.field", hasItem("parentIdentifier"));
        create.remove("parentIdentifier");
        put(base + "/" + identifier, create).statusCode(200).body("version", equalTo(1));
        create.put("name", "Stale environment update");
        put(base + "/" + identifier, create).statusCode(409);
        get(base + "/" + identifier).body("name", equalTo("Versioned environment"));
    }

    @Test
    void keepsQuarantinedEnvironmentsAndTheirNamesReserved() {
        String base = "/api/v1/workspaces/" + workspace() + "/environments";
        String identifier = post(base, withType("Reserved environment name", "CUSTOM")).statusCode(201).extract().path("identifier");
        requestDelete(base + "/" + identifier).statusCode(400);
        post(base + "/" + identifier + "/inactivate", null).statusCode(204);
        requestDelete(base + "/" + identifier).statusCode(204);
        get(base + "?lifecycle=QUARANTINED").statusCode(200).body("identifier", hasItem(identifier));
        post(base + "/" + identifier + "/restore", null).statusCode(400);
        requestDelete(base + "/" + identifier).statusCode(400);
        post(base, withType("Reserved environment name", "CUSTOM")).statusCode(400).body("details.field", hasItem("name"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT lifecycle_code FROM environments WHERE identifier=?", String.class, identifier))
            .isEqualTo("QUARANTINED");
    }

    @Test
    void rejectsAutomaticSortOrderOverflowWithoutPersistingAnInvalidRow() {
        Map<String, Object> maximum = new HashMap<>(input("Last possible sort order"));
        maximum.put("sortOrder", Integer.MAX_VALUE);
        post("/api/v1/environment-defaults", maximum).statusCode(201);
        post("/api/v1/environment-defaults", input("Automatic order overflow")).statusCode(400)
            .body("code", equalTo("ENVIRONMENT-108"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM environments", Integer.class)).isEqualTo(1);
    }

    @Test
    void serializesParentTypeChangesWithChildCreation() throws Exception {
        String workspace = workspace();
        String base = "/api/v1/workspaces/" + workspace + "/environments";
        String global = post("/api/v1/environment-defaults", input("Global for type change"))
            .statusCode(201).extract().path("identifier");
        post("/api/v1/environment-types/compatibilities", Map.of("parentTypeCode", "DEFAULT", "childTypeCode", "CELL"))
            .statusCode(201);
        String parent = post(base, child("Parent changing type", "SHARD", global)).statusCode(201).extract().path("identifier");
        Map<String, Object> update = new HashMap<>(withType("Parent changing type", "CELL"));
        update.put("version", 0);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> change = executor.submit(() -> { start.await(); return put(base + "/" + parent, update).extract().statusCode(); });
            Future<Integer> create = executor.submit(() -> { start.await(); return post(base, child("Concurrent new cell", "CELL", parent)).extract().statusCode(); });
            start.countDown();
            int changeStatus = change.get(30, java.util.concurrent.TimeUnit.SECONDS);
            int createStatus = create.get(30, java.util.concurrent.TimeUnit.SECONDS);
            org.assertj.core.api.Assertions.assertThat(changeStatus).isIn(200, 400);
            org.assertj.core.api.Assertions.assertThat(createStatus).isIn(201, 400);
            org.assertj.core.api.Assertions.assertThat(changeStatus == 200 && createStatus == 201).isFalse();
        }
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM environments child JOIN environments parent ON parent.id=child.parent_environment_id JOIN environment_types type ON type.code=parent.environment_type_code WHERE type.code='CELL'", Integer.class)).isZero();
    }

    @Test
    void serializesRepeatedRestorationAgainstTheCurrentLifecycle() throws Exception {
        String base = "/api/v1/environment-defaults";
        String identifier = post(base, input("Concurrent restore")).statusCode(201).extract().path("identifier");
        post(base + "/" + identifier + "/inactivate", null).statusCode(204);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> restore = () -> { start.await(); return post(base + "/" + identifier + "/restore", null).extract().statusCode(); };
            Future<Integer> first = executor.submit(restore);
            Future<Integer> second = executor.submit(restore);
            start.countDown();
            org.assertj.core.api.Assertions.assertThat(java.util.List.of(first.get(30, java.util.concurrent.TimeUnit.SECONDS), second.get(30, java.util.concurrent.TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(200, 400);
        }
        get(base + "/" + identifier).statusCode(200).body("version", equalTo(2));
    }

    @Test
    void reloadsManagedEnvironmentsBeforeLockingAndChecksCurrentAncestors() {
        String workspace = workspace();
        String base = "/api/v1/workspaces/" + workspace + "/environments";
        String parent = post(base, withType("Ancestor current state", "CUSTOM")).statusCode(201).extract().path("identifier");
        String child = post(base, child("Descendant current state", "SHARD", parent)).statusCode(201).extract().path("identifier");
        Long workspaceId = jdbc.queryForObject("SELECT id FROM workspaces WHERE identifier=?", Long.class, workspace);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            environmentRepository.findByIdentifier(parent).orElseThrow();
            environmentRepository.findByIdentifier(child).orElseThrow();
            post(base + "/" + parent + "/inactivate", null).statusCode(204);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> environmentFinder.findActiveForUpdate(parent, workspaceId))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.NotFoundException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> environmentFinder.findActiveForUpdate(child, workspaceId))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.NotFoundException.class);
            status.setRollbackOnly();
        });
    }

    @Test
    void checksChildrenUsingCurrentDataDespiteAnEarlierTransactionSnapshot() {
        String workspace = workspace();
        String base = "/api/v1/workspaces/" + workspace + "/environments";
        String parent = post(base, withType("Parent with later child", "CUSTOM")).statusCode(201).extract().path("identifier");
        Long workspaceId = jdbc.queryForObject("SELECT id FROM workspaces WHERE identifier=?", Long.class, workspace);
        new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            environmentRepository.findByIdentifier(parent).orElseThrow();
            jdbc.queryForObject("SELECT COUNT(*) FROM environments WHERE parent_environment_id=(SELECT id FROM environments WHERE identifier=?)", Integer.class, parent);
            post(base, child("Child committed after snapshot", "SHARD", parent)).statusCode(201);
            post(base + "/" + parent + "/inactivate", null).statusCode(204);
            br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment current = environmentFinder.findInactiveForDeletionForUpdate(parent, workspaceId);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> environmentValidator.validateForDelete(current))
                .isInstanceOf(br.com.portalmanager.platform.library.messaging.exception.ValidationException.class)
                .hasMessage(br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys.HAS_CHILDREN);
            status.setRollbackOnly();
        });
    }

    @Test
    void appliesWorkspaceAndEnvironmentVisibilityToHierarchyAndWrites() {
        String workspace = workspace();
        jdbc.update("UPDATE workspaces SET authorizer_group='TEAM_A' WHERE identifier=?", workspace);
        String base = "/api/v1/workspaces/" + workspace + "/environments";
        Map<String, Object> visibleInput = new HashMap<>(withType("Visible parent", "CUSTOM"));
        visibleInput.put("authorizerGroup", "TEAM_A");
        String visible = post(base, visibleInput).statusCode(201).extract().path("identifier");
        Map<String, Object> hiddenInput = new HashMap<>(withType("Hidden parent", "CUSTOM"));
        hiddenInput.put("authorizerGroup", "TEAM_B");
        String hidden = post(base, hiddenInput).statusCode(201).extract().path("identifier");
        Map<String, Object> orphanInput = new HashMap<>(child("Visible child of hidden parent", "SHARD", hidden));
        orphanInput.put("authorizerGroup", "TEAM_A");
        String orphan = post(base, orphanInput).statusCode(201).extract().path("identifier");
        Map<String, Object> inactiveChildInput = new HashMap<>(child("Inactive child of hidden parent", "SHARD", hidden));
        inactiveChildInput.put("authorizerGroup", "TEAM_A");
        String inactiveChild = post(base, inactiveChildInput).statusCode(201).extract().path("identifier");
        post(base + "/" + inactiveChild + "/inactivate", null).statusCode(204);
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
            .addAuthorizerGroup("WORKSPACE_TEAM_A", "ADM", "ADM", "TEAM_A")
            .addAuthorizerGroup("ENVIRONMENT_TEAM_A", "ADM", "ADM", "E-TEAM_A"));
        get(base).statusCode(200).body("identifier", contains(visible));
        get(base + "/roots").statusCode(200).body("identifier", contains(visible));
        get(base + "/tree").statusCode(200).body("environment.identifier", contains(visible));
        get(base + "/" + hidden).statusCode(404);
        get(base + "/" + orphan).statusCode(404);
        get(base + "?lifecycle=INACTIVE").statusCode(200).body("identifier", not(hasItem(inactiveChild)));
        post(base + "/" + inactiveChild + "/restore", null).statusCode(404);
        requestDelete(base + "/" + inactiveChild).statusCode(404);
        Map<String, Object> newChild = new HashMap<>(child("Unauthorized child creation", "SHARD", hidden));
        newChild.put("authorizerGroup", "TEAM_A");
        post(base, newChild).statusCode(404);
        post(base + "/" + hidden + "/inactivate", null).statusCode(404);
        Map<String, Object> update = new HashMap<>(visibleInput);
        update.put("version", 0);
        put(base + "/" + visible, update).statusCode(200);
        jdbc.update("UPDATE workspaces SET authorizer_group='TEAM_B' WHERE identifier=?", workspace);
        get(base).statusCode(404);
        get(base + "/tree").statusCode(404);
        post(base, newChild).statusCode(404);
    }

    @Test
    void protectsChildPermissionAgainstActiveCompatibilitiesAndAllowsIncomingLinks() {
        String types = "/api/v1/environment-types";
        String compatibilities = types + "/compatibilities";
        String shardCell = compatibilityIdentifier("SHARD", "CELL");
        Map<String, Object> shard = new HashMap<>(typeInput("SHARD", "Shard", 3));
        shard.put("version", get(types + "/SHARD").extract().path("version"));
        shard.put("allowsChildren", false);
        put(types + "/SHARD", shard).statusCode(400)
            .body("details.code", hasItem("ENVIRONMENT-144"));

        Map<String, Object> cell = new HashMap<>(typeInput("CELL", "Cell leaf", 4));
        cell.put("version", get(types + "/CELL").extract().path("version"));
        cell.put("allowsChildren", false);
        put(types + "/CELL", cell).statusCode(200).body("allowsChildren", equalTo(false));

        post(compatibilities + "/" + shardCell + "/inactivate", null).statusCode(204);
        put(types + "/SHARD", shard).statusCode(200).body("allowsChildren", equalTo(false));
        Map<String, Object> pair = Map.of("parentTypeCode", "SHARD", "childTypeCode", "CELL");
        post(compatibilities, pair).statusCode(400)
            .body("details.code", hasItem("ENVIRONMENT-141"));
        shard.put("version", get(types + "/SHARD").extract().path("version"));
        shard.put("allowsChildren", true);
        put(types + "/SHARD", shard).statusCode(200);
        post(compatibilities, pair).statusCode(201).body("identifier", equalTo(shardCell));
    }

    @Test
    void rejectsGlobalChildrenOfWorkspaceTypesAndInvalidBuiltinScopeFlags() {
        String types = "/api/v1/environment-types";
        for (String parent : new String[] { "CUSTOM", "SHARD" }) {
            post(types + "/compatibilities", Map.of("parentTypeCode", parent, "childTypeCode", "DEFAULT"))
                .statusCode(400).body("details.code", hasItem("ENVIRONMENT-143"));
        }
        for (String code : new String[] { "DEFAULT", "CUSTOM", "SHARD", "CELL" }) {
            Map<String, Object> update = new HashMap<>(typeInput(code, code, 1));
            update.put("version", get(types + "/" + code).extract().path("version"));
            update.put("workspaceRequired", code.equals("DEFAULT"));
            put(types + "/" + code, update).statusCode(400)
                .body("details.code", hasItem("ENVIRONMENT-115"));
        }
    }

    @Test
    void returnsLocalizedErrorsForCompatibilityPurgeAndPurgesAnUnusedPair() {
        String compatibilities = "/api/v1/environment-types/compatibilities";
        String identifier = post(compatibilities, Map.of("parentTypeCode", "DEFAULT", "childTypeCode", "CUSTOM"))
            .statusCode(201).extract().path("identifier");
        requestDelete(compatibilities + "/" + identifier).statusCode(400)
            .body("details.code", hasItem("ENVIRONMENT-145"))
            .body("details.message", hasItem(containsString("compatibilidade inativa")));
        post(compatibilities + "/" + identifier + "/inactivate", null).statusCode(204);
        requestDelete(compatibilities + "/" + identifier).statusCode(204);
        get(compatibilities + "/" + identifier).statusCode(404);
    }

    @Test
    void quarantinedChildrenKeepTheirParentTypeAndCompatibilityReferences() {
        String environments = "/api/v1/workspaces/" + workspace() + "/environments";
        String parent = post(environments, withType("Parent retaining history", "CUSTOM"))
            .statusCode(201).extract().path("identifier");
        String child = post(environments, child("Quarantined child", "SHARD", parent))
            .statusCode(201).extract().path("identifier");
        post(environments + "/" + child + "/inactivate", null).statusCode(204);
        requestDelete(environments + "/" + child).statusCode(204);
        String compatibilities = "/api/v1/environment-types/compatibilities";
        String link = compatibilityIdentifier("CUSTOM", "SHARD");
        post(compatibilities + "/" + link + "/inactivate", null).statusCode(204);
        requestDelete(compatibilities + "/" + link).statusCode(400)
            .body("details.code", hasItem("ENVIRONMENT-146"))
            .body("details.message", hasItem(containsString("ambientes usando")));
        post(environments + "/" + parent + "/inactivate", null).statusCode(204);
        requestDelete(environments + "/" + parent).statusCode(409).body("code", equalTo("ENVIRONMENT-131"));
        post("/api/v1/environment-types/SHARD/inactivate", null).statusCode(204);
        requestDelete("/api/v1/environment-types/SHARD").statusCode(400)
            .body("details.code", hasItem("ENVIRONMENT-114"));
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
            "SELECT parent_environment_id FROM environments WHERE identifier=?", Long.class, child
        )).isEqualTo(jdbc.queryForObject("SELECT id FROM environments WHERE identifier=?", Long.class, parent));
        get(environments + "?lifecycle=QUARANTINED").statusCode(200).body("identifier", hasItem(child));
    }

    private String compatibilityIdentifier(String parentCode, String childCode) {
        return jdbc.queryForObject("SELECT compatibility.identifier FROM environment_type_compatibilities compatibility JOIN environment_types parent ON parent.code=compatibility.parent_type_code JOIN environment_types child ON child.code=compatibility.child_type_code WHERE parent.code=? AND child.code=?", String.class, parentCode, childCode);
    }

    private void prepareWorkspaceTypesForCycle() {
        String compatibilities = "/api/v1/environment-types/compatibilities";
        String shardCell = compatibilityIdentifier("SHARD", "CELL");
        post(compatibilities + "/" + shardCell + "/inactivate", null).statusCode(204);
        requestDelete(compatibilities + "/" + shardCell).statusCode(204);
        Map<String, Object> cell = new HashMap<>(typeInput("CELL", "Cell", 4));
        cell.put("version", get("/api/v1/environment-types/CELL").extract().path("version"));
        put("/api/v1/environment-types/CELL", cell).statusCode(200);
    }

    private ValidatableResponse requestDelete(String path) {
        return given().port(port).header("correlation-id", "environment-api-it").header("Authorization", "Bearer environment-api-it")
            .delete(path).then();
    }

    private String workspace() {
        return post(
            "/api/v1/workspaces",
            Map.of(
                "workspaceType",
                "MANAGER",
                "name",
                "Environment Workspace " + UUID.randomUUID().toString().substring(0, 8),
                "description",
                "Workspace for environments",
                "requester",
                "requester",
                "acronym",
                "ENV",
                "settings",
                Map.of(),
                "emailGroup",
                "workspace@portalmanager.com",
                "approvers",
                java.util.List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com"))
            )
        )
            .statusCode(201)
            .extract()
            .path("identifier");
    }

    private Map<String, Object> input(String name) {
        return Map.of(
            "name",
            name,
            "description",
            "Environment description",
            "authorizationType",
            "DEV",
            "settings",
            Map.of(),
            "environmentTypeCode",
            "DEFAULT"
        );
    }

    private Map<String, Object> withType(String name, String code) {
        HashMap<String, Object> result = new java.util.HashMap<String, Object>(input(name));
        result.put("environmentTypeCode", typeIdentifier(code));
        return result;
    }

    private Map<String, Object> child(String name, String code, String parent) {
        HashMap<String, Object> result = new java.util.HashMap<String, Object>(withType(name, code));
        result.put("parentIdentifier", parent);
        return result;
    }

    private ValidatableResponse get(String path) {
        return given()
            .port(port)
            .header("correlation-id", "environment-api-it")
            .header("Authorization", "Bearer environment-api-it")
            .accept(ContentType.JSON)
            .when()
            .get(path)
            .then();
    }

    private ValidatableResponse post(String path, Object body) {
        RequestSpecification request = given()
            .port(port)
            .header("correlation-id", "environment-api-it")
            .header("Authorization", "Bearer environment-api-it")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }

    private ValidatableResponse put(String path, Object body) {
        return given()
            .port(port)
            .header("correlation-id", "environment-api-it")
            .header("Authorization", "Bearer environment-api-it")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(body)
            .when()
            .put(path)
            .then();
    }

    private int putAfter(CountDownLatch start, String path, Object body) throws Exception {
        start.await();
        return put(path, body).extract().statusCode();
    }

    private int postAfter(CountDownLatch start, String path, String parentTypeCode, String childTypeCode)
        throws Exception {
        start.await();
        return post(
            path,
            Map.of("parentTypeCode", parentTypeCode, "childTypeCode", childTypeCode)
        )
            .extract()
            .statusCode();
    }

    private String typeIdentifier(String code) {
        return code;
    }

    private Map<String, Object> typeInput(String code, String name, int displayOrder) {
        Map<String, Object> request = new HashMap<>(Map.of(
            "code", code,
            "name", name,
            "description", "Test environment type",
            "rootAllowed", !code.equals("SHARD") && !code.equals("CELL"),
            "allowsChildren", true,
            "workspaceRequired", !code.equals("DEFAULT"),
            "displayOrder", displayOrder
        ));
        return request;
    }
}
