package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.CreateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.UpdateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageTranslationResponse;
import br.com.portalmanager.platform.workspace.feature.message.usecase.MessageTranslationCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.MessageTranslationQueryService;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.audit.annotation.AuditField;
import br.com.portalmanager.platform.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.audit.annotation.Auditable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages/{messageIdentifier}/translations")
@AuthorizationRequired(level = AuthorizationLevel.ADM)
public class MessageTranslationController {

    private final MessageTranslationCommandService commandService;
    private final MessageTranslationQueryService queryService;

    public MessageTranslationController(
            MessageTranslationCommandService commandService,
            MessageTranslationQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @GetMapping
    public List<MessageTranslationResponse> findAll(
            @PathVariable String messageIdentifier,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Boolean active
    ) {
        return queryService.findAll(messageIdentifier, locale, active).stream()
                .map(MessageTranslationResponse::from)
                .toList();
    }

    @GetMapping("/{translationIdentifier}")
    public MessageTranslationResponse findByIdentifier(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
                queryService.findByIdentifier(messageIdentifier, translationIdentifier)
        );
    }

    @PostMapping
    @Auditable(
            resource = "MESSAGE_TRANSLATION",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<MessageTranslationResponse> create(
            @PathVariable String messageIdentifier,
            @RequestBody CreateMessageTranslationRequest request
    ) {
        MessageTranslationResponse response = MessageTranslationResponse.from(
                commandService.create(messageIdentifier, request.toInput())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{translationIdentifier}")
    @Auditable(
            resource = "MESSAGE_TRANSLATION",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "translationIdentifier")
    )
    public MessageTranslationResponse update(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier,
            @RequestBody UpdateMessageTranslationRequest request
    ) {
        return MessageTranslationResponse.from(
                commandService.update(messageIdentifier, translationIdentifier, request.toInput())
        );
    }

    @PatchMapping("/{translationIdentifier}/activate")
    @Auditable(
            resource = "MESSAGE_TRANSLATION",
            action = "ACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "translationIdentifier")
    )
    public MessageTranslationResponse activate(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
                commandService.activate(messageIdentifier, translationIdentifier)
        );
    }

    @PatchMapping("/{translationIdentifier}/inactivate")
    @Auditable(
            resource = "MESSAGE_TRANSLATION",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "translationIdentifier")
    )
    public MessageTranslationResponse inactivate(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
                commandService.inactivate(messageIdentifier, translationIdentifier)
        );
    }

    @DeleteMapping("/{translationIdentifier}")
    @Auditable(
            resource = "MESSAGE_TRANSLATION",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "translationIdentifier")
    )
    public ResponseEntity<Void> delete(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        commandService.delete(messageIdentifier, translationIdentifier);
        return ResponseEntity.noContent().build();
    }
}
