package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidationAdapter;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import tools.jackson.databind.ObjectMapper;

import java.util.*;

class EnvironmentConfigurationApiIT extends EnvironmentConfigurationHttpFixture {
    @Test
    void supportsIndependentCrudLifecycleOrderingVersionsAndUniquenessForBothOwners() {
        for (boolean app : List.of(false, true)) {
            String base = base(app);
            String id =
                    post(base, create(globalEnvironment))
                            .statusCode(201)
                            .body("version", equalTo(0))
                            .body("lifecycle", equalTo("ACTIVE"))
                            .body("publishers.sortOrder", contains(1, 2))
                            .body("id", nullValue())
                            .body("workspaceId", nullValue())
                            .body("environmentIdentifier", equalTo(globalEnvironment))
                            .extract()
                            .path("identifier");
            assertThat(UUID.fromString(id)).isNotNull();
            get(base + "/" + id).statusCode(200).body("identifier", equalTo(id));
            get(base).statusCode(200).body("page.totalElements", equalTo(1));
            put(base + "/" + id, update(0))
                    .statusCode(200)
                    .body("version", equalTo(1))
                    .body("settings.own", equalTo("application"))
                    .body("publishers.publisherCode", contains(http, kafka));
            put(base + "/" + id, update(0)).statusCode(409).body("code", equalTo("GLOBAL-0009"));
            post(base, create(globalEnvironment))
                    .statusCode(409)
                    .body("code", equalTo("GLOBAL-0004"));
            put(base + "/" + UUID.randomUUID(), update(0)).statusCode(404);
            request().delete(base + "/" + id).then().statusCode(400);
            post(base + "/" + id + "/inactivate", null).statusCode(204);
            put(base + "/" + id, update(2)).statusCode(400);
            post(base + "/" + id + "/inactivate", null).statusCode(400);
            get(base).statusCode(200).body("page.totalElements", equalTo(0));
            get(base + "?lifecycle=INACTIVE")
                    .statusCode(200)
                    .body("page.totalElements", equalTo(1));
            request().delete(base + "/" + id).then().statusCode(204);
            get(base + "?lifecycle=QUARANTINED")
                    .statusCode(200)
                    .body("page.totalElements", equalTo(1));
            post(base, create(globalEnvironment)).statusCode(409);
            get(base + "/" + id)
                    .statusCode(200)
                    .body("lifecycle", equalTo("QUARANTINED"))
                    .body("version", equalTo(3))
                    .body("publishers", hasSize(2));
        }
        assertThat(
                        jdbc.queryForObject(
                                "select json_extract(settings,'$.registered') from workspaces where"
                                        + " identifier=?",
                                String.class,
                                workspace))
                .isEqualTo("true");
        assertThat(
                        jdbc.queryForObject(
                                "select json_extract(settings,'$.registered') from applications"
                                        + " where identifier=?",
                                String.class,
                                application))
                .isEqualTo("true");
    }

    @Test
    void enforcesStrictEnvelopeAndAcceptsOnlyFreeObjectsForSettingsAndParameters() {
        for (boolean app : List.of(false, true)) {
            String base = base(app);
            for (String field : List.of("environmentIdentifier", "settings", "publishers")) {
                Map<String, Object> invalid = create(globalEnvironment);
                invalid.remove(field);
                post(base, invalid).statusCode(400);
            }
            for (Object invalid : List.of("text", 123, true, List.of())) {
                Map<String, Object> input = create(globalEnvironment);
                input.put("settings", invalid);
                post(base, input).statusCode(400);
                input = create(globalEnvironment);
                input.put(
                        "publishers",
                        List.of(
                                Map.of(
                                        "publisherCode",
                                        http,
                                        "sortOrder",
                                        1,
                                        "parameters",
                                        invalid)));
                post(base, input).statusCode(400);
            }
            Map<String, Object> invalid = create(globalEnvironment);
            invalid.put("settings", null);
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("version", 0);
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("extra", true);
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("environmentIdentifier", "bad-uuid");
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("publishers", List.of());
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("publishers", List.of(publisher(http, 0)));
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("publishers", List.of(publisher(http, 1), publisher(kafka, 1)));
            post(base, invalid).statusCode(400);
            invalid = create(globalEnvironment);
            invalid.put("publishers", List.of(publisher(http, 1), publisher(http, 2)));
            post(base, invalid).statusCode(400);
            Map<String, Object> valid = create(globalEnvironment);
            valid.put("version", null);
            String id = post(base, valid).statusCode(201).extract().path("identifier");
            invalid = update(0);
            invalid.remove("version");
            put(base + "/" + id, invalid).statusCode(400);
            invalid = update(0);
            invalid.put("environmentIdentifier", ownEnvironment);
            put(base + "/" + id, invalid).statusCode(400);
            get(base + "?sort=settings,asc").statusCode(400);
            get(base + "?lifecycle=UNKNOWN").statusCode(400);
        }
    }

