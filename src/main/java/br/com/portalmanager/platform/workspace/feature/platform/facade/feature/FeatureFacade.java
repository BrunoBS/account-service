package br.com.portalmanager.platform.workspace.feature.platform.facade.feature;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.CreateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.UpdateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response.FeatureResponse;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FeatureFacade {
    private final FeatureCommandService command;
    private final FeatureQueryService query;
    public FeatureFacade(FeatureCommandService command, FeatureQueryService query) {
        this.command = command; this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public FeatureResponse create(CreateFeatureRequest request) {
        return FeatureResponse.from(command.create(request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public FeatureResponse findByIdentifier(String identifier) {
        return FeatureResponse.from(query.findByIdentifier(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<FeatureResponse> findAll(String contextCode) {
        return (contextCode == null ? query.findAll() : query.findByContext(contextCode))
                .stream().map(FeatureResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public List<FeatureContextResponse> findContexts(String identifier) {
        return query.findContexts(identifier).stream().map(FeatureContextResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureResponse update(String identifier, UpdateFeatureRequest request) {
        return FeatureResponse.from(command.update(identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public FeatureResponse activate(String identifier) {
        return FeatureResponse.from(command.activate(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public FeatureResponse inactivate(String identifier) {
        return FeatureResponse.from(command.inactivate(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureResponse associateContext(String identifier, String contextIdentifier) {
        return FeatureResponse.from(command.associateContext(identifier, contextIdentifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public FeatureResponse removeContext(String identifier, String contextIdentifier) {
        return FeatureResponse.from(command.removeContext(identifier, contextIdentifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(String identifier) {
        command.delete(identifier);
    }
}
