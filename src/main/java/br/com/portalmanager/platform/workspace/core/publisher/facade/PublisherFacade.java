package br.com.portalmanager.platform.workspace.core.publisher.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PublisherFacade {

    private final PublisherCommandService command;
    private final PublisherQueryService query;

    public PublisherFacade(PublisherCommandService command, PublisherQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public PublisherOutput create(AuthorizationContext context, CreatePublisherInput input) {
        return command.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public Page<PublisherOutput> findAll(
        AuthorizationContext context,
        String lifecycle,
        String scope,
        Pageable pageable
    ) {
        return query.findAll(lifecycle, scope, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public PublisherOutput find(AuthorizationContext context, String identifier) {
        return query.find(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public PublisherOutput update(AuthorizationContext context, String identifier, UpdatePublisherInput input) {
        return command.update(identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String identifier) {
        command.inactivate(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public PublisherOutput restore(AuthorizationContext context, String identifier) {
        return command.restore(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String identifier) {
        command.delete(identifier);
    }
}
