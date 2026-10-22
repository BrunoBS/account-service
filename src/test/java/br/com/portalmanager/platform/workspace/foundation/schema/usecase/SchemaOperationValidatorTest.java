package br.com.portalmanager.platform.workspace.foundation.schema.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.schema.SchemaOperationValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.version.SchemaVersionOperationValidator;
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
    void validatesStorageLengthsAfterTheSameTrimmingUsedByTheDomain() {
        assertThatCode(() -> schemas.validateCreateInput(new CreateSchemaInput(
                "a".repeat(50), " " + "n".repeat(100) + " ", "d".repeat(500), null, null, null)))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> schemas.validateCreateInput(new CreateSchemaInput(
                "a".repeat(51), "Name", null, null, null, null))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> schemas.validateCreateInput(new CreateSchemaInput(
                "code", "n".repeat(101), null, null, null, null))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> schemas.validateCreateInput(new CreateSchemaInput(
                "code", "Name", "d".repeat(501), null, null, null))).isInstanceOf(ValidationException.class);
        assertThatCode(() -> versions.validateVersionName(" " + "v".repeat(100) + " "))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> versions.validateVersionName("v".repeat(101)))
                .isInstanceOf(ValidationException.class);
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
