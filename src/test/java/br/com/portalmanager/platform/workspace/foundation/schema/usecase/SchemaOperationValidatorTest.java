package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaOperationValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaVersionOperationValidator;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SchemaOperationValidatorTest {
    private final SchemaOperationValidator schemas = new SchemaOperationValidator();
    private final SchemaVersionOperationValidator versions = new SchemaVersionOperationValidator();

    @Test
    void rejectsInvalidCodeBeforeCreatingSchema() {
        assertThatThrownBy(() -> schemas.validateCreateInput(
                new CreateSchemaInput("JSON", "INVALID_CODE", "My schema", null, null, "v1", null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Schema code must use lowercase kebab-case");
    }

    @Test
    void rejectsWorkspaceScopeWithoutWorkspaceId() {
        assertThatThrownBy(() -> schemas.validateOwnership(SchemaScopeTypeCode.workspace(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Workspace id is required for WORKSPACE schema");
    }

    @Test
    void rejectsEditingPublishedVersionBeforeChangingDefinition() {
        Schema schema = new Schema("JSON", SchemaScopeTypeCode.platform(), null,
                "settings", "Settings", null, LocalDateTime.now());
        SchemaVersion version = new SchemaVersion(schema, 1, "v1", "{}",
                SchemaVersionStatusTypeCode.draft(), LocalDateTime.now());
        version.publish();

        assertThatThrownBy(() -> versions.validateDraftUpdate(version, "{\"type\":\"object\"}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Published schema version is immutable");
    }
}
