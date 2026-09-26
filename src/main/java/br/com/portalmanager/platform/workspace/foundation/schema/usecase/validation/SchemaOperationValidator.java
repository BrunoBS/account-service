package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.ValidateSettingsInput;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;

import java.util.Objects;
import java.util.regex.Pattern;

/** Decisions about schema requests and state transitions belong to the use case. */
public class SchemaOperationValidator {
    private static final Pattern CODE = Pattern.compile("^[a-z][a-z0-9]*(?:-[a-z0-9]+)*$");
    private static final String SCOPE_INVALID = "workspace-service.schema.scope.invalid";

    public SchemaScopeTypeCode validateSettingsScope(ValidateSettingsInput input) {
        if (input == null) throw new ValidationException(new ValidationResult("schema", SCOPE_INVALID));
        if (SchemaScopeTypeCode.platform().value().equals(input.scope())) return SchemaScopeTypeCode.platform();
        if (SchemaScopeTypeCode.workspace().value().equals(input.scope())) return SchemaScopeTypeCode.workspace();
        throw new ValidationException(new ValidationResult("scope", SCOPE_INVALID));
    }

    public void validateCreateInput(CreateSchemaInput input) {
        if (input == null) throw new IllegalArgumentException("Schema input is required");
        if (input.schemaTypeCode() == null || input.schemaTypeCode().isBlank()) {
            throw new IllegalArgumentException("Schema type code is required");
        }
        if (input.code() == null || !CODE.matcher(input.code()).matches()) {
            throw new IllegalArgumentException("Schema code must use lowercase kebab-case");
        }
        validateName(input.name());
    }

    public void validateOwnership(SchemaScopeTypeCode scope, Long workspaceId) {
        if (scope == null) throw new IllegalArgumentException("Schema scope is required");
        if (SchemaScopeTypeCode.workspace().equals(scope) && workspaceId == null) {
            throw new IllegalArgumentException("Workspace id is required for WORKSPACE schema");
        }
        if (!SchemaScopeTypeCode.workspace().equals(scope) && workspaceId != null) {
            throw new IllegalArgumentException("Workspace id must be empty for PLATFORM schema");
        }
    }

    public void validateWorkspaceIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Workspace identifier is required");
        }
    }

    public void validatePlatformTypeAvailable(boolean alreadyExists) {
        if (alreadyExists) throw new IllegalArgumentException("Platform schema type already has a schema");
    }

    public void validateTypeActive(boolean active) {
        if (!active) throw new IllegalArgumentException("Active schema type not found");
    }

    public void validateCodeAvailable(boolean alreadyExists) {
        if (alreadyExists) throw new IllegalArgumentException("Schema code already exists in scope");
    }

    public void validateUpdate(Schema schema, UpdateSchemaInput input) {
        if (input == null || !Objects.equals(input.version(), schema.getVersion())) {
            throw new IllegalStateException("Schema version conflict");
        }
        validateName(input.name());
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Schema name is required");
    }
}
