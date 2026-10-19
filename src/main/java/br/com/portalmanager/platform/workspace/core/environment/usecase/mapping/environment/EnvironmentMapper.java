package br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environment;

import br.com.portalmanager.platform.workspace.core.environment.domain.environment.EnvironmentDetails;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentTreeOutput;
import java.util.List;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentMapper {

    public EnvironmentReferenceOutput toReference(Long id, EnvironmentOutput output) {
        return new EnvironmentReferenceOutput(id, output);
    }

    public EnvironmentTreeOutput toTreeOutput(EnvironmentOutput output, List<EnvironmentTreeOutput> children) {
        return new EnvironmentTreeOutput(output, children);
    }

    public EnvironmentOutput toOutput(Environment environment, String workspaceIdentifier) {
        String parentIdentifier = environment.getParent() == null ? null : environment.getParent().getIdentifier();
        return toOutput(environment, workspaceIdentifier, parentIdentifier);
    }

    public EnvironmentOutput toOutput(Environment environment, String workspaceIdentifier, String parentIdentifier) {
        return new EnvironmentOutput(environment.getVersion(), environment.getIdentifier(), workspaceIdentifier,
            environment.getName(), environment.getDescription(), environment.getAuthorizationType().value(),
            environment.getEnvironmentType().getCode(), parentIdentifier, environment.getSortOrder(),
            environment.getAuthorizerGroup(), environment.getSettings(), environment.getLifecycle().value(),
            environment.getCreatedAt(), environment.getUpdatedAt());
    }

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
