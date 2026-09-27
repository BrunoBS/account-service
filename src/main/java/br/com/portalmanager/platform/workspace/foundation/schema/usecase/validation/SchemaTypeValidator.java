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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class SchemaTypeValidator {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    public Set<SchemaScopeTypeCode> validateForCreate(
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

        Set<SchemaScopeTypeCode> scopes = parseScopes(input.allowedScopes());
        validateDefaultScopePolicy(code, scopes);

        if (codeDuplicate) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DUPLICATE);
        }
        return scopes;
    }

    public Set<SchemaScopeTypeCode> validateForUpdate(
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

        Set<SchemaScopeTypeCode> scopes = parseScopes(input.allowedScopes());
        validateDefaultScopePolicy(schemaType.getCode(), scopes);
        return scopes;
    }

    public void validateRemovedScope(boolean scopeInUse) {
        if (scopeInUse) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_SCOPE_IN_USE);
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

    private Set<SchemaScopeTypeCode> parseScopes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            throw invalid("allowedScopes", SchemaMessageKeys.SCHEMA_TYPE_SCOPES_REQUIRED);
        }

        Set<SchemaScopeTypeCode> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                throw invalid("allowedScopes", SchemaMessageKeys.SCOPE_INVALID);
            }
            String normalized = value.trim().toUpperCase();
            if ("PLATFORM".equals(normalized)) {
                result.add(SchemaScopeTypeCode.platform());
            } else if ("WORKSPACE".equals(normalized)) {
                result.add(SchemaScopeTypeCode.workspace());
            } else {
                throw invalid("allowedScopes", SchemaMessageKeys.SCOPE_INVALID);
            }
        }
        return result;
    }

    private void validateDefaultScopePolicy(
            String code,
            Set<SchemaScopeTypeCode> requestedScopes
    ) {
        if (!"DEFAULT".equals(code)) {
            return;
        }
        if (requestedScopes.size() != 1 || !requestedScopes.contains(SchemaScopeTypeCode.platform())) {
            throw invalid("allowedScopes", SchemaMessageKeys.SCHEMA_TYPE_DEFAULT_SCOPE_INVALID);
        }
    }

    private boolean isDefault(SchemaType schemaType) {
        return schemaType != null && "DEFAULT".equals(schemaType.getCode());
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
