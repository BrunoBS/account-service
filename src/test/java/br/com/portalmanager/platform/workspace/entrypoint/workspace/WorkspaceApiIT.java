package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

class WorkspaceApiIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private PlatformRequestSpecificationFactory requests;

    private WorkspaceClient client;

    @BeforeEach
    void authorizeAsOwner() {
        client = new WorkspaceClient(requests, JsonMapper.builder().build());
        new WorkspaceScenario(jdbcTemplate, authorizationMock).ownerCanManageWorkspaces();
    }

    @Test
    void shouldValidateOriginalJsonBeforeBindingTypedRequests() {
        Map<String, Object> payload = validCreate("Typed Workspace", "MANAGER");
        io.restassured.response.ExtractableResponse<io.restassured.response.Response> created = post(payload)
            .statusCode(201)
            .extract();
        String identifier = created.path("identifier");
        Object version = created.path("version");
        Map<String, Object> invalidValues = Map.of(
            "name",
            123,
            "requester",
            true,
            "tags",
            List.of(123),
            "version",
            "0",
            "unexpected",
            "must not disappear",
            "approvers",
            List.of(Map.of("functional", "F1000", "email", "approver@example.com", "unexpected", true))
        );
        for (Map.Entry<String, Object> invalidValue : invalidValues.entrySet()) {
            Map<String, Object> invalid = validCreate("Invalid Typed Workspace", "MANAGER");
            invalid.put("version", version);
            invalid.put(invalidValue.getKey(), invalidValue.getValue());
            post(invalid).statusCode(400);
            put("/api/v1/workspaces/" + identifier, invalid).statusCode(400);
        }
        get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .body("version", equalTo(version));
    }

    @Test
    void shouldCreateNormalizeAndReadActiveWorkspace() {
        Map<String, Object> request = validCreate("  Workspace G3  ", " admin ");
        request.put("description", "  Descrição válida do workspace G3  ");
        request.put("requester", "  requester  ");
        request.put("acronym", " G3 ");
        request.put("authorizerGroup", "  GRP_WORK  ");
        request.put("emailGroup", "  workspace@portalmanager.com  ");
        request.put("approvers", List.of(Map.of("functional", "  F1234  ", "email", "  approver@portalmanager.com  ")));

        String identifier = post(request)
            .statusCode(201)
            .body("id", nullValue())
            .body("identifier", not(nullValue()))
            .body("version", equalTo(0))
            .body("workspaceType", equalTo("ADMIN"))
            .body("name", equalTo("Workspace G3"))
            .body("description", equalTo("Descrição válida do workspace G3"))
            .body("requester", equalTo("requester"))
            .body("acronym", equalTo("G3"))
            .body("authorizerGroup", equalTo("GRP_WORK"))
            .body("emailGroup", equalTo("workspace@portalmanager.com"))
            .body("lifecycle", equalTo("ACTIVE"))
            .body("onboarding", equalTo(false))
            .body("approvers", hasSize(1))
            .body("approvers[0].functional", equalTo("F1234"))
            .extract()
            .path("identifier");

        get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .body("id", nullValue())
            .body("identifier", equalTo(identifier))
            .body("name", equalTo("Workspace G3"))
            .body("lifecycle", equalTo("ACTIVE"));
    }

    @Test
    void shouldListByLifecycleAndNormalizedTypeFilter() {
        String adminIdentifier = create("Workspace Admin", "ADMIN");
        String managerIdentifier = create("Workspace Manager", "MANAGER");

        post("/api/v1/workspaces/" + managerIdentifier + "/inactivate").statusCode(204);

        client.listActiveByType(" admin ").response()
            .statusCode(200)
            .body("content.identifier", hasItem(adminIdentifier))
            .body("content.identifier", not(hasItem(managerIdentifier)))
            .body("content.workspaceType", containsInAnyOrder("ADMIN"));

        get("/api/v1/workspaces?lifecycle=INACTIVE")
            .statusCode(200)
            .body("content.identifier", containsInAnyOrder(managerIdentifier))
            .body("content.lifecycle", containsInAnyOrder("INACTIVE"));
    }

    @Test
    void shouldUpdateWorkspaceAndReplaceApprovers() {
        String identifier = create("Workspace Atualizável", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Atualizável", "MANAGER");
        update.put("description", "Descrição atualizada com sucesso");
        update.put(
            "approvers",
            List.of(
                Map.of("functional", "F2000", "email", "new1@portalmanager.com"),
                Map.of("functional", "F3000", "email", "new2@portalmanager.com")
            )
        );

        put("/api/v1/workspaces/" + identifier, update)
            .statusCode(200)
            .body("id", nullValue())
            .body("identifier", equalTo(identifier))
            .body("version", greaterThan(version))
            .body("name", equalTo("Workspace Atualizável"))
            .body("workspaceType", equalTo("MANAGER"))
            .body("description", equalTo("Descrição atualizada com sucesso"))
            .body("approvers", hasSize(2))
            .body("approvers.functional", containsInAnyOrder("F2000", "F3000"));
    }

    @Test
    void shouldRejectValuesLargerThanDatabaseColumnsOnCreateAndUpdate() {
        for (String field : List.of(
            "name",
            "description",
            "acronym",
            "tags",
            "requester",
            "authorizerGroup",
            "emailGroup",
            "approvers[0].functional",
            "approvers[0].email"
        )) {
            String name = "Workspace Limites " + field;
            String identifier = create(name, "ADMIN");
            Integer version = get("/api/v1/workspaces/" + identifier)
                .statusCode(200)
                .extract()
                .path("version");
            Map<String, Object> request = validCreate(name + " Create", "ADMIN");
            applyOversizedValue(request, field);
            String expectedField = field.equals("tags") ? "tags[0]" : field;
            post(request).statusCode(400).body("details.field", hasItem(expectedField));
            request = validUpdate(version, name, "ADMIN");
            applyOversizedValue(request, field);
            put("/api/v1/workspaces/" + identifier, request)
                .statusCode(400)
                .body("details.field", hasItem(expectedField));
            get("/api/v1/workspaces/" + identifier)
                .statusCode(200)
                .body("version", equalTo(version));
        }
    }

    @Test
    void shouldLimitApproversAndTagsOnCreateAndUpdate() {
        String identifier = create("Workspace Collection Bounds", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");
        List<Map<String, String>> approvers = java.util.stream.IntStream.range(0, 6)
            .mapToObj(index -> Map.of("functional", "F" + index, "email", "user" + index + "@example.com"))
            .toList();
        List<String> tags = java.util.stream.IntStream.range(0, 21)
            .mapToObj(index -> "tag-" + index)
            .toList();
        for (String field : List.of("approvers", "tags")) {
            Map<String, Object> request = validCreate("Workspace Bounds " + field, "ADMIN");
            request.put(field, field.equals("approvers") ? approvers : tags);
            post(request).statusCode(400).body("details.field", hasItem(field));
            request = validUpdate(version, "Workspace Collection Bounds", "ADMIN");
            request.put(field, field.equals("approvers") ? approvers : tags);
            put("/api/v1/workspaces/" + identifier, request)
                .statusCode(400)
                .body("details.field", hasItem(field));
        }
        Map<String, Object> maximum = validUpdate(version, "Workspace Collection Bounds", "ADMIN");
        maximum.put("approvers", approvers.subList(0, 5));
        maximum.put("tags", tags.subList(0, 20));
        put("/api/v1/workspaces/" + identifier, maximum)
            .statusCode(200)
            .body("approvers", hasSize(5))
            .body("tags", hasSize(20));
    }

    @Test
    void shouldPaginateWithoutTruncatingApproversAndValidatePageParameters() {
        for (int index = 0; index < 3; index++) {
            Map<String, Object> request = validCreate("Workspace Page " + index, "ADMIN");
            request.put(
                "approvers",
                List.of(
                    Map.of("functional", "F1", "email", "first@example.com"),
                    Map.of("functional", "F2", "email", "second@example.com")
                )
            );
            post(request).statusCode(201);
        }
        get("/api/v1/workspaces?page=0&size=2")
            .statusCode(200)
            .body("content", hasSize(2))
            .body("page.number", equalTo(0))
            .body("page.size", equalTo(2))
            .body("page.totalElements", equalTo(3))
            .body("page.totalPages", equalTo(2))
            .body("content[0].approvers", hasSize(2));
        get("/api/v1/workspaces?page=1&size=2")
            .statusCode(200)
            .body("content", hasSize(1))
            .body("content[0].name", equalTo("Workspace Page 2"))
            .body("page.totalElements", equalTo(3));
        for (String parameters : List.of("page=-1", "size=0", "page=text", "size=text", "page=2147483648")) {
            get("/api/v1/workspaces?" + parameters)
                .statusCode(200)
                .body("page.number", equalTo(0))
                .body("page.size", equalTo(20));
        }
        get("/api/v1/workspaces?size=101").statusCode(200).body("page.size", equalTo(100));
        get("/api/v1/workspaces?page=2147483647&size=100").statusCode(400);
        get("/api/v1/workspaces?sort=approvers.email").statusCode(400);
        get("/api/v1/workspaces?sort=unknown").statusCode(400);
        get("/api/v1/workspaces?sort=name,desc&size=2")
            .statusCode(200)
            .body("content.name", contains("Workspace Page 2", "Workspace Page 1"));
    }

    @Test
    void shouldRejectCatalogAsWorkspaceTypeAndFilter() {
        post(validCreate("Workspace Unsupported Type", "CATALOG")).statusCode(400);
        get("/api/v1/workspaces?typeName=CATALOG").statusCode(400);
    }

    @Test
    void shouldAcceptMaximumLengthsIncludingTheWorkspaceNameSystemTag() {
        Map<String, Object> request = validCreate("N".repeat(50), "ADMIN");
        request.put("description", "D".repeat(200));
        request.put("requester", "R".repeat(10));
        request.put("acronym", "ABCDE");
        request.put("authorizerGroup", "G".repeat(10));
        String maximumEmail = "a".repeat(63) + "@" + "b".repeat(32) + ".com";
        request.put("emailGroup", maximumEmail);
        request.put("tags", List.of("T".repeat(50)));
        request.put("approvers", List.of(Map.of("functional", "F".repeat(10), "email", maximumEmail)));
        String identifier = post(request).statusCode(201).extract().path("identifier");
        org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForList(
                "select t.name from workspace_tag t join workspaces w on w.id = t.workspace_id where w.identifier = ?",
                String.class,
                identifier
            )
        ).contains("n".repeat(50), "t".repeat(50));
    }

    @Test
    void shouldPreserveUnchangedApproversAndAllowEmailSwaps() {
        Map<String, Object> request = validCreate("Workspace Approver Reconcile", "ADMIN");
        List<Map<String, String>> approvers = List.of(
            Map.of("functional", "F1000", "email", "first@portalmanager.com"),
            Map.of("functional", "F2000", "email", "second@portalmanager.com")
        );
        request.put("approvers", approvers);
        String identifier = post(request).statusCode(201).extract().path("identifier");
        List<String> originalIds = approverIds(identifier);
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");
        request.put("version", version);
        version = put("/api/v1/workspaces/" + identifier, request)
            .statusCode(200)
            .extract()
            .path("version");
        org.assertj.core.api.Assertions.assertThat(approverIds(identifier)).containsExactlyElementsOf(originalIds);
        request.put("version", version);
        request.put(
            "approvers",
            List.of(
                Map.of("functional", "F1000", "email", "second@portalmanager.com"),
                Map.of("functional", "F2000", "email", "first@portalmanager.com")
            )
        );
        put("/api/v1/workspaces/" + identifier, request).statusCode(200);
        get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .body("approvers.email", containsInAnyOrder("first@portalmanager.com", "second@portalmanager.com"));
        org.assertj.core.api.Assertions.assertThat(approverIds(identifier))
            .hasSize(2)
            .doesNotContainAnyElementsOf(originalIds);
    }

    private List<String> approverIds(String identifier) {
        return jdbcTemplate.queryForList(
            "select a.id from workspace_approvers a join workspaces w on w.id = a.workspace_id where w.identifier = ? order by a.functional",
            String.class,
            identifier
        );
    }

    private void applyOversizedValue(Map<String, Object> request, String field) {
        if (field.startsWith("approvers")) {
            request.put(
                "approvers",
                List.of(
                    Map.of(
                        "functional",
                        field.endsWith("functional") ? "F".repeat(11) : "F1000",
                        "email",
                        field.endsWith("email") ? "a".repeat(89) + "@example.com" : "approver@example.com"
                    )
                )
            );
        } else {
            int maximumLength = switch (field) {
                case "name" -> 50;
                case "description" -> 200;
                case "requester" -> 10;
                case "authorizerGroup" -> 10;
                case "acronym" -> 5;
                case "tags" -> 50;
                default -> 100;
            };
            if (field.equals("tags")) request.put(field, List.of("T".repeat(maximumLength + 1)));
            else request.put(
                field,
                field.equals("emailGroup") ? "a".repeat(89) + "@example.com" : "A".repeat(maximumLength + 1)
            );
        }
    }

    @Test
    void shouldRejectStaleUpdateWithConflict() {
        String identifier = create("Workspace Concorrente", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> first = validUpdate(version, "Workspace Concorrente", "ADMIN");
        first.put("description", "Descrição da primeira atualização");
        put("/api/v1/workspaces/" + identifier, first).statusCode(200);

        Map<String, Object> stale = validUpdate(version, "Workspace Concorrente", "ADMIN");
        stale.put("description", "Descrição da atualização obsoleta");

        put("/api/v1/workspaces/" + identifier, stale)
            .statusCode(409)
            .body("code", equalTo("GLOBAL-0009"));
    }

    @Test
    void shouldEnforceGlobalCaseInsensitiveNameUniquenessOnCreateAndUpdate() {
        String identifier = create("Portal CaseInsensitive", "ADMIN");
        post(validCreate("  portal caseinsensitive  ", "MANAGER"))
            .statusCode(400)
            .body("details.field", hasItem("name"));
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");
        put("/api/v1/workspaces/" + identifier, validUpdate(version, "PORTAL CASEINSENSITIVE", "ADMIN"))
            .statusCode(200)
            .body("name", equalTo("PORTAL CASEINSENSITIVE"));
        String otherIdentifier = create("Other Case Workspace", "ADMIN");
        Integer otherVersion = get("/api/v1/workspaces/" + otherIdentifier)
            .statusCode(200)
            .extract()
            .path("version");
        put("/api/v1/workspaces/" + otherIdentifier, validUpdate(otherVersion, "portal caseinsensitive", "ADMIN"))
            .statusCode(400)
            .body("details.field", hasItem("name"));
        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);
        post(validCreate("portal caseinsensitive", "MANAGER")).statusCode(400).body("details.field", hasItem("name"));
    }

    @Test
    void shouldRejectDuplicateNameAfterNormalization() {
        create("Workspace Único G3", "ADMIN");

        post(validCreate("  Workspace Único G3  ", "MANAGER"))
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("name"));
    }

    @Test
    void shouldRejectDuplicateApproverFunctionalOnCreate() {
        Map<String, Object> request = validCreate("Workspace Functional Duplicado", "ADMIN");
        request.put(
            "approvers",
            List.of(
                Map.of("functional", "F1000", "email", "first@portalmanager.com"),
                Map.of("functional", "f1000", "email", "second@portalmanager.com")
            )
        );

        post(request)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].functional"))
            .body(
                "details.message",
                hasItem("O funcional informado já foi adicionado como aprovador deste workspace.")
            );
    }

    @Test
    void shouldRejectDuplicateApproverEmailOnCreate() {
        Map<String, Object> request = validCreate("Workspace Email Duplicado", "ADMIN");
        request.put(
            "approvers",
            List.of(
                Map.of("functional", "F1000", "email", "same@portalmanager.com"),
                Map.of("functional", "F2000", "email", "SAME@portalmanager.com")
            )
        );

        post(request)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].email"))
            .body("details.message", hasItem("O e-mail informado já foi adicionado como aprovador deste workspace."));
    }

    @Test
    void shouldRejectDuplicateApproverFunctionalOnUpdate() {
        String identifier = create("Workspace Update Functional", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Update Functional", "ADMIN");
        update.put(
            "approvers",
            List.of(
                Map.of("functional", "F3000", "email", "first-update@portalmanager.com"),
                Map.of("functional", "f3000", "email", "second-update@portalmanager.com")
            )
        );

        put("/api/v1/workspaces/" + identifier, update)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].functional"));
    }

    @Test
    void shouldRejectDuplicateApproverEmailOnUpdate() {
        String identifier = create("Workspace Update Email", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Update Email", "ADMIN");
        update.put(
            "approvers",
            List.of(
                Map.of("functional", "F4000", "email", "same-update@portalmanager.com"),
                Map.of("functional", "F5000", "email", "SAME-UPDATE@portalmanager.com")
            )
        );

        put("/api/v1/workspaces/" + identifier, update)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].email"));
    }

    @Test
    void shouldReturnValidationDetailsForInvalidPayload() {
        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("workspaceType", "INVALID");
        invalid.put("name", " A ");
        invalid.put("description", " curta ");
        invalid.put("requester", " x ");
        invalid.put("acronym", "TOO-LONG");
        invalid.put("emailGroup", "invalid-email");
        invalid.put("approvers", List.of());

        post(invalid)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("workspaceType"))
            .body("details.field", hasItem("name"))
            .body("details.field", hasItem("description"))
            .body("details.field", hasItem("requester"))
            .body("details.field", hasItem("acronym"))
            .body("details.field", hasItem("emailGroup"))
            .body("details.field", hasItem("approvers"));
    }

    @Test
    void shouldRejectInvalidStructureWithoutCoercingOrDiscardingFields() {
        for (Map<String, Object> invalidField : List.<Map<String, Object>>of(
            Map.of("name", 123),
            Map.of("settings", List.of()),
            Map.of("tags", "invalid"),
            Map.of(
                "approvers",
                List.of(Map.of("functional", "F1000", "email", "valid@example.com", "unexpected", true))
            ),
            Map.of("unexpected", true)
        )) {
            Map<String, Object> request = validCreate("Workspace Schema Structure", "ADMIN");
            request.putAll(invalidField);
            post(request).statusCode(400).body("code", equalTo("GLOBAL-0001"));
        }
        client.protocolRequest()
            .contentType(ContentType.JSON)
            .body(List.of())
            .post("/api/v1/workspaces")
            .then()
            .statusCode(400);
        org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("select count(*) from workspaces", Integer.class)
        ).isZero();
    }

    @Test
    void shouldUseTheSameSchemaForCreationAndUpdateWhileRejectingVersionOnCreate() {
        Map<String, Object> request = validCreate("Workspace Shared Schema", "ADMIN");
        request.put("version", 999);
        post(request).statusCode(400);
        request.remove("version");
        String identifier = post(request).statusCode(201).body("version", equalTo(0)).extract().path("identifier");
        request.put("version", 0);
        put("/api/v1/workspaces/" + identifier, request).statusCode(200);
    }

    @Test
    void shouldRequireVersionForUpdateAndValidateItsStructureWithTheSharedSchema() {
        String identifier = create("Workspace Schema Version", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");
        Map<String, Object> request = validCreate("Workspace Schema Version", "ADMIN");
        put("/api/v1/workspaces/" + identifier, request)
            .statusCode(400)
            .body("details.field", hasItem("version"));
        for (Object invalidVersion : List.of(-1, 0.5, "0")) {
            request.put("version", invalidVersion);
            put("/api/v1/workspaces/" + identifier, request)
                .statusCode(400)
                .body("details.field", hasItem("version"));
        }
        request.put("version", null);
        put("/api/v1/workspaces/" + identifier, request)
            .statusCode(400)
            .body("details.field", hasItem("version"));
        get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .body("version", equalTo(version));
    }

    @Test
    void shouldRejectWorkspaceSettingsThatAreNotAJsonObject() {
        Map<String, Object> request = validCreate("Workspace Invalid Settings", "ADMIN");
        request.put("settings", "not-an-object");

        post(request).statusCode(400).body("code", equalTo("GLOBAL-0001")).body("details.field", hasItem("settings"));
    }

    @Test
    void shouldInactivateHideAndRestoreWorkspace() {
        String identifier = create("Workspace Lifecycle", "ADMIN");

        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);

        get("/api/v1/workspaces/" + identifier)
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));

        post("/api/v1/workspaces/" + identifier + "/restore")
            .statusCode(200)
            .body("id", nullValue())
            .body("identifier", equalTo(identifier))
            .body("name", equalTo("Workspace Lifecycle"))
            .body("lifecycle", equalTo("ACTIVE"));

        post("/api/v1/workspaces/" + identifier + "/restore")
            .statusCode(400)
            .body("code", equalTo("WORKSPACE-0002"));
    }

    @Test
    void shouldQuarantineOnlyInactiveWorkspace() {
        String identifier = create("Workspace Quarantine", "ADMIN");

        delete("/api/v1/workspaces/" + identifier)
            .statusCode(400)
            .body("code", equalTo("WORKSPACE-0003"));

        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);

        delete("/api/v1/workspaces/" + identifier).statusCode(204);

        String lifecycle = jdbcTemplate.queryForObject(
            "SELECT lifecycle_code FROM workspaces WHERE identifier = ?",
            String.class,
            identifier
        );
        org.assertj.core.api.Assertions.assertThat(lifecycle).isEqualTo("QUARANTINED");
        post("/api/v1/workspaces/" + identifier + "/restore")
            .statusCode(400)
            .body("code", equalTo("WORKSPACE-0002"));
        delete("/api/v1/workspaces/" + identifier)
            .statusCode(400)
            .body("code", equalTo("WORKSPACE-0003"));
        org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject(
                "SELECT lifecycle_code FROM workspaces WHERE identifier = ?",
                String.class,
                identifier
            )
        ).isEqualTo("QUARANTINED");
    }

    @Test
    void shouldRejectInvalidTypeFilter() {
        get("/api/v1/workspaces?typeName=INVALID")
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("typeName"));
    }

    @Test
    void shouldReturnNotFoundForUpdateAndDeleteOfUnknownWorkspace() {
        Map<String, Object> update = validUpdate(0, "Workspace Inexistente", "ADMIN");

        put("/api/v1/workspaces/00000000-0000-0000-0000-000000000000", update)
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));

        delete("/api/v1/workspaces/00000000-0000-0000-0000-000000000000")
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));
    }

    private String create(String name, String type) {
        return post(validCreate(name, type)).statusCode(201).extract().path("identifier");
    }

    @Test
    void shouldPreventTypeChangeWithApplicationsInAnyLifecycle() {
        String identifier = create("Workspace With Application", "MANAGER");
        Long workspaceId = jdbcTemplate.queryForObject(
            "select id from workspaces where identifier = ?",
            Long.class,
            identifier
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend application', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT INTO applications (identifier, workspace_id, name, alias, acronym, application_scope_code, settings, lifecycle_code, created_at, updated_at) VALUES (?, ?, 'Linked App', 'linked', 'APP', 'BACKEND', '{}', 'ACTIVE', NOW(), NOW())",
            java.util.UUID.randomUUID().toString(),
            workspaceId
        );
        for (String lifecycle : List.of("ACTIVE", "INACTIVE", "QUARANTINED")) {
            jdbcTemplate.update(
                "UPDATE applications SET lifecycle_code = ? WHERE workspace_id = ?",
                lifecycle,
                workspaceId
            );
            put("/api/v1/workspaces/" + identifier, validUpdate(0, "Workspace With Application", "ADMIN"))
                .statusCode(400)
                .body("details.field", hasItem("workspaceType"));
            get("/api/v1/workspaces/" + identifier)
                .statusCode(200)
                .body("workspaceType", equalTo("MANAGER"))
                .body("version", equalTo(0));
        }
        put("/api/v1/workspaces/" + identifier, validUpdate(0, "Workspace With Application Edited", "MANAGER"))
            .statusCode(200)
            .body("workspaceType", equalTo("MANAGER"));
    }

    @Test
    void shouldAllowOwnerTypeChangeWithoutApplications() {
        String identifier = create("Workspace Without Application", "MANAGER");
        put("/api/v1/workspaces/" + identifier, validUpdate(0, "Workspace Without Application", "ADMIN"))
            .statusCode(200)
            .body("workspaceType", equalTo("ADMIN"));
    }

    private Map<String, Object> validCreate(String name, String type) {
        return WorkspaceRequestFactory.validCreate(name, type);
    }

    private Map<String, Object> validUpdate(Integer version, String name, String type) {
        return WorkspaceRequestFactory.validUpdate(version, name, type);
    }

    private ValidatableResponse get(String path) {
        return client.find(path).response();
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return client.create(body).response();
    }

    private ValidatableResponse post(String path) {
        return client.action(path).response();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return client.update(path, body).response();
    }

    private ValidatableResponse delete(String path) {
        return client.delete(path).response();
    }

}
