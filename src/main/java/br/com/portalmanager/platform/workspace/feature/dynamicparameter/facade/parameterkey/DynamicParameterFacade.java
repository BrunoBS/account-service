package br.com.portalmanager.platform.workspace.feature.dynamicparameter.facade.parameterkey;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.CreateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.PublishParameterKeysInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.UpdateParameterKeyInput;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyCommandService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyPublicationService;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey.ParameterKeyQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class DynamicParameterFacade {
    private final ParameterKeyCommandService commands;
    private final ParameterKeyQueryService queries;
    private final ParameterKeyPublicationService publication;

    public DynamicParameterFacade(
            ParameterKeyCommandService commands,
            ParameterKeyQueryService queries,
            ParameterKeyPublicationService publication) {
        this.commands = commands;
        this.queries = queries;
        this.publication = publication;
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
