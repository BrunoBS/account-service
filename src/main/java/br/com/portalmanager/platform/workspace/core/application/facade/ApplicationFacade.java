package br.com.portalmanager.platform.workspace.core.application.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationCommandService;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationSummaryResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import java.util.List;

@Service
public class ApplicationFacade {
    private final ApplicationCommandService command;
    private final ApplicationQueryService query;
    public ApplicationFacade(ApplicationCommandService command, ApplicationQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ApplicationResponse create(String workspaceIdentifier, JsonNode payload) throws tools.jackson.core.JacksonException {
        return ApplicationResponse.from(command.create(workspaceIdentifier, payload));
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ApplicationResponse find(String workspaceIdentifier, String identifier) {
        return ApplicationResponse.from(query.findByIdentifier(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<ApplicationResponse> findAll(String workspaceIdentifier, Boolean active, String tagName) {
        return query.findAll(workspaceIdentifier, active, tagName).stream().map(ApplicationResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<ApplicationSummaryResponse> summary(String workspaceIdentifier) {
        return query.summary(workspaceIdentifier).stream().map(ApplicationSummaryResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public ApplicationResponse update(String workspaceIdentifier, String identifier, JsonNode payload) throws tools.jackson.core.JacksonException {
        return ApplicationResponse.from(command.update(workspaceIdentifier, identifier, payload));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(String workspaceIdentifier, String identifier) {
        command.inactivate(workspaceIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public ApplicationResponse restore(String workspaceIdentifier, String identifier) {
        return ApplicationResponse.from(command.restore(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(String workspaceIdentifier, String identifier) {
        command.delete(workspaceIdentifier, identifier);
    }
}
