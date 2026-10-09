package br.com.portalmanager.platform.workspace.feature.message.facade;

import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.CreateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.UpdateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageTranslationResponse;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
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
    public List<MessageTranslationResponse> findAll(
            String messageIdentifier,
            String locale,
            Boolean active
    ) {
        return queryService.findAll(messageIdentifier, locale, active).stream()
                .map(MessageTranslationResponse::from)
                .toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.READ)
    public MessageTranslationResponse findByIdentifier(
            String messageIdentifier,
            String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
                queryService.findByIdentifier(messageIdentifier, translationIdentifier)
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ResponseEntity<MessageTranslationResponse> create(
            String messageIdentifier,
            CreateMessageTranslationRequest request
    ) {
        MessageTranslationResponse response = MessageTranslationResponse.from(
                commandService.create(messageIdentifier, request.toInput())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public MessageTranslationResponse update(
            String messageIdentifier,
            String translationIdentifier,
            UpdateMessageTranslationRequest request
    ) {
        return MessageTranslationResponse.from(
                commandService.update(messageIdentifier, translationIdentifier, request.toInput())
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.ACTIVATE)
    public MessageTranslationResponse activate(
            String messageIdentifier,
            String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
                commandService.activate(messageIdentifier, translationIdentifier)
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public MessageTranslationResponse inactivate(
            String messageIdentifier,
            String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
                commandService.inactivate(messageIdentifier, translationIdentifier)
        );
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> delete(
            String messageIdentifier,
            String translationIdentifier
    ) {
        commandService.delete(messageIdentifier, translationIdentifier);
        return ResponseEntity.noContent().build();
    }
}
