package br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository;

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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=76",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DynamicBundleRenameMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void preservesMessagesTranslatorsVersionsAndViewAcrossUpgrade() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "INSERT INTO"
                    + " platform_microservices(identifier,code,name,settings,lifecycle_code,created_at,updated_at)"
                    + " VALUES ('bundle-service','bundle-service','Bundle"
                    + " Service','{}','ACTIVE',NOW(6),NOW(6))");
        jdbc.update(
                "INSERT INTO"
                    + " messages(identifier,version,microservice_id,message_key,code,http_status,lifecycle_code,created_at,updated_at)"
                    + " SELECT"
                    + " 'bundle-message',3,id,'test.message','TEST-001',400,'ACTIVE',NOW(6),NOW(6)"
                    + " FROM platform_microservices WHERE identifier='bundle-service'");
        jdbc.update(
                "INSERT INTO"
                    + " message_translations(identifier,version,message_id,locale,title,detail,suggestion,lifecycle_code,created_at,updated_at)"
                    + " SELECT"
                    + " 'bundle-translator',7,id,'pt-BR','Title','Detail','Suggestion','ACTIVE',NOW(6),NOW(6)"
                    + " FROM messages WHERE identifier='bundle-message'");
        java.util.List<java.util.Map<String, Object>> messages =
                jdbc.queryForList("SELECT * FROM messages WHERE identifier='bundle-message'");
        java.util.List<java.util.Map<String, Object>> translators =
                jdbc.queryForList(
                        "SELECT * FROM message_translations WHERE identifier='bundle-translator'");
        Flyway upgrade =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("77")
                        .load();
        upgrade.migrate();
        assertThat(jdbc.queryForList("SELECT * FROM messages WHERE identifier='bundle-message'"))
                .isEqualTo(messages);
        assertThat(
                        jdbc.queryForList(
                                "SELECT * FROM message_translators WHERE"
                                        + " identifier='bundle-translator'"))
                .isEqualTo(translators);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT message FROM vw_dynamic_bundle_messages WHERE"
                                        + " message_key='bundle-service.test.message'",
                                String.class))
                .isEqualTo("Detail");
        jdbc.update("DELETE FROM messages WHERE identifier='bundle-message'");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM message_translators WHERE"
                                        + " identifier='bundle-translator'",
                                Integer.class))
                .isZero();
    }
}
