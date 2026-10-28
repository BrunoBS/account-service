package br.com.portalmanager.platform.workspace.feature.parameter.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyOutput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyCommandService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyPublisherService;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations.ParameterKeyQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DynamicParameterFacade {
    private final ParameterKeyCommandService commands;
    private final ParameterKeyQueryService queries;
    private final ParameterKeyPublisherService publication;

    public DynamicParameterFacade(
            ParameterKeyCommandService commands,
            ParameterKeyQueryService queries,
            ParameterKeyPublisherService publication) {
        this.commands = commands;
        this.queries = queries;
        this.publication = publication;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public ParameterCachePage findCache(AuthorizationContext context, String microserviceIdentifier,
            String cursor, Integer count) {
        return queries.findCache(microserviceIdentifier, cursor, count);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void clearCache(AuthorizationContext context, String microserviceIdentifier) {
        publication.clearCache(microserviceIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public void publish(AuthorizationContext context, PublishParameterKeysInput input) {
        publication.publish(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ParameterKeyMetadata create(
            AuthorizationContext context, CreateParameterKeyInput input) {
        return commands.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public ParameterKeyMetadata update(
            AuthorizationContext context, String identifier, UpdateParameterKeyInput input) {
        return commands.update(identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public ParameterKeyMetadata activate(AuthorizationContext context, String identifier) {
        return commands.activate(identifier);
    }

    @AuthorizationRequired(
            level = AuthorizationLevel.OWNER,
            action = AuthorizationAction.DEACTIVATE)
    public ParameterKeyMetadata inactivate(AuthorizationContext context, String identifier) {
        return commands.inactivate(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ParameterKeyMetadata delete(AuthorizationContext context, String identifier) {
        return commands.delete(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public ParameterKeyOutput findByIdentifier(AuthorizationContext context, String identifier) {
        return queries.findByIdentifier(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.READ)
    public Page<ParameterKeyOutput> findAll(
            AuthorizationContext context,
            String microserviceIdentifier,
            String status,
            String type,
            boolean active,
            String key,
            Pageable pageable) {
        return queries.findAll(microserviceIdentifier, status, type, active, key, pageable);
    }
}
