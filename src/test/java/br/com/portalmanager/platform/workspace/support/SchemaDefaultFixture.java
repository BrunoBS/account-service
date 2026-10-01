package br.com.portalmanager.platform.workspace.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** Recreates the persisted schema fallback and Golden resource contracts after test database cleanup. */
public final class SchemaDefaultFixture {
    private SchemaDefaultFixture() { }

    public static void seed(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT IGNORE INTO type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}'),
                       ('INACTIVE', 'Inactive', 'Inactive lifecycle', 2, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO type_schema_scopes
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('PLATFORM', 'Platform', 'Platform scope', 1, true, '{}'),
                       ('WORKSPACE', 'Workspace', 'Workspace scope', 2, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO type_schema_version_status
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('DRAFT', 'Draft', 'Draft schema version', 1, true, '{}'),
                       ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_definitions
                    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', 'default', 'Default', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                SELECT UUID(), id, 1, 'Default', '{"type":"object","additionalProperties":true}',
                       'PUBLISHED', CURRENT_TIMESTAMP
                FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default'
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_configuration
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
        jdbc.update("""
                INSERT IGNORE INTO schema_definitions
                    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', 'workspace-resource', 'Workspace Resource', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                SELECT UUID(), sd.id, 1, 'Workspace Resource v1',
                       '{
                          "type":"object",
                          "additionalProperties":false,
                          "required":["version","workspaceType","name","description","requester","acronym","emailGroup","approvers"],
                          "properties":{
                            "version":{"type":"integer","minimum":0},
                            "workspaceType":{"type":"string","minLength":1,"maxLength":50,"pattern":"^[A-Z][A-Z0-9_-]*$"},
                            "name":{"type":"string","minLength":3,"maxLength":100,"pattern":"^(?! ).*(?<! )$"},
                            "description":{"type":"string","minLength":10,"maxLength":500,"pattern":"^(?! ).*(?<! )$"},
                            "requester":{"type":"string","minLength":5,"maxLength":255,"pattern":"^(?! ).*(?<! )$"},
                            "acronym":{"type":"string","minLength":1,"maxLength":5,"pattern":"^[A-Z0-9]+$"},
                            "authorizerGroup":{"type":["string","null"],"minLength":1,"maxLength":255,"pattern":"^(?! ).*(?<! )$"},
                            "settings":{"type":["object","null"],"additionalProperties":true},
                            "emailGroup":{"type":"string","minLength":3,"maxLength":320,"pattern":"^[^ @]+@[^ @]+[.][^ @]+$"},
                            "approvers":{"type":"array","minItems":1,"items":{"type":"object","additionalProperties":false,"required":["functional","email"],"properties":{"functional":{"type":"string","minLength":1,"maxLength":255,"pattern":"^(?! ).*(?<! )$"},"email":{"type":"string","minLength":3,"maxLength":320,"pattern":"^[^ @]+@[^ @]+[.][^ @]+$"}}}},
                            "tags":{"type":"array","uniqueItems":true,"items":{"type":"string","minLength":1,"maxLength":150,"pattern":"^[a-z0-9]+(-[a-z0-9]+)*$"}}
                          }
                        }',
                       'PUBLISHED', CURRENT_TIMESTAMP
                FROM schema_definitions sd
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'workspace-resource'
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_configuration
                    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code,
                     created_at, updated_at)
                SELECT 0, UUID(), 'WORKSPACE', 'workspace', sd.id, 'ACTIVE',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM schema_definitions sd
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'workspace-resource'
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_definitions
                    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', 'application-resource', 'Application Resource', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
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
        jdbc.update("""
                INSERT IGNORE INTO schema_configuration
                    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code,
                     created_at, updated_at)
                SELECT 0, UUID(), 'APPLICATION', 'application', sd.id, 'ACTIVE',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM schema_definitions sd
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'application-resource'
                """);
    }
}
