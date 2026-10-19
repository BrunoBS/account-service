package br.com.portalmanager.platform.workspace.core.application.usecase.mapping;

import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationDetails;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationReference;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationScopeReference;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class ApplicationMapper {

    public ApplicationScopeReferenceOutput toScopeReference(Long workspaceId, Long applicationId, String scope) {
        return new ApplicationScopeReferenceOutput(workspaceId, applicationId, scope);
    }

    public ApplicationScopeReferenceOutput toScopeReference(Long workspaceId, Application application) {
        return toScopeReference(workspaceId, application.getId(), application.getApplicationScope().value());
    }

    public ApplicationScopeReferenceOutput toScopeReference(Long workspaceId, Long applicationId, ApplicationScopeReference reference) {
        return toScopeReference(workspaceId, applicationId, reference.getApplicationScope());
    }

    public ApplicationReferenceOutput toReference(ApplicationReference reference, String workspaceIdentifier) {
        return new ApplicationReferenceOutput(reference.getId(), reference.getWorkspaceId(), reference.getIdentifier(),
            workspaceIdentifier, reference.getName());
    }

    public ApplicationSummary toSummary(ApplicationReference reference) {
        return new ApplicationSummary(reference.getIdentifier(), reference.getName(), reference.getAlias(),
            reference.getAcronym(), reference.getApplicationScope(), reference.getAuthorizerGroup());
    }

    public ApplicationOutput toOutput(Application application, String workspaceIdentifier, List<String> tags) {
        return new ApplicationOutput(
            application.getVersion(), application.getIdentifier(), workspaceIdentifier, application.getName(),
            application.getAlias(), application.getDescription(), application.getAcronym(),
            application.getApplicationScope().value(), application.getAuthorizerGroup(), application.getSettings(),
            application.getLifecycle().value(), application.getCreatedAt(), application.getUpdatedAt(),
            tags == null ? List.of() : List.copyOf(tags)
        );
    }

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
