package br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaConfiguration;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SchemaConfigurationValidator {
    private static final String MESSAGE = "workspace-service.schema-configuration.";

    public void validateCreate(CreateSchemaConfigurationInput input) {
        if (input == null) throw invalid("request", "request.invalid");
        if (input.resourceType() == null || input.resourceType().isBlank() || input.resourceType().length() > 100)
            throw invalid("resourceType", "resource-type.invalid");
        if (input.resourceCode() == null || input.resourceCode().isBlank() || input.resourceCode().length() > 50)
            throw invalid("resourceCode", "resource-code.invalid");
    }

    public void validateAvailable(boolean alreadyExists) {
        if (alreadyExists) throw new ConflictException(MESSAGE + "duplicate");
    }

    public void validateUpdate(SchemaConfiguration configuration, UpdateSchemaConfigurationInput input) {
        if (input == null || input.version() == null) throw invalid("version", "version.required");
        if (!Objects.equals(configuration.getVersion(), input.version())) throw new ResourceVersionConflictException();
    }

    public String requireSchemaIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) throw invalid("schemaIdentifier", "schema.required");
        return identifier.trim();
    }

    public void validatePlatformSchema(Schema schema) {
        if (!SchemaScopeTypeCode.platform().equals(schema.getScope()))
            throw invalid("schemaIdentifier", "schema.platform-required");
    }

    public void validateDeletion(SchemaConfiguration configuration) {
        if (configuration.isActive()) throw new ConflictException(MESSAGE + "delete.active");
    }

    public NotFoundException schemaNotFound() {
        return new NotFoundException(MESSAGE + "schema.not-found");
    }

    public NotFoundException configurationNotFound() {
        return new NotFoundException(MESSAGE + "not-found");
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, MESSAGE + key));
    }
}
