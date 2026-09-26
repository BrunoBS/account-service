package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaResolution;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.ValidateSettingsInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaResolver;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import org.springframework.stereotype.Service;

@Service
public class ValidateSettingsUseCase {

    private static final String SCOPE_INVALID = "workspace-service.schema.scope.invalid";

    private final SchemaResolver resolver;
    private final SchemaValidator validator;

    public ValidateSettingsUseCase(
            SchemaResolver resolver,
            SchemaValidator validator
    ) {
        this.resolver = resolver;
        this.validator = validator;
    }

    public SchemaResolution validate(ValidateSettingsInput input) {
        if (input == null) {
            throw validation("schema", SCOPE_INVALID);
        }

        SchemaResolution resolution;
        if (SchemaScopeTypeCode.platform().value().equals(input.scope())) {
            resolution = resolver.resolvePlatform(input.schemaTypeCode());
        } else if (SchemaScopeTypeCode.workspace().value().equals(input.scope())) {
            resolution = resolver.resolveWorkspace(
                    input.workspaceIdentifier(),
                    input.schemaTypeCode(),
                    input.schemaCode()
            );
        } else {
            throw validation("scope", SCOPE_INVALID);
        }

        ValidationResult result = new ValidationResult();
        validator.validateJson(
                resolution.definition(),
                input.settings(),
                "settings",
                result
        );

        if (result.hasErrors()) {
            throw new ValidationException(result);
        }

        return resolution;
    }

    private ValidationException validation(String field, String messageKey) {
        ValidationResult result = new ValidationResult();
        result.addError(field, messageKey);
        return new ValidationException(result);
    }
}
