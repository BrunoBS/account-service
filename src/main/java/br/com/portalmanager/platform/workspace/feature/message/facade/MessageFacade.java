package br.com.portalmanager.platform.workspace.feature.message.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message.MessageCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message.MessageQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MessageFacade {

    private final MessageCommandService commandService;
    private final MessageQueryService queryService;

    public MessageFacade(MessageCommandService commandService, MessageQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public MessageOutput create(AuthorizationContext context, CreateMessageInput input) {
        return commandService.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageOutput findByIdentifier(AuthorizationContext context, String messageIdentifier) {
        return queryService.findByIdentifier(messageIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public Page<MessageOutput> findAll(
        AuthorizationContext context,
        String microserviceIdentifier,
        String lifecycle,
        String code,
        String messageKey,
        Pageable pageable
    ) {
        return queryService.findAll(microserviceIdentifier, lifecycle, code, messageKey, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageOutput update(AuthorizationContext context, String messageIdentifier, UpdateMessageInput input) {
        return commandService.update(messageIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageOutput activate(AuthorizationContext context, String messageIdentifier) {
        return commandService.activate(messageIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageOutput inactivate(AuthorizationContext context, String messageIdentifier) {
        return commandService.inactivate(messageIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String messageIdentifier) {
        commandService.delete(messageIdentifier);
    }
}
