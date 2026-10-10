package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.configuration.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.schema.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class SchemaConfigurationValidator {

    public void validateCreate(CreateSchemaConfigurationInput input) {
        if (input == null) throw invalid("request", SchemaMessageKeys.CONFIGURATION_REQUEST_INVALID);
        if (
            input.resourceType() == null || input.resourceType().isBlank() || input.resourceType().length() > 100
        ) throw invalid("resourceType", SchemaMessageKeys.CONFIGURATION_RESOURCE_TYPE_INVALID);
        if (
            input.resourceCode() == null || input.resourceCode().isBlank() || input.resourceCode().length() > 50
        ) throw invalid("resourceCode", SchemaMessageKeys.CONFIGURATION_RESOURCE_CODE_INVALID);
    }

    public void validateAvailable(boolean alreadyExists) {
        if (alreadyExists) throw new ConflictException(SchemaMessageKeys.CONFIGURATION_DUPLICATE);
    }

    public void validateUpdate(SchemaConfiguration configuration, UpdateSchemaConfigurationInput input) {
        if (input == null || input.version() == null) throw invalid(
            "version",
            SchemaMessageKeys.CONFIGURATION_VERSION_REQUIRED
        );
        if (!Objects.equals(configuration.getVersion(), input.version())) throw new ResourceVersionConflictException();
    }

    public String requireSchemaIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) throw invalid(
            "schemaIdentifier",
            SchemaMessageKeys.CONFIGURATION_SCHEMA_REQUIRED
        );
        return identifier.trim();
    }

    public void validatePlatformSchema(Schema schema) {
        if (!SchemaScopeTypeCode.platform().equals(schema.getScope())) throw invalid(
            "schemaIdentifier",
            SchemaMessageKeys.CONFIGURATION_SCHEMA_PLATFORM_REQUIRED
        );
    }

    public void validateDeletion(SchemaConfiguration configuration) {
        if (configuration.isActive()) throw new ConflictException(SchemaMessageKeys.CONFIGURATION_DELETE_ACTIVE);
    }

    public NotFoundException schemaNotFound() {
        return new NotFoundException(SchemaMessageKeys.CONFIGURATION_SCHEMA_NOT_FOUND);
    }

    public NotFoundException configurationNotFound() {
        return new NotFoundException(SchemaMessageKeys.CONFIGURATION_NOT_FOUND);
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
