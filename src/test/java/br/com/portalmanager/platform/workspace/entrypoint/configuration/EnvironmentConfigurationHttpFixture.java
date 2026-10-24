package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.*;
import tools.jackson.databind.json.JsonMapper;

abstract class EnvironmentConfigurationHttpFixture extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport {
    @Autowired PlatformRequestSpecificationFactory requests;
    @Autowired JdbcTemplate jdbc;
    @Autowired AuthorizationMock authorization;
    private EnvironmentConfigurationClient client;
    final String workspace = UUID.randomUUID().toString();
    final String otherWorkspace = UUID.randomUUID().toString();
    final String application = UUID.randomUUID().toString();
    final String globalEnvironment = UUID.randomUUID().toString();
    final String ownEnvironment = UUID.randomUUID().toString();
    final String foreignEnvironment = UUID.randomUUID().toString();
    final String http = "WEB_SOCKET";
    final String kafka = "KAFKA";
    final String appconfig = "APP_CONFIG";

    @BeforeEach
    void prepareConfigurations() {
        client = new EnvironmentConfigurationClient(requests, JsonMapper.builder().build());
        SchemaDefaultFixture.seed(jdbc);
        for (String code : List.of("ACTIVE", "INACTIVE", "QUARANTINED"))
            jdbc.update(
                    "insert ignore into type_life_cycle(code,label,sort_order,is_active,settings)"
                            + " values (?,?,1,true,'{}')",
                    code,
                    code);
        jdbc.update(
                "insert ignore into type_workspaces(code,label,sort_order,is_active,settings)"
                        + " values ('MANAGER','Manager',1,true,'{}')");
        jdbc.update(
                "insert ignore into"
                    + " type_application_scopes(code,label,sort_order,is_active,settings) values"
                    + " ('BACKEND','Backend',1,true,'{}')");
        jdbc.update(
                "insert ignore into type_resource_scopes(code,label,sort_order,is_active,settings)"
                    + " values"
                    + " ('WORKSPACE','Workspace',1,true,'{}'),('APPLICATION','Application',2,true,'{}')");
        jdbc.update(
                "insert ignore into type_authorizations(code,label,sort_order,is_active,settings)"
                        + " values ('DEV','DEV',1,true,'{}')");
        jdbc.update(
                "insert ignore into"
                    + " environment_types(code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at)"
                    + " values"
                    + " ('DEFAULT','Default','Default',true,true,false,'ACTIVE',1,now(6),now(6)),('CUSTOM','Custom','Custom',true,true,true,'ACTIVE',2,now(6),now(6))");
        for (String id : List.of(workspace, otherWorkspace))
            jdbc.update(
                    "insert into"
                        + " workspaces(version,identifier,workspace_type_code,name,description,requester,acronym,settings,email_group,onboarding,lifecycle_code,created_at,updated_at)"
                        + " values (0,?,'MANAGER',?,'Configuration owner"
                        + " description','requester','CONF','{\"registered\":true}','test@example.com',false,'ACTIVE',now(6),now(6))",
                    id,
                    "Owner " + id);
        jdbc.update(
                "insert into"
                    + " applications(version,identifier,workspace_id,name,alias,acronym,application_scope_code,settings,lifecycle_code,created_at,updated_at)"
                    + " values (0,?,?,'Configuration"
                    + " app','app','APP','BACKEND','{\"registered\":true}','ACTIVE',now(6),now(6))",
                application,
                workspaceId());
        environment(globalEnvironment, null, "DEFAULT");
        environment(ownEnvironment, workspaceId(), "CUSTOM");
        environment(
                foreignEnvironment,
                jdbc.queryForObject(
                        "select id from workspaces where identifier=?", Long.class, otherWorkspace),
                "CUSTOM");
        for (String code : List.of("WEB_SOCKET", "KAFKA", "APP_CONFIG"))
            jdbc.update(
                    "insert into publishers(code,label,sort_order,is_active,settings) values"
                            + " (?,?,1,true,?)",
                    code,
                    code,
                    "{\"scope\":\""
                            + (code.equals("APP_CONFIG") ? "APPLICATION" : "WORKSPACE")
                            + "\"}");
        ownerAccess();
    }

    void ownerAccess() {
        authorization.reset();
        authorization.allow(session -> session.userName("conf-user").groups("PM5_OWNER"));
    }

    Long workspaceId() {
        return jdbc.queryForObject(
                "select id from workspaces where identifier=?", Long.class, workspace);
    }

    private void environment(String id, Long workspaceId, String type) {
        jdbc.update(
                "insert into"
                    + " environments(version,identifier,workspace_id,environment_type_code,name,description,authorization_type_code,settings,sort_order,lifecycle_code,created_at,updated_at)"
                    + " values (0,?,?,?,?,'Configuration"
                    + " environment','DEV','{}',1,'ACTIVE',now(6),now(6))",
                id,
                workspaceId,
                type,
                "Environment " + id);
    }

    String base(boolean app) {
        return "/api/v1/workspaces/"
                + workspace
                + (app ? "/applications/" + application : "")
                + "/environment-configurations";
    }

    Map<String, Object> publisher(String id, int order) {
        return EnvironmentConfigurationRequestFactory.publisher(id, order);
    }

    Map<String, Object> create(String environment) {
        return EnvironmentConfigurationRequestFactory.validCreate(environment, http, kafka);
    }

    Map<String, Object> update(int version) {
        return EnvironmentConfigurationRequestFactory.validUpdate(version, http, kafka);
    }

    RequestSpecification request() {
        return client.protocolRequest();
    }

    ValidatableResponse post(String path, Object body) {
        return client.create(path, body).response();
    }

    ValidatableResponse put(String path, Object body) {
        return client.update(path, body).response();
    }

    ValidatableResponse get(String path) {
        return client.find(path).response();
    }
}
