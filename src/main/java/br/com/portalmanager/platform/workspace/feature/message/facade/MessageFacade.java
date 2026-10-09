package br.com.portalmanager.platform.workspace.feature.message.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.CreateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.UpdateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageResponse;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message.MessageCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.message.MessageQueryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MessageFacade {
    private final MessageCommandService commandService;
    private final MessageQueryService queryService;
    public MessageFacade(MessageCommandService commandService, MessageQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public MessageResponse create(CreateMessageRequest request) { return MessageResponse.from(commandService.create(request.toInput())); }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageResponse findByIdentifier(String identifier) { return MessageResponse.from(queryService.findByIdentifier(identifier)); }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public List<MessageResponse> findAll(String microserviceIdentifier, Boolean active, String code, String messageKey) {
        return queryService.findAll(microserviceIdentifier, active, code, messageKey).stream().map(MessageResponse::from).toList();
    }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageResponse update(String identifier, UpdateMessageRequest request) { return MessageResponse.from(commandService.update(identifier, request.toInput())); }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageResponse activate(String identifier) { return MessageResponse.from(commandService.activate(identifier)); }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageResponse inactivate(String identifier) { return MessageResponse.from(commandService.inactivate(identifier)); }
    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(String identifier) { commandService.delete(identifier); }
}
