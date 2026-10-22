package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.domain.*;
import br.com.portalmanager.platform.workspace.core.applicationconfiguration.repository.*;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.domain.*;
import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.repository.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;

@PlatformIntegrationTest
@WithMySql
@TestPropertySource(
        properties = {"platform.authorization.mode=MOCK", "platform.audit.enabled=false"})
class EnvironmentConfigurationPersistenceIT {
    @Autowired JdbcTemplate jdbc;
    @Autowired WorkspaceEnvironmentConfigurationRepository workspaces;
    @Autowired WorkspaceEnvironmentConfigurationPublisherRepository workspacePublishers;
    @Autowired ApplicationEnvironmentConfigurationRepository applications;
    @Autowired ApplicationEnvironmentConfigurationPublisherRepository applicationPublishers;
    @Autowired PlatformTransactionManager transactions;
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void flywayCreatesIndependentJsonAggregatesAndOrderedRelationshipsWithOptimisticVersion() {
        seed();
        Long workspaceId =
                jdbc.queryForObject(
                        "select id from workspaces where name='Configuration owner'", Long.class);
        Long applicationId =
                jdbc.queryForObject(
                        "select id from applications where name='Configuration app'", Long.class);
        Long environmentId =
                jdbc.queryForObject(
                        "select id from environments where name='Configuration environment'",
                        Long.class);
        List<String> publisherIds =
                jdbc.queryForList("select code from publishers order by code", String.class);
        LocalDateTime now = LocalDateTime.now();
        WorkspaceEnvironmentConfiguration workspace =
                workspaces.saveAndFlush(
                        new WorkspaceEnvironmentConfiguration(
                                workspaceId,
                                environmentId,
                                mapper.createObjectNode().put("owner", "workspace"),
                                now));
        ApplicationEnvironmentConfiguration application =
                applications.saveAndFlush(
                        new ApplicationEnvironmentConfiguration(
                                applicationId,
                                environmentId,
                                mapper.createObjectNode().put("owner", "application"),
                                now));
        workspacePublishers.saveAndFlush(
                new WorkspaceEnvironmentConfigurationPublisher(
                        workspace,
                        publisherIds.get(0),
                        2,
                        mapper.createObjectNode().put("channel", "a")));
        workspacePublishers.saveAndFlush(
                new WorkspaceEnvironmentConfigurationPublisher(
                        workspace,
                        publisherIds.get(1),
                        1,
                        mapper.createObjectNode().put("channel", "b")));
        applicationPublishers.saveAndFlush(
                new ApplicationEnvironmentConfigurationPublisher(
                        application,
                        publisherIds.get(0),
                        1,
                        mapper.createObjectNode().put("channel", "c")));
        assertThat(workspacePublishers.findByConfigurationIdOrderBySortOrderAsc(workspace.getId()))
                .extracting(WorkspaceEnvironmentConfigurationPublisher::getSortOrder)
                .containsExactly(1, 2);
        assertThat(workspace.getVersion()).isZero();
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            WorkspaceEnvironmentConfiguration stored =
                                    workspaces.findById(workspace.getId()).orElseThrow();
                            stored.updateSettings(
                                    mapper.createObjectNode().put("changed", true),
                                    now.plusSeconds(1));
                            workspaces.saveAndFlush(stored);
                        });
        assertThat(workspaces.findById(workspace.getId()).orElseThrow().getVersion()).isEqualTo(1L);
        assertThat(
                        applications
                                .findById(application.getId())
                                .orElseThrow()
                                .getSettings()
                                .get("owner")
                                .asString())
                .isEqualTo("application");
        assertThat(
                        jdbc.queryForObject(
                                "select json_extract(settings,'$.registered') from workspaces where"
                                        + " id=?",
                                String.class,
                                workspaceId))
                .isEqualTo("true");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from flyway_schema_history where version='84' and"
                                        + " success=true",
                                Integer.class))
                .isEqualTo(1);
    }

    @Test
    void databaseRejectsDuplicateOwnerEnvironmentPublisherOrderAndBrokenReferences() {
        seed();
        Long workspaceId =
                jdbc.queryForObject(
                        "select id from workspaces where name='Configuration owner'", Long.class);
        Long environmentId =
                jdbc.queryForObject(
                        "select id from environments where name='Configuration environment'",
                        Long.class);
        List<String> publishers =
                jdbc.queryForList("select code from publishers order by code", String.class);
        WorkspaceEnvironmentConfiguration configuration =
                workspaces.saveAndFlush(
                        new WorkspaceEnvironmentConfiguration(
                                workspaceId,
                                environmentId,
                                mapper.createObjectNode(),
                                LocalDateTime.now()));
        assertThatThrownBy(
                        () ->
                                workspaces.saveAndFlush(
                                        new WorkspaceEnvironmentConfiguration(
                                                workspaceId,
                                                environmentId,
                                                mapper.createObjectNode(),
                                                LocalDateTime.now())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        workspacePublishers.saveAndFlush(
                new WorkspaceEnvironmentConfigurationPublisher(
                        configuration, publishers.get(0), 1, mapper.createObjectNode()));
        String insert =
                "insert into"
                    + " workspace_environment_configuration_publishers(configuration_id,publisher_code,sort_order,parameters)"
                    + " values (?,?,?,'{}')";
        assertThatThrownBy(() -> jdbc.update(insert, configuration.getId(), publishers.get(0), 2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, configuration.getId(), publishers.get(1), 1))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, configuration.getId(), publishers.get(1), 0))
                .isInstanceOf(org.springframework.jdbc.UncategorizedSQLException.class)
                .hasMessageContaining("ck_workspace_env_config_order");
        assertThatThrownBy(() -> jdbc.update(insert, configuration.getId(), "UNKNOWN", 2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "delete from workspace_environment_configurations where"
                                                + " id=?",
                                        configuration.getId()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void applicationConstraintsAndQuarantineRetainTheOwnerEnvironmentReservation() {
        seed();
        Long applicationId =
                jdbc.queryForObject(
                        "select id from applications where name='Configuration app'", Long.class);
        Long environmentId =
                jdbc.queryForObject(
                        "select id from environments where name='Configuration environment'",
                        Long.class);
        List<String> publishers =
                jdbc.queryForList("select code from publishers order by code", String.class);
        ApplicationEnvironmentConfiguration configuration =
                applications.saveAndFlush(
                        new ApplicationEnvironmentConfiguration(
                                applicationId,
                                environmentId,
                                mapper.createObjectNode(),
                                LocalDateTime.now()));
        applicationPublishers.saveAndFlush(
                new ApplicationEnvironmentConfigurationPublisher(
                        configuration, publishers.get(0), 1, mapper.createObjectNode()));
        Long configurationId = configuration.getId();
        String insert =
                "insert into"
                    + " application_environment_configuration_publishers(configuration_id,publisher_code,sort_order,parameters)"
                    + " values (?,?,?,'{}')";
        assertThatThrownBy(() -> jdbc.update(insert, configurationId, publishers.get(0), 2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, configurationId, publishers.get(1), 1))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, configurationId, publishers.get(1), 0))
                .isInstanceOf(org.springframework.jdbc.UncategorizedSQLException.class)
                .hasMessageContaining("ck_application_env_config_order");
        assertThatThrownBy(() -> jdbc.update(insert, configurationId, "UNKNOWN", 2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        configuration.inactivate(LocalDateTime.now());
        applications.saveAndFlush(configuration);
        configuration = applications.findById(configuration.getId()).orElseThrow();
        configuration.quarantine(LocalDateTime.now());
        applications.saveAndFlush(configuration);
        assertThat(
                        applications
                                .findById(configuration.getId())
                                .orElseThrow()
                                .getLifecycle()
                                .value())
                .isEqualTo("QUARANTINED");
        assertThatThrownBy(
                        () ->
                                applications.saveAndFlush(
                                        new ApplicationEnvironmentConfiguration(
                                                applicationId,
                                                environmentId,
                                                mapper.createObjectNode(),
                                                LocalDateTime.now())))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(
                        applicationPublishers.findByConfigurationIdOrderBySortOrderAsc(
                                configuration.getId()))
                .hasSize(1);
    }

    @Test
    void staleDetachedUpdatesCannotOverwriteAnAlreadyCommittedVersion() {
        seed();
        Long workspaceId =
                jdbc.queryForObject(
                        "select id from workspaces where name='Configuration owner'", Long.class);
        Long environmentId =
                jdbc.queryForObject(
                        "select id from environments where name='Configuration environment'",
                        Long.class);
        WorkspaceEnvironmentConfiguration configuration =
                workspaces.saveAndFlush(
                        new WorkspaceEnvironmentConfiguration(
                                workspaceId,
                                environmentId,
                                mapper.createObjectNode(),
                                LocalDateTime.now()));
        WorkspaceEnvironmentConfiguration first =
                workspaces.findById(configuration.getId()).orElseThrow();
        WorkspaceEnvironmentConfiguration stale =
                workspaces.findById(configuration.getId()).orElseThrow();
        first.updateSettings(mapper.createObjectNode().put("writer", "first"), LocalDateTime.now());
        workspaces.saveAndFlush(first);
        stale.updateSettings(mapper.createObjectNode().put("writer", "stale"), LocalDateTime.now());
        assertThatThrownBy(() -> workspaces.saveAndFlush(stale))
                .isInstanceOf(
                        org.springframework.orm.ObjectOptimisticLockingFailureException.class);
        assertThat(
                        workspaces
                                .findById(configuration.getId())
                                .orElseThrow()
                                .getSettings()
                                .get("writer")
                                .asString())
                .isEqualTo("first");
    }

    private void seed() {
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
                        + " values ('WORKSPACE','Workspace',1,true,'{}')");
        jdbc.update(
                "insert ignore into type_authorizations(code,label,sort_order,is_active,settings)"
                        + " values ('DEV','DEV',1,true,'{}')");
        jdbc.update(
                "insert ignore into"
                    + " environment_types(code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at)"
                    + " values"
                    + " ('DEFAULT','Default','Default',true,true,false,'ACTIVE',1,now(6),now(6))");
        jdbc.update(
                "insert into"
                    + " workspaces(version,identifier,workspace_type_code,name,description,requester,acronym,settings,email_group,onboarding,lifecycle_code,created_at,updated_at)"
                    + " values (0,uuid(),'MANAGER','Configuration owner','Configuration owner"
                    + " description','requester','CONF','{\"registered\":true}','test@example.com',false,'ACTIVE',now(6),now(6))");
        Long workspaceId =
                jdbc.queryForObject(
                        "select id from workspaces where name='Configuration owner'", Long.class);
        jdbc.update(
                "insert into"
                    + " applications(version,identifier,workspace_id,name,alias,acronym,application_scope_code,settings,lifecycle_code,created_at,updated_at)"
                    + " values (0,uuid(),?,'Configuration"
                    + " app','app','APP','BACKEND','{}','ACTIVE',now(6),now(6))",
                workspaceId);
        jdbc.update(
                "insert into"
                    + " environments(version,identifier,workspace_id,environment_type_code,name,description,authorization_type_code,settings,sort_order,lifecycle_code,created_at,updated_at)"
                    + " values (0,uuid(),null,'DEFAULT','Configuration environment','Configuration"
                    + " environment','DEV','{}',1,'ACTIVE',now(6),now(6))");
        for (String code : List.of("WEB_SOCKET", "KAFKA"))
            jdbc.update(
                    "insert into publishers(code,label,sort_order,is_active,settings) values"
                        + " (?,?,1,true,'{\"scope\":\"WORKSPACE\"}')",
                    code,
                    code);
    }
}
