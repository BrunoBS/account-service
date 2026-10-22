package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=86",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
            "platform.authorization.mode=MOCK",
            "platform.audit.enabled=false"
        })
class EnvironmentConfigurationUpgradeIT {
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void migratesCatalogAndBothExistingConfigurationAssociationsWithoutLosingData() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("86");
        for (String code : List.of("ACTIVE", "INACTIVE", "QUARANTINED"))
            jdbc.update(
                    "insert ignore into type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " values (?,?,1,true,'{}')",
                    code,
                    code);
        for (String code : List.of("WORKSPACE", "APPLICATION"))
            jdbc.update(
                    "insert ignore into"
                        + " type_resource_scopes(code,label,sort_order,is_active,settings) values"
                        + " (?,?,1,true,'{}')",
                    code,
                    code);
        jdbc.update(
                "insert ignore into type_schema_scopes(code,label,sort_order,is_active,settings)"
                    + " values ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "insert ignore into"
                    + " type_schema_version_status(code,label,sort_order,is_active,settings) values"
                    + " ('PUBLISHED','Published',1,true,'{}')");
        jdbc.update(
                "insert ignore into type_workspaces(code,label,sort_order,is_active,settings)"
                    + " values ('MANAGER','Manager',1,true,'{}')");
        jdbc.update(
                "insert ignore into"
                    + " type_application_scopes(code,label,sort_order,is_active,settings) values"
                    + " ('BACKEND','Backend',1,true,'{}')");
        jdbc.update(
                "insert ignore into type_authorizations(code,label,sort_order,is_active,settings)"
                    + " values ('DEV','Dev',1,true,'{}')");
        jdbc.update(
                "insert into"
                    + " publishers(version,identifier,code,name,description,publisher_scope,deprecated,settings,lifecycle_code,created_at,updated_at)"
                    + " values (7,'11111111-1111-1111-1111-111111111111','KAFKA','Kafka','Legacy"
                    + " kafka','WORKSPACE',true,'{\"topic\":\"events\",\"address\":\"broker\"}','ACTIVE',now(),now()),(3,uuid(),'WEB_SOCKET','WebSocket','Quarantined"
                    + " legacy','WORKSPACE',false,'{}','QUARANTINED',now(),now())");
        Long publisherId =
                jdbc.queryForObject("select id from publishers where code='KAFKA'", Long.class);
        jdbc.update(
                "insert into"
                    + " workspaces(version,identifier,workspace_type_code,name,description,requester,acronym,settings,email_group,onboarding,lifecycle_code,created_at,updated_at)"
                    + " values (7,uuid(),'MANAGER','Migration workspace','Migration"
                    + " workspace','requester','MIG','{\"preserved\":true}','migration@example.com',false,'ACTIVE',now(),now())");
        Long workspaceId = jdbc.queryForObject("select id from workspaces", Long.class);
        jdbc.update(
                "insert into"
                    + " applications(version,identifier,workspace_id,name,alias,acronym,application_scope_code,settings,lifecycle_code,created_at,updated_at)"
                    + " values (0,uuid(),?,'Migration"
                    + " app','app','APP','BACKEND','{}','ACTIVE',now(),now())",
                workspaceId);
        jdbc.update(
                "insert into"
                    + " environment_types(code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at)"
                    + " values"
                    + " ('DEFAULT','Default','Default',true,true,false,'ACTIVE',1,now(),now())");
        jdbc.update(
                "insert into"
                    + " environments(version,identifier,workspace_id,environment_type_code,name,description,authorization_type_code,settings,sort_order,lifecycle_code,created_at,updated_at)"
                    + " values (0,uuid(),null,'DEFAULT','Migration environment','Migration"
                    + " environment','DEV','{}',1,'ACTIVE',now(),now())");
        for (String owner : List.of("workspace", "application")) {
            jdbc.update(
                    "insert into "
                            + owner
                            + "_environment_configurations(version,identifier,"
                            + owner
                            + "_id,environment_id,lifecycle_code,settings,created_at,updated_at)"
                            + " select 5,uuid(),o.id,e.id,'ACTIVE','{\"own\":\""
                            + owner
                            + "\"}',now(),now() from "
                            + owner
                            + "s o cross join environments e");
            jdbc.update(
                    "insert into "
                            + owner
                            + "_environment_configuration_publishers(configuration_id,publisher_id,sort_order,parameters)"
                            + " select id,?,3,'{\"nested\":{\"preserved\":true}}' from "
                            + owner
                            + "_environment_configurations",
                    publisherId);
        }
        Flyway upgraded =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .load();
        upgraded.migrate();
        upgraded.validate();
        assertThat(upgraded.info().current().getVersion().getVersion()).isEqualTo("87");
        assertThat(jdbc.queryForList("select code from publishers order by code", String.class))
                .containsExactly("APP_CONFIG", "KAFKA", "WEB_SOCKET");
        assertThat(
                        jdbc.queryForObject(
                                "select identifier from publishers where code='KAFKA'",
                                String.class))
                .isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(
                        jdbc.queryForObject(
                                "select version from publishers where code='KAFKA'", Long.class))
                .isEqualTo(7L);
        assertThat(
                        jdbc.queryForObject(
                                "select json_unquote(json_extract(settings,'$.scope')) from"
                                    + " publishers where code='KAFKA'",
                                String.class))
                .isEqualTo("WORKSPACE");
        assertThat(
                        jdbc.queryForObject(
                                "select json_unquote(json_extract(settings,'$.topic')) from"
                                    + " publishers where code='KAFKA'",
                                String.class))
                .isEqualTo("events");
        assertThat(
                        jdbc.queryForObject(
                                "select is_active from publishers where code='WEB_SOCKET'",
                                Boolean.class))
                .isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "select json_unquote(json_extract(settings,'$.scope')) from"
                                    + " publishers where code='APP_CONFIG'",
                                String.class))
                .isEqualTo("APPLICATION");
        for (String owner : List.of("workspace", "application")) {
            assertThat(
                            jdbc.queryForObject(
                                    "select publisher_code from "
                                            + owner
                                            + "_environment_configuration_publishers",
                                    String.class))
                    .isEqualTo("KAFKA");
            assertThat(
                            jdbc.queryForObject(
                                    "select publisher_id from "
                                            + owner
                                            + "_environment_configuration_publishers",
                                    Long.class))
                    .isEqualTo(publisherId);
            assertThat(
                            jdbc.queryForObject(
                                    "select json_extract(parameters,'$.nested.preserved') from "
                                            + owner
                                            + "_environment_configuration_publishers",
                                    String.class))
                    .isEqualTo("true");
            assertThat(
                            jdbc.queryForObject(
                                    "select version from " + owner + "_environment_configurations",
                                    Long.class))
                    .isEqualTo(5L);
        }
    }
}
