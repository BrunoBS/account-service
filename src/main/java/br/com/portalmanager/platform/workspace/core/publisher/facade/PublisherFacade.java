package br.com.portalmanager.platform.workspace.core.publisher.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.PublisherCommandService;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.PublisherQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.CreatePublisherRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.UpdatePublisherRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.response.PublisherResponse;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PublisherFacade {
    private final PublisherCommandService command;
    private final PublisherQueryService query;
    public PublisherFacade(PublisherCommandService command, PublisherQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public PublisherResponse create(CreatePublisherRequest request) {
        return PublisherResponse.from(command.create(request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<PublisherResponse> list(Boolean active, String scope) {
        return query.list(active, scope).stream().map(PublisherResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public PublisherResponse find(String identifier) {
        return PublisherResponse.from(query.find(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public PublisherResponse update(String identifier, UpdatePublisherRequest request) {
        return PublisherResponse.from(command.update(identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(String identifier) { command.inactivate(identifier); }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public PublisherResponse restore(String identifier) {
        return PublisherResponse.from(command.restore(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(String identifier) { command.delete(identifier); }
}
