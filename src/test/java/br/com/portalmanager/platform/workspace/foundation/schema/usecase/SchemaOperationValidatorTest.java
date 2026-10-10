package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaOperationValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaVersionOperationValidator;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SchemaOperationValidatorTest {

    private final SchemaOperationValidator schemas = new SchemaOperationValidator();
    private final SchemaVersionOperationValidator versions = new SchemaVersionOperationValidator();

    @Test
    void rejectsInvalidCodeBeforeCreatingSchema() {
        assertThatThrownBy(() ->
            schemas.validateCreateInput(new CreateSchemaInput("INVALID_CODE", "My schema", null, null, "v1", null))
        ).isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsWorkspaceScopeWithoutWorkspaceId() {
        assertThatThrownBy(() -> schemas.validateOwnership(SchemaScopeTypeCode.workspace(), null)).isInstanceOf(
            ValidationException.class
        );
    }

    @Test
    void rejectsEditingPublishedVersionBeforeChangingDefinition() {
        Schema schema = new Schema(
            SchemaScopeTypeCode.platform(),
            null,
            "settings",
            "Settings",
            null,
            LocalDateTime.now()
        );
        SchemaVersion version = new SchemaVersion(
            schema,
            1,
            "v1",
            "{}",
            SchemaVersionStatusTypeCode.draft(),
            LocalDateTime.now()
        );
        version.publish();

        assertThatThrownBy(() -> versions.validateDraftUpdate(version, "{\"type\":\"object\"}")).isInstanceOf(
            ConflictException.class
        );
    }
}
