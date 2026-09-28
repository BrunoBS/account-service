package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaInput;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Decisions about schema requests and state transitions belong to the use case.
 */
public class SchemaOperationValidator {
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");

    public void validateCreateInput(CreateSchemaInput input) {
        if (input == null) throw invalid("schema", SchemaMessageKeys.REQUEST_INVALID);
        if (input.code() == null || !CODE.matcher(input.code()).matches()) {
            throw invalid("code", SchemaMessageKeys.CODE_INVALID);
        }
        validateName(input.name());
    }

    public void validateOwnership(SchemaScopeTypeCode scope, Long workspaceId) {
        if (scope == null) throw invalid("scope", SchemaMessageKeys.SCOPE_INVALID);
        if (SchemaScopeTypeCode.workspace().equals(scope) && workspaceId == null) {
            throw invalid("workspaceIdentifier", SchemaMessageKeys.WORKSPACE_REQUIRED);
        }
        if (!SchemaScopeTypeCode.workspace().equals(scope) && workspaceId != null) {
            throw invalid("workspaceIdentifier", SchemaMessageKeys.OWNERSHIP_INVALID);
        }
    }

    public void validateWorkspaceIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw invalid("workspaceIdentifier", SchemaMessageKeys.WORKSPACE_REQUIRED);
        }
    }

    public void validateCodeAvailable(boolean alreadyExists) {
        if (alreadyExists) throw new ConflictException(SchemaMessageKeys.CODE_DUPLICATE);
    }

    public void validateUpdate(Schema schema, UpdateSchemaInput input) {
        if (input == null) throw invalid("schema", SchemaMessageKeys.REQUEST_INVALID);
        if (input.version() == null) throw invalid("version", SchemaMessageKeys.VERSION_INVALID);
        if (!Objects.equals(input.version(), schema.getVersion())) {
            throw new ResourceVersionConflictException();
        }
        validateName(input.name());
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) throw invalid("name", SchemaMessageKeys.NAME_REQUIRED);
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
