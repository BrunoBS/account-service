package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;

public class SchemaVersionOperationValidator {
    public void validateCreate(Schema schema, Integer number, String definition,
                               SchemaVersionStatusTypeCode status) {
        if (schema == null) throw invalid("schema", SchemaMessageKeys.REQUEST_INVALID);
        if (number == null || number < 1) throw invalid("version", SchemaMessageKeys.VERSION_INVALID);
        validateDefinition(definition);
        if (status == null) throw invalid("status", SchemaMessageKeys.VERSION_INVALID);
    }

    public void validateDraftUpdate(SchemaVersion version, String definition) {
        validateDraft(version);
        validateDefinition(definition);
    }

    public void validateDraft(SchemaVersion version) {
        if (!version.isDraft()) {
            throw new ConflictException(SchemaMessageKeys.VERSION_IMMUTABLE);
        }
    }

    public void validateDraftDeletion(SchemaVersion version) {
        if (!version.isDraft()) {
            throw new ConflictException(SchemaMessageKeys.VERSION_IMMUTABLE);
        }
    }

    private void validateDefinition(String definition) {
        if (definition == null || definition.isBlank()) {
            throw invalid("definition", SchemaMessageKeys.DEFINITION_REQUIRED);
        }
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
