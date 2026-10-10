package br.com.portalmanager.platform.workspace.feature.message.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.*;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MessageTranslationFacade {

    private final MessageTranslationCommandService commandService;
    private final MessageTranslationQueryService queryService;

    public MessageTranslationFacade(MessageTranslationCommandService c, MessageTranslationQueryService q) {
        commandService = c;
        queryService = q;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public List<MessageTranslationOutput> findAll(AuthorizationContext context, String m, String l, Boolean a) {
        return queryService.findAll(m, l, a);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageTranslationOutput findByIdentifier(AuthorizationContext context, String m, String i) {
        return queryService.findByIdentifier(m, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public MessageTranslationOutput create(AuthorizationContext context, String m, CreateMessageTranslationInput i) {
        return commandService.create(m, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageTranslationOutput update(
        AuthorizationContext context,
        String m,
        String i,
        UpdateMessageTranslationInput in
    ) {
        return commandService.update(m, i, in);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageTranslationOutput activate(AuthorizationContext context, String m, String i) {
        return commandService.activate(m, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageTranslationOutput inactivate(AuthorizationContext context, String m, String i) {
        return commandService.inactivate(m, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String m, String i) {
        commandService.delete(m, i);
    }
}
