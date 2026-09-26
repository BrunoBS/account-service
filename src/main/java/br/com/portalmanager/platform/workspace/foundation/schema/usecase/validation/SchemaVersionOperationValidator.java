package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;

public class SchemaVersionOperationValidator {
    public void validateCreate(Schema schema, Integer number, String definition,
                               SchemaVersionStatusTypeCode status) {
        if (schema == null) throw new IllegalArgumentException("Schema is required");
        if (number == null || number < 1) throw new IllegalArgumentException("Schema version must be positive");
        validateDefinition(definition);
        if (status == null) throw new IllegalArgumentException("Schema version status is required");
    }

    public void validateDraftUpdate(SchemaVersion version, String definition) {
        validateDraft(version);
        validateDefinition(definition);
    }

    public void validateDraft(SchemaVersion version) {
        if (!version.isDraft()) {
            throw new IllegalStateException("Published schema version is immutable");
        }
    }

    public void validateDraftDeletion(SchemaVersion version) {
        if (!version.isDraft()) {
            throw new IllegalStateException("Published schema version cannot be physically deleted");
        }
    }

    private void validateDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            throw new IllegalArgumentException("Schema definition is required");
        }
    }
}