    @Test
    void resolvesGlobalDefaultAndWorkspaceEnvironmentAndRejectsCrossWorkspaceMissingAndInactive() {
        for (boolean app : List.of(false, true)) {
            post(base(app), create(ownEnvironment)).statusCode(201);
            post(base(app), create(globalEnvironment)).statusCode(201);
            post(base(app), create(foreignEnvironment)).statusCode(404);
            post(base(app), create(UUID.randomUUID().toString())).statusCode(404);
        }
        jdbc.update(
                "update environments set lifecycle_code='INACTIVE' where identifier=?",
                ownEnvironment);
        for (boolean app : List.of(false, true))
            post(base(app), create(ownEnvironment)).statusCode(404);
        String foreignAppBase = base(true).replace(workspace, otherWorkspace);
        post(foreignAppBase, create(globalEnvironment)).statusCode(404);
        jdbc.update(
                "update workspaces set lifecycle_code='INACTIVE' where identifier=?", workspace);
        for (boolean app : List.of(false, true)) get(base(app)).statusCode(404);
    }

    @Test
    void generalSchemaCanValidateParametersConditionallyByPublisherCode() {
        publishConstraint("workspace-environment-configuration-settings", "requiredSetting");
        Map<String, Object> input = create(globalEnvironment);
        post(base(false), input).statusCode(400);
        input.put("settings", Map.of("requiredSetting", true));
        constrainKafkaParameters("workspace-environment-configuration");
        post(base(false), input).statusCode(400);
        input.put(
                "publishers",
                List.of(Map.of("publisherCode", http, "sortOrder", 1, "parameters", Map.of())));
        post(base(false), input).statusCode(201);
    }

    private void constrainKafkaParameters(String code) {
        jdbc.update(
                "insert into"
                    + " schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)"
                    + " select uuid(),d.id,2,'Conditional"
                    + " Kafka',json_set(v.definition,'$.properties.publishers.items.allOf',cast(?"
                    + " as json)),'PUBLISHED',now() from schema_definitions d join schema_versions"
                    + " v on v.schema_id=d.id where d.code=? and v.schema_version=1",
                "[{\"if\":{\"properties\":{\"publisherCode\":{\"const\":\"KAFKA\"}}},\"then\":{\"properties\":{\"parameters\":{\"required\":[\"kafkaRequired\"]}}}}]",
                code);
    }

    @Test
    void acceptsEmptyAndNestedParametersForEverySupportedType() {
        for (String id : List.of(http, kafka, appconfig)) {
            boolean app = id.equals(appconfig);
            Map<String, Object> body =
                    create(id.equals(kafka) ? ownEnvironment : globalEnvironment);
            body.put(
                    "publishers",
                    List.of(Map.of("publisherCode", id, "sortOrder", 1, "parameters", Map.of())));
            String created = post(base(app), body).statusCode(201).extract().path("identifier");
            Map<String, Object> replacement = update(0);
            replacement.put(
                    "publishers",
                    List.of(
                            Map.of(
                                    "publisherCode",
                                    id,
                                    "sortOrder",
                                    1,
                                    "parameters",
                                    Map.of(
                                            "nested",
                                            Map.of("arbitrary", List.of(true, 1, "text"))))));
            put(base(app) + "/" + created, replacement)
                    .statusCode(200)
                    .body("publishers[0].parameters.nested.arbitrary", hasSize(3));
        }
    }

