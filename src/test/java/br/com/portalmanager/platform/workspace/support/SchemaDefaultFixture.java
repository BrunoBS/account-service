package br.com.portalmanager.platform.workspace.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

/**
 * Recreates the persisted schema fallback and Golden resource contracts after test database
 * cleanup.
 */
public final class SchemaDefaultFixture {

    private SchemaDefaultFixture() {}

    public static void seed(JdbcTemplate jdbc) {
        jdbc.update(
                "INSERT IGNORE INTO"
                    + " type_parameter_key_types(code,label,sort_order,is_active,settings) VALUES"
                    + " ('CONFIGURATION','Configuration',1,true,'{}'),('EXPRESSION','Expression',2,true,'{}')");
        jdbc.update(
                "INSERT IGNORE INTO"
                        + " type_parameter_key_statuses(code,label,sort_order,is_active,settings)"
                        + " VALUES"
                        + " ('DRAFT','Draft',1,true,'{}'),('PUBLISHED','Published',2,true,'{}')");
        jdbc.update(
                """
                INSERT IGNORE INTO type_onboardings
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('WORKSPACE_REGISTRATION', 'Cadastro do workspace',
                        'Cadastro dos dados principais do workspace.', 1, true,
                        '{"orientation":"Revise os dados principais do workspace."}'),
                       ('WORKSPACE_FIRST_ENVIRONMENT', 'Configuração do ambiente do workspace',
                        'Configuração do primeiro ambiente do workspace.', 2, true,
                        '{"orientation":"Configure o ambiente padrão do workspace quando a configuração de ambientes estiver disponível."}'),
                       ('FIRST_APPLICATION_REGISTRATION', 'Cadastro da primeira aplicação',
                        'Cadastro da primeira aplicação vinculada ao workspace.', 3, true,
                        '{"orientation":"Cadastre a primeira aplicação vinculada ao workspace."}'),
                       ('APPLICATION_FIRST_ENVIRONMENT', 'Configuração do ambiente da aplicação',
                        'Configuração do primeiro ambiente da aplicação.', 4, true,
                        '{"orientation":"Configure o ambiente padrão da aplicação quando a configuração de ambientes estiver disponível."}')
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO type_sharing_statuses
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('PENDING', 'Pendente', 'Solicitação aguardando decisão do proprietário.', 1, true, '{}'),
                       ('APPROVED', 'Aprovada', 'Participação autorizada enquanto contrato e mapeamento estiverem aptos.', 2, true, '{}'),
                       ('REJECTED', 'Rejeitada', 'Solicitação rejeitada pelo proprietário.', 3, true, '{}'),
                       ('REVOKED', 'Revogada', 'Participação aprovada e posteriormente revogada.', 4, true, '{}')
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO type_publication_modes
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('AUTOMATIC', 'Automática', 'Propaga publicações para os destinos mapeados.', 1, true, '{}'),
                       ('MANUAL', 'Manual', 'Permite publicação explícita pelo destino.', 2, true, '{}')
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}'),
                       ('INACTIVE', 'Inactive', 'Inactive lifecycle', 2, true, '{}')
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO type_schema_scopes
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('PLATFORM', 'Platform', 'Platform scope', 1, true, '{}'),
                       ('WORKSPACE', 'Workspace', 'Workspace scope', 2, true, '{}')
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO type_schema_version_status
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('DRAFT', 'Draft', 'Draft schema version', 1, true, '{}'),
                       ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}')
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO schema_definitions
                    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', 'default', 'Default', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                SELECT UUID(), id, 1, 'Default', '{"type":"object","additionalProperties":true}',
                       'PUBLISHED', CURRENT_TIMESTAMP
                FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default'
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO schema_binding
                    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code,
                     created_at, updated_at)
                SELECT 0, UUID(), resource_types.resource_type, 'DEFAULT', sd.id, 'ACTIVE',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM schema_definitions sd
                CROSS JOIN (
                    SELECT 'WORKSPACE' AS resource_type UNION ALL
                    SELECT 'APPLICATION' UNION ALL
                    SELECT 'ENVIRONMENT' UNION ALL
                    SELECT 'PUBLISHER' UNION ALL
                    SELECT 'FEATURE' UNION ALL
                    SELECT 'MICROSERVICE' UNION ALL
                    SELECT 'CATALOG'
                ) resource_types
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'default'
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO schema_definitions
                    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', 'application-resource', 'Application Resource', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                SELECT UUID(), sd.id, 1, 'Application Resource v1',
                       '{
                          "type":"object",
                          "additionalProperties":false,
                          "required":["name","alias","acronym","applicationScope"],
                          "properties":{
                            "version":{"type":"integer","minimum":0},
                            "name":{"type":"string","minLength":3,"maxLength":100},
                            "alias":{"type":"string","minLength":1,"maxLength":100},
                            "acronym":{"type":"string","minLength":1,"maxLength":20},
                            "applicationScope":{"type":"string","enum":["BACKEND","FRONTEND","SHARED"]},
                            "authorizerGroup":{"type":"string","minLength":1,"maxLength":255},
                            "settings":{"type":"object","additionalProperties":true},
                            "tags":{
                              "type":"array",
                              "uniqueItems":true,
                              "items":{
                                "type":"string",
                                "minLength":1,
                                "maxLength":150,
                                "pattern":"^[a-z0-9]+(-[a-z0-9]+)*$"
                              }
                            }
                          }
                        }',
                       'PUBLISHED', CURRENT_TIMESTAMP
                FROM schema_definitions sd
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'application-resource'
                """);
        jdbc.update(
                """
                INSERT IGNORE INTO schema_binding
                    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code,
                     created_at, updated_at)
                SELECT 0, UUID(), 'APPLICATION', 'application', sd.id, 'ACTIVE',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM schema_definitions sd
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'application-resource'
                """);
        jdbc.execute(
                (ConnectionCallback<Void>)
                        connection -> {
                            executeSchemaSeed(
                                    connection, "db/migration/V42__workspace_resource_schema.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V50__publisher_resource_schema_and_limits.sql");
                            executeSchemaSeed(
                                    connection, "db/migration/V52__environment_resource_schemas.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V55__environment_type_compatibility_identifiers.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V57__environment_type_code_schemas.sql");
                            executeSchemaSeed(
                                    connection, "db/migration/V58__message_resource_schemas.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V61__onboarding_phase_settings_schema.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V62__microservice_resource_schema.sql");
                            executeSchemaSeed(
                                    connection, "db/migration/V63__feature_resource_schema.sql");
                            executeSchemaSeed(
                                    connection, "db/migration/V64__context_resource_schema.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V65__separate_feature_context_resource_schema.sql");
                            executeSchemaSeed(
                                    connection, "db/migration/V67__segment_resource_schemas.sql");
                            executeSchemaSeed(
                                    connection,
                                    "db/migration/V85__environment_configuration_schemas.sql");
                            executeSchemaSeed(connection, "migration/dynamic-bundle-schema-seed.sql");
                            executeSchemaSeed(connection, "migration/service-context-schema-seed.sql");
                            executeSchemaSeed(connection, "migration/segmentation-schema-seed.sql");
                            return null;
                        });
        try {
            seedDynamicParameterSchemas(jdbc);
            jdbc.update(
                    """
                    INSERT IGNORE INTO schema_versions
                        (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                    SELECT UUID(), id, 2, 'Publisher Resource v2', ?, 'PUBLISHED', CURRENT_TIMESTAMP
                    FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'publisher-resource'
                    """,
                    new ClassPathResource("schemas/publisher.json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            jdbc.update(
                    """
                    INSERT IGNORE INTO schema_versions
                        (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                    SELECT UUID(), id, 2, 'Environment Type Resource v2', ?, 'PUBLISHED', CURRENT_TIMESTAMP
                    FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'environment-type-resource'
                    """,
                    new ClassPathResource("schemas/environment-type.json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            jdbc.update(
                    """
                    INSERT IGNORE INTO schema_versions
                        (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                    SELECT UUID(), id, 4, 'Application Resource v4', ?, 'PUBLISHED', CURRENT_TIMESTAMP
                    FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'application-resource'
                    """,
                    new ClassPathResource("schemas/application.json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            jdbc.update(
                    """
                    INSERT IGNORE INTO schema_versions
                        (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                    SELECT UUID(), id, 2, 'Workspace Resource v2', ?, 'PUBLISHED', CURRENT_TIMESTAMP
                    FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'workspace-resource'
                    """,
                    new ClassPathResource("schemas/workspace.json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(
                    "Cannot load a reference resource schema", exception);
        }
    }

    private static void executeSchemaSeed(Connection connection, String path)
            throws java.sql.SQLException {
        ClassPathResource resource = new ClassPathResource(path);
        try (var input = resource.getInputStream()) {
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("schema_configuration", "schema_binding");
            ScriptUtils.executeSqlScript(
                    connection,
                    new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8), path));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load historical schema seed: " + path, exception);
        }
    }

    private static void seedDynamicParameterSchemas(JdbcTemplate jdbc) throws java.io.IOException {
        for (String resource :
                java.util.List.of(
                        "dynamic-parameter",
                        "dynamic-parameter-publication",
                        "parameter-key-type",
                        "parameter-key-status-type")) {
            boolean catalog = resource.startsWith("parameter-key-");
            String code =
                    catalog
                            ? resource + "-settings"
                            : resource.equals("dynamic-parameter")
                                    ? resource + "-resource"
                                    : resource;
            int version = resource.equals("dynamic-parameter") ? 5 : 2;
            jdbc.update(
                    "INSERT IGNORE INTO schema_definitions"
                        + " (version,identifier,scope_code,code,name,lifecycle_code,created_at,updated_at)"
                        + " VALUES (0,UUID(),'PLATFORM',?,?,'ACTIVE',NOW(),NOW())",
                    code,
                    code);
            String definition =
                    new ClassPathResource(
                                    "schemas/"
                                            + (catalog ? resource + "-settings" : resource)
                                            + ".json")
                            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
            jdbc.update(
                    "INSERT IGNORE INTO schema_versions"
                        + " (identifier,schema_id,schema_version,version_name,definition,status,created_at)"
                        + " SELECT UUID(),id,?,?,?,'PUBLISHED',NOW() FROM schema_definitions WHERE"
                        + " scope_code='PLATFORM' AND code=?",
                    version,
                    code + " v" + version,
                    definition,
                    code);
            jdbc.update(
                    "INSERT IGNORE INTO schema_binding"
                        + " (version,identifier,resource_type,resource_code,schema_id,lifecycle_code,created_at,updated_at)"
                        + " SELECT 0,UUID(),?,?,id,'ACTIVE',NOW(),NOW() FROM schema_definitions"
                        + " WHERE scope_code='PLATFORM' AND code=?",
                    catalog ? "CATALOG" : "PLATFORM",
                    resource,
                    code);
        }
    }
}
