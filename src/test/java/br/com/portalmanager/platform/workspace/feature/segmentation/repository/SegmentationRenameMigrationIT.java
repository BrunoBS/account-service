package br.com.portalmanager.platform.workspace.feature.segmentation.repository;

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
            "spring.flyway.target=78",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SegmentationRenameMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void preservesCategoryDataAndMandatoryParentAndPerSegmentUniqueness() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "INSERT INTO segment(identifier,code,name,lifecycle_code,created_at,updated_at)"
                    + " VALUES"
                    + " ('segment-a','segment-a','A','ACTIVE',NOW(),NOW()),('segment-b','segment-b','B','ACTIVE',NOW(),NOW())");
        jdbc.update(
                "INSERT INTO"
                    + " segment_category(identifier,version,segment_id,code,name,lifecycle_code,created_at,updated_at)"
                    + " SELECT 'category-a',7,id,'same-code','Name','ACTIVE',NOW(),NOW() FROM"
                    + " segment WHERE identifier='segment-a'");
        assertThat(
                        jdbc.queryForObject(
                                "SELECT COUNT(*) FROM segment_category c LEFT JOIN segment s ON"
                                        + " s.id=c.segment_id WHERE s.id IS NULL",
                                Integer.class))
                .isZero();
        assertThat(
                        jdbc.queryForList(
                                "SELECT segment_id,code FROM segment_category GROUP BY"
                                        + " segment_id,code HAVING COUNT(*)>1"))
                .isEmpty();
        java.util.List<java.util.Map<String, Object>> before =
                jdbc.queryForList("SELECT * FROM segment_category");
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("79")
                .load()
                .migrate();
        assertThat(jdbc.queryForList("SELECT * FROM category")).isEqualTo(before);
        jdbc.update(
                "INSERT INTO"
                    + " category(identifier,segment_id,code,name,lifecycle_code,created_at,updated_at)"
                    + " SELECT 'category-b',id,'same-code','Name','ACTIVE',NOW(),NOW() FROM segment"
                    + " WHERE identifier='segment-b'");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " category(identifier,segment_id,code,name,lifecycle_code,created_at,updated_at)"
                                            + " SELECT"
                                            + " 'duplicate',id,'same-code','Name','ACTIVE',NOW(),NOW()"
                                            + " FROM segment WHERE identifier='segment-a'"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE category SET segment_id=NULL WHERE"
                                                + " identifier='category-a'"))
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "UPDATE category SET segment_id=999999 WHERE"
                                                + " identifier='category-a'"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
