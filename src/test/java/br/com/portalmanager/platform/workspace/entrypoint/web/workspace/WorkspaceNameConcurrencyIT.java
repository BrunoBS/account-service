package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doAnswer;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class WorkspaceNameConcurrencyIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @MockitoSpyBean
    private WorkspaceRepository repository;

    @MockitoSpyBean
    private WorkspaceFinder workspaceFinder;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void setUp() {
        SchemaDefaultFixture.seed(jdbcTemplate);
        for (String code : List.of("ACTIVE", "INACTIVE", "QUARANTINED")) {
            jdbcTemplate.update(
                "INSERT IGNORE INTO type_life_cycle (code, label, sort_order, is_active, settings) VALUES (?, ?, 1, true, '{}')",
                code,
                code
            );
        }
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 1, true, '{}')"
        );
        authorizationMock.reset();
        authorizationMock.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
    }

    @Test
    void shouldTranslateConcurrentCaseInsensitiveCreateCollisionAndRollBackTheLoser() throws Exception {
        String name = "Concurrent Workspace Create";
        CyclicBarrier prechecks = new CyclicBarrier(2);
        // Both requests observe the name as available before either can insert.
        doAnswer(invocation -> {
            prechecks.await(15, TimeUnit.SECONDS);
            return false;
        })
            .when(repository)
            .existsByName(matches("(?i)" + name));
        List<Response> responses = concurrent(
            () -> post(input(name)),
            () -> post(input(name.toLowerCase(java.util.Locale.ROOT)))
        );
        assertThat(responses.stream().map(Response::statusCode).toList()).containsExactlyInAnyOrder(201, 409);
        assertSafeIntegrityConflictResponse(responses);
        assertThat(
            jdbcTemplate.queryForObject("select count(*) from workspaces where name = ?", Long.class, name)
        ).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("select count(*) from workspace_approvers", Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldTranslateConcurrentCaseInsensitiveRenameCollisionAndPreserveTheLoser() throws Exception {
        Response first = post(input("Workspace Rename First"));
        Response second = post(input("Workspace Rename Second"));
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(second.statusCode()).isEqualTo(201);
        String name = "Concurrent Workspace Rename";
        Map<String, Object> firstUpdate = input(name);
        Map<String, Object> secondUpdate = input(name.toLowerCase(java.util.Locale.ROOT));
        firstUpdate.put("version", first.jsonPath().getLong("version"));
        secondUpdate.put("version", second.jsonPath().getLong("version"));
        CyclicBarrier prechecks = new CyclicBarrier(2);
        doAnswer(invocation -> {
            prechecks.await(15, TimeUnit.SECONDS);
            return false;
        })
            .when(repository)
            .existsByNameAndIdNot(matches("(?i)" + name), anyLong());
        List<Response> responses = concurrent(
            () -> put(first.jsonPath().getString("identifier"), firstUpdate),
            () -> put(second.jsonPath().getString("identifier"), secondUpdate)
        );
        assertThat(responses.stream().map(Response::statusCode).toList()).containsExactlyInAnyOrder(200, 409);
        assertSafeIntegrityConflictResponse(responses);
        assertThat(
            jdbcTemplate.queryForObject("select count(*) from workspaces where name = ?", Long.class, name)
        ).isEqualTo(1L);
        assertThat(
            jdbcTemplate.queryForObject(
                "select count(*) from workspaces where name in ('Workspace Rename First', 'Workspace Rename Second') and version = 0",
                Long.class
            )
        ).isEqualTo(1L);
    }

    private void assertSafeIntegrityConflictResponse(List<Response> responses) {
        Response rejected = responses
            .stream()
            .filter(response -> response.statusCode() == 409)
            .findFirst()
            .orElseThrow();
        assertThat(rejected.jsonPath().getString("code")).isEqualTo("GLOBAL-0004");
        assertThat(rejected.jsonPath().getString("message")).isNotBlank();
        assertThat(rejected.jsonPath().getList("details")).isEmpty();
        assertThat(rejected.asString()).doesNotContain(
            "uk_workspaces_name",
            "Duplicate entry",
            "SQL",
            "ConstraintViolationException",
            "DataIntegrityViolationException",
            "insert into",
            "update workspaces",
            "workspace_approvers",
            "Concurrent Workspace"
        );
    }

    @Test
    void shouldRejectTypeChangeAfterWaitingForApplicationCreation() throws Exception {
        seedApplicationScopeAndAdminType();
        Response created = post(input("Workspace Application Race"));
        String identifier = created.jsonPath().getString("identifier");
        Long workspaceId = jdbcTemplate.queryForObject(
            "select id from workspaces where identifier = ?",
            Long.class,
            identifier
        );
        EntityManager writer = entityManagerFactory.createEntityManager();
        CountDownLatch requestedLock = observeWorkspaceLock(identifier);
        Map<String, Object> update = input("Workspace Application Race");
        update.put("workspaceType", "ADMIN");
        update.put("version", 0);
        writer.getTransaction().begin();
        writer.find(Workspace.class, workspaceId, LockModeType.PESSIMISTIC_WRITE);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Response> response = executor.submit(() -> put(identifier, update));
            try {
                assertThat(requestedLock.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> response.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                writer
                    .createNativeQuery(
                        "INSERT INTO applications (identifier, workspace_id, name, alias, acronym, application_scope_code, settings, lifecycle_code, created_at, updated_at) VALUES (UUID(), :workspaceId, 'Concurrent App', 'concurrent', 'APP', 'BACKEND', '{}', 'ACTIVE', NOW(), NOW())"
                    )
                    .setParameter("workspaceId", workspaceId)
                    .executeUpdate();
                writer.getTransaction().commit();
                assertThat(response.get(20, TimeUnit.SECONDS).statusCode()).isEqualTo(400);
                assertThat(
                    jdbcTemplate.queryForObject(
                        "select workspace_type_code from workspaces where id = ?",
                        String.class,
                        workspaceId
                    )
                ).isEqualTo("MANAGER");
            } finally {
                if (writer.getTransaction().isActive()) writer.getTransaction().rollback();
            }
        } finally {
            writer.close();
        }
    }

    @Test
    void shouldRejectApplicationCreationAfterWaitingForTypeChange() throws Exception {
        seedApplicationScopeAndAdminType();
        String identifier = post(input("Workspace Type Race")).jsonPath().getString("identifier");
        Long workspaceId = jdbcTemplate.queryForObject(
            "select id from workspaces where identifier = ?",
            Long.class,
            identifier
        );
        EntityManager writer = entityManagerFactory.createEntityManager();
        CountDownLatch requestedLock = observeWorkspaceLock(identifier);
        writer.getTransaction().begin();
        writer.find(Workspace.class, workspaceId, LockModeType.PESSIMISTIC_WRITE);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Response> response = executor.submit(() ->
                given()
                    .port(port)
                    .header("correlation-id", "workspace-application-race")
                    .header("Authorization", "Bearer workspace-application-race")
                    .contentType(ContentType.JSON)
                    .body(
                        Map.of(
                            "name",
                            "Concurrent App",
                            "alias",
                            "concurrent",
                            "acronym",
                            "APP",
                            "applicationScope",
                            "BACKEND",
                            "settings",
                            Map.of("language", "JAVA", "infrastructure", "VM")
                        )
                    )
                    .post("/api/v1/workspaces/" + identifier + "/applications")
            );
            try {
                assertThat(requestedLock.await(10, TimeUnit.SECONDS)).isTrue();
                assertThatThrownBy(() -> response.get(200, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
                writer
                    .createNativeQuery(
                        "update workspaces set workspace_type_code = 'ADMIN', version = version + 1 where id = :workspaceId"
                    )
                    .setParameter("workspaceId", workspaceId)
                    .executeUpdate();
                writer.getTransaction().commit();
                assertThat(response.get(20, TimeUnit.SECONDS).statusCode()).isEqualTo(400);
                assertThat(
                    jdbcTemplate.queryForObject(
                        "select count(*) from applications where workspace_id = ?",
                        Long.class,
                        workspaceId
                    )
                ).isZero();
            } finally {
                if (writer.getTransaction().isActive()) writer.getTransaction().rollback();
            }
        } finally {
            writer.close();
        }
    }

    private CountDownLatch observeWorkspaceLock(String identifier) {
        CountDownLatch requestedLock = new CountDownLatch(1);
        WorkspaceFinder finderTarget = AopTestUtils.getUltimateTargetObject(workspaceFinder);
        doAnswer(invocation -> {
            requestedLock.countDown();
            return invocation.callRealMethod();
        })
            .when(finderTarget)
            .findActiveForUpdate(identifier);
        return requestedLock;
    }

    private void seedApplicationScopeAndAdminType() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend application', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 2, true, '{}')"
        );
    }

    private List<Response> concurrent(Callable<Response> firstRequest, Callable<Response> secondRequest)
        throws Exception {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Response> first = executor.submit(firstRequest);
            Future<Response> second = executor.submit(secondRequest);
            return List.of(first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS));
        }
    }

    private Map<String, Object> input(String name) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("workspaceType", "MANAGER");
        input.put("name", name);
        input.put("description", "Workspace name concurrency test");
        input.put("requester", "requester");
        input.put("acronym", "RACE");
        input.put("emailGroup", "workspace@example.com");
        input.put("approvers", List.of(Map.of("functional", "F1234", "email", "approver@example.com")));
        return input;
    }

    private Response post(Map<String, Object> input) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-name-race")
            .header("Authorization", "Bearer workspace-name-race")
            .contentType(ContentType.JSON)
            .body(input)
            .when()
            .post("/api/v1/workspaces");
    }

    private Response put(String identifier, Map<String, Object> input) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-name-race")
            .header("Authorization", "Bearer workspace-name-race")
            .contentType(ContentType.JSON)
            .body(input)
            .when()
            .put("/api/v1/workspaces/" + identifier);
    }
}
