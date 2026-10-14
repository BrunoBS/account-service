package br.com.portalmanager.platform.workspace.core.application.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationDetails;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMapper {

    public ApplicationDetails toDetails(CreateApplicationInput input) {
        return new ApplicationDetails(
            input.name(),
            input.alias(),
            input.description(),
            input.acronym(),
            ApplicationScopeTypeCode.of(input.applicationScope()),
            input.authorizerGroup(),
            input.settings()
        );
    }

    public ApplicationDetails toDetails(UpdateApplicationInput input) {
        return new ApplicationDetails(
            input.name(),
            input.alias(),
            input.description(),
            input.acronym(),
            ApplicationScopeTypeCode.of(input.applicationScope()),
            input.authorizerGroup(),
            input.settings()
        );
    }
}
