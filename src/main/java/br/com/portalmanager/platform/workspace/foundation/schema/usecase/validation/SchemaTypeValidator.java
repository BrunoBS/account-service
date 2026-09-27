package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.regex.Pattern;

@Component
public class SchemaTypeValidator {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    public SchemaScopeTypeCode validateForCreate(
            CreateSchemaTypeInput input,
            boolean codeDuplicate
    ) {
        if (input == null) {
            throw invalid("schemaType", SchemaMessageKeys.REQUEST_INVALID);
        }

        String code = canonicalCode(input.code());
        validateCode(code);
        validateName(input.name());
        validateDescription(input.description());

        SchemaScopeTypeCode scope = parseScope(input.scope());
        validateDefaultScopePolicy(code, scope);

        if (codeDuplicate) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DUPLICATE);
        }
        return scope;
    }

    public SchemaScopeTypeCode validateForUpdate(
            SchemaType schemaType,
            UpdateSchemaTypeInput input
    ) {
        if (input == null || input.version() == null) {
            throw invalid("version", SchemaMessageKeys.VERSION_INVALID);
        }
        if (!Objects.equals(input.version(), schemaType.getVersion())) {
            throw new ResourceVersionConflictException();
        }

        validateName(input.name());
        validateDescription(input.description());

        SchemaScopeTypeCode scope = parseScope(input.scope());
        validateDefaultScopePolicy(schemaType.getCode(), scope);
        return scope;
    }

    public void validateRemovedScope(boolean scopeInUse) {
        if (scopeInUse) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_SCOPE_IN_USE);
        }
    }

    public void validateActive(SchemaType schemaType) {
        if (!schemaType.isActive()) {
            throw invalid("schemaTypeCode", SchemaMessageKeys.TYPE_INACTIVE);
        }
    }

    public void validateAllowedScope(SchemaType schemaType, SchemaScopeTypeCode scope) {
        if (!schemaType.allowsScope(scope)) {
            throw invalid("scope", SchemaMessageKeys.SCHEMA_TYPE_SCOPE_NOT_ALLOWED);
        }
    }

    public void validateInactivation(SchemaType schemaType) {
        if (isDefault(schemaType)) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DEFAULT_PROTECTED);
        }
    }

    public void validateDeletion(SchemaType schemaType, boolean inUse) {
        if (isDefault(schemaType)) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DEFAULT_PROTECTED);
        }
        if (schemaType.isActive()) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DELETE_ACTIVE);
        }
        if (inUse) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_IN_USE);
        }
    }

    public String canonicalCode(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }

    private void validateCode(String code) {
        if (code == null || !CODE.matcher(code).matches()) {
            throw invalid("code", SchemaMessageKeys.SCHEMA_TYPE_CODE_INVALID);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100) {
            throw invalid("name", SchemaMessageKeys.SCHEMA_TYPE_NAME_INVALID);
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > 500) {
            throw invalid("description", SchemaMessageKeys.SCHEMA_TYPE_DESCRIPTION_INVALID);
        }
    }

    private SchemaScopeTypeCode parseScope(String value) {
        if (value == null || value.isBlank()) {
            throw invalid("scope", SchemaMessageKeys.SCHEMA_TYPE_SCOPES_REQUIRED);
        }

        String normalized = value.trim().toUpperCase();
        if ("PLATFORM".equals(normalized)) {
            return SchemaScopeTypeCode.platform();
        }
        if ("WORKSPACE".equals(normalized)) {
            return SchemaScopeTypeCode.workspace();
        }
        throw invalid("scope", SchemaMessageKeys.SCOPE_INVALID);
    }

    private void validateDefaultScopePolicy(
            String code,
            SchemaScopeTypeCode scope
    ) {
        if ("DEFAULT".equals(code) && !SchemaScopeTypeCode.platform().equals(scope)) {
            throw invalid("scope", SchemaMessageKeys.SCHEMA_TYPE_DEFAULT_SCOPE_INVALID);
        }
    }

    private boolean isDefault(SchemaType schemaType) {
        return schemaType != null && "DEFAULT".equals(schemaType.getCode());
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
