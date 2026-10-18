package br.com.portalmanager.platform.workspace.feature.dynamicbundle.facade.messagetranslator;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.CreateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.MessageTranslatorOutput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.model.messagetranslator.UpdateMessageTranslatorInput;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorCommandService;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.usecase.operations.messagetranslator.MessageTranslatorQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class MessageTranslatorFacade {

    private final MessageTranslatorCommandService commandService;
    private final MessageTranslatorQueryService queryService;

    public MessageTranslatorFacade(
            MessageTranslatorCommandService commandService,
            MessageTranslatorQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public Page<MessageTranslatorOutput> findAll(
            AuthorizationContext context,
            String messageIdentifier,
            String locale,
            String lifecycle,
            Pageable pageable) {
        return queryService.findAll(messageIdentifier, locale, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageTranslatorOutput findByIdentifier(
            AuthorizationContext context, String messageIdentifier, String translatorIdentifier) {
        return queryService.findByIdentifier(messageIdentifier, translatorIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public MessageTranslatorOutput create(
            AuthorizationContext context,
            String messageIdentifier,
            CreateMessageTranslatorInput input) {
        return commandService.create(messageIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageTranslatorOutput update(
            AuthorizationContext context,
            String messageIdentifier,
            String translatorIdentifier,
            UpdateMessageTranslatorInput input) {
        return commandService.update(messageIdentifier, translatorIdentifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageTranslatorOutput activate(
            AuthorizationContext context, String messageIdentifier, String translatorIdentifier) {
        return commandService.activate(messageIdentifier, translatorIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageTranslatorOutput inactivate(
            AuthorizationContext context, String messageIdentifier, String translatorIdentifier) {
        return commandService.inactivate(messageIdentifier, translatorIdentifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(
            AuthorizationContext context, String messageIdentifier, String translatorIdentifier) {
        commandService.delete(messageIdentifier, translatorIdentifier);
    }
}
