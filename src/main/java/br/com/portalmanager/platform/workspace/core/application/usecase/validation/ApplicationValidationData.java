package br.com.portalmanager.platform.workspace.core.application.usecase.validation;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.ResourceValidationData;

import java.util.List;

public record ApplicationValidationData(
        Long version,
        String name,
        String alias,
        String acronym,
        String applicationScope,
        String authorizerGroup,
        String settings,
        List<String> tags
) implements ResourceValidationData {

    public static ApplicationValidationData from(CreateApplicationInput input) {
        return new ApplicationValidationData(null, input.name(), input.alias(), input.acronym(),
                input.applicationScope(), input.authorizerGroup(), input.settings(), input.tags());
    }

    public static ApplicationValidationData from(UpdateApplicationInput input) {
        return new ApplicationValidationData(input.version(), input.name(), input.alias(), input.acronym(),
                input.applicationScope(), input.authorizerGroup(), input.settings(), input.tags());
    }
}
