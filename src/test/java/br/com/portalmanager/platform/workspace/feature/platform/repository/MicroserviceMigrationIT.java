package br.com.portalmanager.platform.workspace.feature.platform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PlatformIntegrationTest
@Import(MigrationSchemaValidationTestConfiguration.class)
@WithMySql
@TestPropertySource(
    properties = {
        "spring.flyway.target=18",
        "platform.schema-validation.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MicroserviceMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void shouldPreserveMicroserviceFeatureAndMessageRelationships() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("18");

        jdbc.update(
            """
            insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
            values ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
            on duplicate key update code = values(code)
            """
        );
        jdbc.update(
            """
            insert into platform_services
                (version, identifier, code, name, lifecycle_code, created_at, updated_at)
            values (0, '11111111-1111-1111-1111-111111111111',
                    'audit-service', 'Audit Service', 'ACTIVE', now(), now())
            """
        );
        jdbc.update(
            """
            insert into platform_features
                (version, identifier, code, name, service_id, lifecycle_code, settings, created_at, updated_at)
            select 0, '22222222-2222-2222-2222-222222222222', 'audit', 'Audit',
                   id, 'ACTIVE', '{}', now(), now()
              from platform_services where code = 'audit-service'
            """
        );
        jdbc.update(
            """
            insert into messages
                (version, identifier, service_id, message_key, code, http_status,
                 lifecycle_code, created_at, updated_at)
            select 0, '33333333-3333-3333-3333-333333333333', id,
                   'audit.missing', 'AUDIT-0001', 404, 'ACTIVE', now(), now()
              from platform_services where code = 'audit-service'
            """
        );
        jdbc.update(
            """
            insert into message_translations
                (version, identifier, message_id, locale, title, detail, suggestion,
                 lifecycle_code, created_at, updated_at)
            select 0, '44444444-4444-4444-4444-444444444444', id,
                   'pt-BR', 'Não encontrado', 'Auditoria não encontrada', 'Revise o identificador',
                   'ACTIVE', now(), now()
              from messages where code = 'AUDIT-0001'
            """
        );

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("19").load().migrate();

        assertThat(
            jdbc.queryForObject(
                """
                select count(*) from platform_features f
                  join platform_microservices s on s.id = f.microservice_id
                 where s.code = 'audit-service' and f.code = 'audit'
                """,
                Integer.class
            )
        ).isEqualTo(1);
        assertThat(
            jdbc.queryForObject(
                """
                select count(*) from messages m
                  join platform_microservices s on s.id = m.microservice_id
                 where s.code = 'audit-service' and m.code = 'AUDIT-0001'
                """,
                Integer.class
            )
        ).isEqualTo(1);
        assertThat(
            jdbc.queryForObject(
                """
                select message from vw_platform_messages
                 where message_key = 'audit-service.audit.missing'
                """,
                String.class
            )
        ).isEqualTo("Auditoria não encontrada");
        assertThat(
            jdbc.queryForObject(
                """
                select microservice_code from vw_feature_runtime_config
                 where feature_code = 'audit'
                """,
                String.class
            )
        ).isEqualTo("audit-service");
        assertThat(tableCount(jdbc, "platform_services")).isZero();
        assertThat(tableCount(jdbc, "type_languages")).isZero();
        assertThat(tableCount(jdbc, "type_infrastructures")).isZero();
    }

    private Integer tableCount(JdbcTemplate jdbc, String table) {
        return jdbc.queryForObject(
            """
            select count(*) from information_schema.tables
             where table_schema = database() and table_name = ?
            """,
            Integer.class,
            table
        );
    }
}
