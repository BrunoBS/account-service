package br.com.portalmanager.platform.workspace.core.environment.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.environment.domain.environment.EnvironmentDetails;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentMapper {

    public EnvironmentDetails toDetails(CreateEnvironmentInput input, int sortOrder) {
        return new EnvironmentDetails(
            input.name(),
            input.description(),
            AuthorizationTypeCode.of(input.authorizationType()),
            input.authorizerGroup(),
            input.settings(),
            sortOrder
        );
    }

    public EnvironmentDetails toDetails(UpdateEnvironmentInput input, int sortOrder) {
        return new EnvironmentDetails(
            input.name(),
            input.description(),
            AuthorizationTypeCode.of(input.authorizationType()),
            input.authorizerGroup(),
            input.settings(),
            sortOrder
        );
    }
}
