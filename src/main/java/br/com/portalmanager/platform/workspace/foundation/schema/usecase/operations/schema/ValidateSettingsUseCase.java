package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaResolution;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.ValidateSettingsInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaOperationValidator;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaValidator;
import org.springframework.stereotype.Service;

@Service
public class ValidateSettingsUseCase {

    private final SchemaResolver resolver;
    private final SchemaValidator validator;
    private final SchemaOperationValidator operationValidator = new SchemaOperationValidator();

    public ValidateSettingsUseCase(
            SchemaResolver resolver,
            SchemaValidator validator
    ) {
        this.resolver = resolver;
        this.validator = validator;
    }

    public SchemaResolution validate(ValidateSettingsInput input) {
        SchemaScopeTypeCode scope = operationValidator.validateSettingsScope(input);
        SchemaResolution resolution;
        if (SchemaScopeTypeCode.platform().equals(scope)) {
            resolution = resolver.resolvePlatform(input.schemaTypeCode());
        } else {
            resolution = resolver.resolveWorkspace(
                    input.workspaceIdentifier(),
                    input.schemaTypeCode(),
                    input.schemaCode()
            );
        }

        validator.requireValidJson(
                resolution.definition(),
                input.settings(),
                "settings"
        );

        return resolution;
    }

}
