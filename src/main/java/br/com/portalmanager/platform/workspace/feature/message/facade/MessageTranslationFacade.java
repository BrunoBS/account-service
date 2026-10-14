package br.com.portalmanager.platform.workspace.feature.message.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.CreateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.MessageTranslationOutput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.UpdateMessageTranslationInput;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationQueryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MessageTranslationFacade {

    private final MessageTranslationCommandService commandService;
    private final MessageTranslationQueryService queryService;

    public MessageTranslationFacade(
        MessageTranslationCommandService commandService,
        MessageTranslationQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public Page<MessageTranslationOutput> findAll(
        AuthorizationContext context,
        String messageIdentifier,
        String locale,
        String lifecycle,
        Pageable pageable
    ) {
        return queryService.findAll(messageIdentifier, locale, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageTranslationOutput findByIdentifier(
        AuthorizationContext context,
        String messageIdentifier,
        String translationIdentifier
    ) {
        return queryService.findByIdentifier(messageIdentifier, translationIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public MessageTranslationOutput create(
        AuthorizationContext context,
        String messageIdentifier,
        CreateMessageTranslationInput input
    ) {
        return commandService.create(messageIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageTranslationOutput update(
        AuthorizationContext context,
        String messageIdentifier,
        String translationIdentifier,
        UpdateMessageTranslationInput input
    ) {
        return commandService.update(messageIdentifier, translationIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageTranslationOutput activate(
        AuthorizationContext context,
        String messageIdentifier,
        String translationIdentifier
    ) {
        return commandService.activate(messageIdentifier, translationIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageTranslationOutput inactivate(
        AuthorizationContext context,
        String messageIdentifier,
        String translationIdentifier
    ) {
        return commandService.inactivate(messageIdentifier, translationIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(
        AuthorizationContext context,
        String messageIdentifier,
        String translationIdentifier
    ) {
        commandService.delete(messageIdentifier, translationIdentifier);
    }
}
