package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=83",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
            "platform.authorization.mode=MOCK",
            "platform.audit.enabled=false"
        })
class EnvironmentConfigurationUpgradeIT {
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void upgradeFromV83PreservesExistingWorkspaceSettingsAndVersion() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("83");
        jdbc.update(
                "insert ignore into type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " values ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "insert ignore into type_workspaces(code,label,sort_order,is_active,settings)"
                        + " values ('MANAGER','Manager',1,true,'{}')");
        String identifier = "2a55e118-a599-42f0-8528-06d11b2e287b";
        jdbc.update(
                """
                insert into workspaces(version,identifier,workspace_type_code,name,description,requester,acronym,settings,email_group,onboarding,lifecycle_code,created_at,updated_at)
                values (7,?,'MANAGER','Preserved owner','Preserved description','requester','PRES','{"registered":true,"limit":123}','test@example.com',false,'ACTIVE',now(6),now(6))
                """,
                identifier);
        jdbc.update(
                "insert ignore into type_resource_scopes(code,label,sort_order,is_active,settings)"
                    + " values"
                    + " ('WORKSPACE','Workspace',1,true,'{}'),('APPLICATION','Application',2,true,'{}')");
        for (String code : java.util.List.of("HTTP", "APPCONFIG")) {
            jdbc.update(
                    "insert into"
                        + " publishers(version,identifier,code,name,description,publisher_scope,deprecated,settings,lifecycle_code,created_at,updated_at)"
                        + " values (7,uuid(),?,?,?"
                        + " ,?,true,'{\"preserved\":true}','ACTIVE',now(),now())",
                    code,
                    code,
                    "Legacy publisher",
                    code.equals("HTTP") ? "WORKSPACE" : "APPLICATION");
        }
        java.util.List<Long> publisherIds =
                jdbc.queryForList("select id from publishers order by id", Long.class);
        jdbc.update(
                "insert ignore into type_schema_scopes(code,label,sort_order,is_active,settings)"
                    + " values ('PLATFORM','Platform',1,true,'{}')");
        jdbc.update(
                "insert ignore into"
                    + " type_schema_version_status(code,label,sort_order,is_active,settings) values"
                    + " ('PUBLISHED','Published',1,true,'{}')");
        jdbc.update(
                "insert into"
                    + " schema_definitions(version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)"
                    + " values"
                    + " (0,uuid(),'PLATFORM','publisher-resource','Publisher','ACTIVE',now(),now())");
        try {
            String legacy =
                    new org.springframework.core.io.ClassPathResource("schemas/publisher.json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8)
                            .replace("WEB_SOCKET", "HTTP")
                            .replace("APP_CONFIG", "APPCONFIG")
                            .replace("60000", "1234");
            jdbc.update(
                    "insert into"
                        + " schema_versions(identifier,schema_id,schema_version,version_name,definition,status,created_at)"
                        + " select uuid(),id,1,'Legacy customized',?,'PUBLISHED',now() from"
                        + " schema_definitions where code='publisher-resource'",
                    legacy);
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
        jdbc.update(
                "insert into"
                    + " schema_binding(version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)"
                    + " select 0,uuid(),'PUBLISHER','HTTP',id,'ACTIVE',now(),now() from"
                    + " schema_definitions where code='publisher-resource'");
        Flyway upgraded =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .load();
        upgraded.migrate();
        upgraded.validate();
        assertThat(upgraded.info().current().getVersion().getVersion()).isEqualTo("86");
        assertThat(
                        jdbc.queryForObject(
                                "select version from workspaces where identifier=?",
                                Long.class,
                                identifier))
                .isEqualTo(7L);
        assertThat(
                        jdbc.queryForObject(
                                "select json_extract(settings,'$.limit') from workspaces where"
                                        + " identifier=?",
                                String.class,
                                identifier))
                .isEqualTo("123");
        assertThat(jdbc.queryForList("select id from publishers order by id", Long.class))
                .isEqualTo(publisherIds);
        assertThat(jdbc.queryForList("select code from publishers order by id", String.class))
                .containsExactly("WEB_SOCKET", "APP_CONFIG");
        assertThat(jdbc.queryForList("select version from publishers order by id", Long.class))
                .containsExactly(8L, 8L);
        assertThat(
                        jdbc.queryForList(
                                "select json_extract(settings,'$.preserved') from publishers",
                                String.class))
                .containsOnly("true");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from schema_binding where"
                                    + " resource_type='PUBLISHER' and resource_code='WEB_SOCKET'",
                                Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForObject(
                                "select"
                                    + " json_extract(definition,'$.allOf[2].then.properties.settings.properties.timeout.maximum')"
                                    + " from schema_versions v join schema_definitions d on"
                                    + " d.id=v.schema_id where d.code='publisher-resource' order by"
                                    + " v.schema_version desc limit 1",
                                String.class))
                .isEqualTo("1234");
        assertThat(
                        jdbc.queryForObject(
                                "select"
                                    + " json_unquote(json_extract(definition,'$.allOf[2].if.properties.code.const'))"
                                    + " from schema_versions v join schema_definitions d on"
                                    + " d.id=v.schema_id where d.code='publisher-resource' order by"
                                    + " v.schema_version desc limit 1",
                                String.class))
                .isEqualTo("WEB_SOCKET");
        for (String code :
                java.util.List.of(
                        "workspace-environment-configuration",
                        "application-environment-configuration",
                        "environment-configuration-publisher-web-socket",
                        "environment-configuration-publisher-kafka",
                        "environment-configuration-publisher-app-config")) {
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from schema_binding b join schema_versions v"
                                        + " on b.schema_id=v.schema_id where"
                                        + " b.resource_type='PLATFORM' and b.resource_code=? and"
                                        + " b.lifecycle_code='ACTIVE' and v.status='PUBLISHED'",
                                    Integer.class,
                                    code))
                    .isEqualTo(1);
        }
        for (String table :
                java.util.List.of(
                        "workspace_environment_configurations",
                        "application_environment_configurations",
                        "workspace_environment_configuration_publishers",
                        "application_environment_configuration_publishers")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isZero();
        }
        try {
            String migration =
                    new org.springframework.core.io.ClassPathResource(
                                    "db/migration/V86__configuration_publisher_contracts.sql")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            String guard =
                    migration.substring(
                            migration.indexOf("CREATE PROCEDURE"), migration.indexOf("END$$") + 3);
            jdbc.update(
                    "insert into"
                        + " publishers(version,identifier,code,name,description,publisher_scope,deprecated,settings,lifecycle_code,created_at,updated_at)"
                        + " values"
                        + " (0,uuid(),'HTTP','Collision','Collision','WORKSPACE',false,'{}','ACTIVE',now(),now())");
            jdbc.execute(guard);
            try {
                org.assertj.core.api.Assertions.assertThatThrownBy(
                                () -> jdbc.execute("CALL validate_publisher_code_rename_v86()"))
                        .hasMessageContaining("Publisher code rename collision");
                assertThat(
                                jdbc.queryForObject(
                                        "select count(*) from publishers where code in"
                                            + " ('HTTP','WEB_SOCKET')",
                                        Integer.class))
                        .isEqualTo(2);
            } finally {
                jdbc.execute("DROP PROCEDURE validate_publisher_code_rename_v86");
            }
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