    @Test
    void rejectsInvalidParametersAndAssociationFieldsWithoutPersisting() {
        for (boolean app : List.of(false, true)) {
            for (String missing : List.of("publisherCode", "sortOrder", "parameters")) {
                Map<String, Object> association = new LinkedHashMap<>(publisher(http, 1));
                association.remove(missing);
                Map<String, Object> request = create(globalEnvironment);
                request.put("publishers", List.of(association));
                post(base(app), request).statusCode(400);
            }
            for (Object invalid : Arrays.asList(null, List.of(), "text", 1, true)) {
                Map<String, Object> item = new LinkedHashMap<>(publisher(http, 1));
                item.put("parameters", invalid);
                Map<String, Object> body = create(globalEnvironment);
                body.put("publishers", List.of(item));
                post(base(app), body).statusCode(400);
            }
            Map<String, Object> item = new LinkedHashMap<>(publisher(http, 1));
            item.put("extra", true);
            Map<String, Object> body = create(globalEnvironment);
            body.put("publishers", List.of(item));
            post(base(app), body).statusCode(400);
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from workspace_environment_configurations",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from application_environment_configurations",
                                Integer.class))
                .isZero();
    }

    @Test
    void misconfiguredPublisherAndIncompatibleScopeHaveDistinctErrorsAndRollbackPut() {
        String id =
                post(base(false), create(globalEnvironment))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        for (String settings :
                List.of(
                        "not-json",
                        "[]",
                        "{}",
                        "{\"scope\":null}",
                        "{\"scope\":1}",
                        "{\"scope\":\"UNKNOWN\"}")) {
            jdbc.update("update publishers set settings=? where code=?", settings, kafka);
            put(base(false) + "/" + id, update(0))
                    .statusCode(400)
                    .body("details.code", hasItem("ECFG-0101"));
        }
        jdbc.update(
                "update publishers set settings='{\"scope\":\"APPLICATION\"}' where code=?", kafka);
        put(base(false) + "/" + id, update(0))
                .statusCode(400)
                .body("details.code", hasItem("ECFG-0102"));
        jdbc.update(
                "update publishers set settings='{\"scope\":\"WORKSPACE\"}' where code=?", kafka);
        jdbc.update("update type_resource_scopes set is_active=false where code='WORKSPACE'");
        put(base(false) + "/" + id, update(0))
                .statusCode(400)
                .body("details.code", hasItem("ECFG-0101"));
        get(base(false) + "/" + id)
                .statusCode(200)
                .body("version", equalTo(0))
                .body("publishers", hasSize(2));
    }

    @Test
    void rejectsMissingCatalogDefinitionForAnAllowedCode() {
        jdbc.update("delete from publishers where code=?", kafka);
        post(base(false), create(globalEnvironment))
                .statusCode(400)
                .body("details.code", hasItem("ECFG-0101"));
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from workspace_environment_configurations",
                                Integer.class))
                .isZero();
    }

    private void publishConstraint(String code, String required) {
        jdbc.update(
                "insert into"
                    + " schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)"
                    + " select uuid(),id,2,'Configuration constraint',?,'PUBLISHED',now() from"
                    + " schema_definitions where scope_code='PLATFORM' and code=?",
                "{\"type\":\"object\",\"required\":[\""
                        + required
                        + "\"],\"additionalProperties\":true}",
                code);
    }

    @Test
    void enforcesPublisherCompatibilityAndPreservesInactiveExistingAssociations() {
        Map<String, Object> input = create(globalEnvironment);
        input.put("publishers", List.of(publisher(appconfig, 1)));
        post(base(false), input).statusCode(400);
        post(base(true), input).statusCode(201);
        jdbc.update("update publishers set deprecated=true where code=?", http);
        String id =
                post(base(false), create(globalEnvironment))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        jdbc.update("update publishers set is_active=false where code=?", http);
        get(base(false) + "/" + id).statusCode(200).body("publishers", hasSize(2));
        put(base(false) + "/" + id, update(0)).statusCode(200);
        post(base(false), create(ownEnvironment)).statusCode(400);
        Map<String, Object> missing = create(ownEnvironment);
        missing.put("publishers", List.of(publisher(UUID.randomUUID().toString(), 1)));
        post(base(false), missing).statusCode(400);
    }

    @Test
    void appliesDevToQueriesAdmToAllMutationsAndRejectsDeniedSessions() {
        for (boolean app : List.of(false, true)) {
            String id =
                    post(base(app), create(globalEnvironment))
                            .statusCode(201)
                            .extract()
                            .path("identifier");
            authorization.verifyCalledWithPolicy("ADM");
            get(base(app)).statusCode(200);
            authorization.verifyCalledWithPolicy("DEV");
            get(base(app) + "/" + id).statusCode(200);
            authorization.verifyCalledWithPolicy("DEV");
            authorization.forbidden();
            post(base(app), create(ownEnvironment)).statusCode(403);
            put(base(app) + "/" + id, update(0)).statusCode(403);
            post(base(app) + "/" + id + "/inactivate", null).statusCode(403);
            request().delete(base(app) + "/" + id).then().statusCode(403);
            authorization.verifyCalledWithPolicy("ADM");
            authorization.deny();
            get(base(app)).statusCode(401);
            ownerAccess();
        }
    }

    @Test
    void
            admCanCreateWithGlobalDefaultWithoutAnEnvironmentAuthorizerAndWorkspaceEnvironmentsKeepVisibility() {
        jdbc.update(
                "update workspaces set authorizer_group='PARENT_A' where identifier=?", workspace);
        jdbc.update(
                "update applications set authorizer_group='A-TEAM_A' where identifier=?",
                application);
        jdbc.update(
                "update environments set authorizer_group='E-TEAM_E' where identifier=?",
                ownEnvironment);
        authorization.reset();
        authorization.allow(
                session ->
                        session.userName("conf-adm")
                                .groups("USER")
                                .addAuthorizerGroup(
                                        "GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A")
                                .addAuthorizerGroup(
                                        "GRP_APPLICATION_ADM_TEAM_A", "ADM", "ADM", "A-TEAM_A"));
        for (boolean app : List.of(false, true)) {
            post(base(app), create(globalEnvironment)).statusCode(201);
            post(base(app), create(ownEnvironment)).statusCode(404);
        }
        authorization.reset();
        authorization.allow(
                session ->
                        session.userName("conf-adm")
                                .groups("USER")
                                .addAuthorizerGroup(
                                        "GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A")
                                .addAuthorizerGroup(
                                        "GRP_APPLICATION_ADM_TEAM_A", "ADM", "ADM", "A-TEAM_A")
                                .addAuthorizerGroup(
                                        "GRP_ENVIRONMENT_ADM_TEAM_E", "ADM", "ADM", "E-TEAM_E"));
        for (boolean app : List.of(false, true))
            post(base(app), create(ownEnvironment)).statusCode(201);
    }

    @Test
    void enforcesWorkspaceAndApplicationResourceVisibilityThroughReferencePorts() {
        String workspaceConfig =
                post(base(false), create(globalEnvironment))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String applicationConfig =
                post(base(true), create(globalEnvironment))
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        jdbc.update(
                "update workspaces set authorizer_group='PARENT_A' where identifier=?", workspace);
        jdbc.update(
                "update applications set authorizer_group='A-TEAM_A' where identifier=?",
                application);
        authorization.reset();
        authorization.allow(
                session ->
                        session.userName("conf-adm")
                                .groups("USER")
                                .addAuthorizerGroup(
                                        "GRP_WORKSPACE_DEV_OTHER", "DEV", "DEV", "OTHER"));
        get(base(false)).statusCode(404);
        get(base(false) + "/" + workspaceConfig).statusCode(404);
        get(base(true) + "/" + applicationConfig).statusCode(404);
        authorization.reset();
        authorization.allow(
                session ->
                        session.userName("conf-adm")
                                .groups("USER")
                                .addAuthorizerGroup(
                                        "GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A"));
        get(base(false)).statusCode(200);
        get(base(true) + "/" + applicationConfig).statusCode(404);
        authorization.reset();
        authorization.allow(
                session ->
                        session.userName("conf-adm")
                                .groups("USER")
                                .addAuthorizerGroup(
                                        "GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                                .addAuthorizerGroup(
                                        "GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A"));
        get(base(true) + "/" + applicationConfig).statusCode(200);
    }

    @Autowired SchemaSettingsValidationAdapter programmaticSchemas;
    @Autowired ObjectMapper jsonMapper;

    @Test
    void programmaticSettingsAndHttpAgreeOnUuidFormatsForBothAggregates() {
        String definition =
                "{\"$schema\":\"https://json-schema.org/draft/2020-12/schema\",\"type\":\"object\",\"required\":[\"identifier\"],\"additionalProperties\":false,\"properties\":{\"identifier\":{\"type\":\"string\",\"format\":\"uuid\"}}}";
        for (boolean app : List.of(false, true)) {
            String code =
                    (app ? "application" : "workspace") + "-environment-configuration-settings";
            jdbc.update(
                    "insert into"
                        + " schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)"
                        + " select uuid(),id,2,'Format parity',?,'PUBLISHED',now() from"
                        + " schema_definitions where scope_code='PLATFORM' and code=?",
                    definition,
                    code);
            ValidationResult errors = new ValidationResult();
            programmaticSchemas.validate(
                    "PLATFORM",
                    code,
                    "settings",
                    jsonMapper.createObjectNode().put("identifier", "invalid-uuid"),
                    errors);
            assertThat(errors.hasErrors()).isTrue();
            assertThat(errors.getDetails())
                    .singleElement()
                    .satisfies(
                            detail -> assertThat(detail.field()).isEqualTo("settings.identifier"));
            Map<String, Object> input = create(globalEnvironment);
            input.put("settings", Map.of("identifier", "invalid-uuid"));
            post(base(app), input)
                    .statusCode(400)
                    .body("details.field", hasItem("settings.identifier"));
            input.put("settings", Map.of("identifier", globalEnvironment));
            ValidationResult valid = new ValidationResult();
            programmaticSchemas.validate(
                    "PLATFORM",
                    code,
                    "settings",
                    jsonMapper.valueToTree(input.get("settings")),
                    valid);
            assertThat(valid.hasErrors()).isFalse();
            post(base(app), input).statusCode(201);
        }
    }
}
