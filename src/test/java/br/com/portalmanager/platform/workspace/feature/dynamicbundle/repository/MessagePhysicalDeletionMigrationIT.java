package br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=58",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MessagePhysicalDeletionMigrationIT {

    @Autowired private DataSource dataSource;

    @Test
    void preservesLegacyRowsAsInactiveRejectsQuarantineAndKeepsDatabaseCascade() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings)"
                    + " VALUES"
                    + " ('ACTIVE','Active',1,true,'{}'),('INACTIVE','Inactive',2,true,'{}'),('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update(
                "INSERT INTO platform_microservices"
                    + " (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES"
                    + " ('review-migration-service','review-service','Review"
                    + " service','{}','ACTIVE',NOW(6),NOW(6))");
        for (String lifecycle : List.of("ACTIVE", "INACTIVE", "QUARANTINED")) {
            jdbc.update(
                    "INSERT INTO messages"
                        + " (identifier,microservice_id,message_key,code,http_status,lifecycle_code,created_at,updated_at)"
                        + " SELECT ?,id,?,?,400,?,NOW(6),NOW(6) FROM platform_microservices",
                    "message-" + lifecycle,
                    "review." + lifecycle,
                    "REVIEW-" + lifecycle,
                    lifecycle);
            jdbc.update(
                    "INSERT INTO message_translations"
                        + " (identifier,message_id,locale,title,detail,suggestion,lifecycle_code,created_at,updated_at)"
                        + " SELECT ?,id,'pt-BR','Title','Detail','Suggestion',?,NOW(6),NOW(6) FROM"
                        + " messages WHERE identifier=?",
                    "translation-" + lifecycle,
                    lifecycle,
                    "message-" + lifecycle);
        }
        Flyway upgrade =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .target("59")
                        .load();
        upgrade.migrate();
        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("59");
        for (String table : List.of("messages", "message_translations")) {
            assertThat(
                            jdbc.queryForList(
                                    "SELECT lifecycle_code FROM " + table + " ORDER BY id",
                                    String.class))
                    .containsExactly("ACTIVE", "INACTIVE", "INACTIVE");
            assertThat(
                            jdbc.queryForList(
                                    "SELECT version FROM " + table + " ORDER BY id", Long.class))
                    .containsExactly(0L, 0L, 1L);
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "UPDATE "
                                                    + table
                                                    + " SET lifecycle_code='QUARANTINED'"))
                    .isInstanceOf(DataAccessException.class)
                    .rootCause()
                    .isInstanceOfSatisfying(
                            SQLException.class,
                            cause -> assertThat(cause.getErrorCode()).isEqualTo(3819));
        }
        assertThat(
                        jdbc.queryForObject(
                                "SELECT title FROM message_translations WHERE"
                                        + " identifier='translation-QUARANTINED'",
                                String.class))
                .isEqualTo("Title");
        jdbc.update("DELETE FROM messages WHERE identifier='message-QUARANTINED'");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM message_translations WHERE"
                                        + " identifier='translation-QUARANTINED'",
                                Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_translations", Integer.class))
                .isEqualTo(2);
    }
}
