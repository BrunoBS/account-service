package br.com.portalmanager.platform.workspace.feature.message.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.feature.message.usecase.model.*;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MessageFacade {

    private final MessageCommandService commandService;
    private final MessageQueryService queryService;

    public MessageFacade(MessageCommandService c, MessageQueryService q) {
        commandService = c;
        queryService = q;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public MessageOutput create(AuthorizationContext context, CreateMessageInput i) {
        return commandService.create(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageOutput findByIdentifier(AuthorizationContext context, String i) {
        return queryService.findByIdentifier(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public List<MessageOutput> findAll(AuthorizationContext context, String m, Boolean a, String c, String k) {
        return queryService.findAll(m, a, c, k);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageOutput update(AuthorizationContext context, String i, UpdateMessageInput in) {
        return commandService.update(i, in);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageOutput activate(AuthorizationContext context, String i) {
        return commandService.activate(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageOutput inactivate(AuthorizationContext context, String i) {
        return commandService.inactivate(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String i) {
        commandService.delete(i);
    }
}
