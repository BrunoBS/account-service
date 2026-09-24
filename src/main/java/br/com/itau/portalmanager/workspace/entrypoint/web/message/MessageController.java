package br.com.itau.portalmanager.workspace.entrypoint.web.message;

import br.com.itau.portalmanager.workspace.entrypoint.web.message.request.CreateMessageRequest;
import br.com.itau.portalmanager.workspace.entrypoint.web.message.request.UpdateMessageRequest;
import br.com.itau.portalmanager.workspace.entrypoint.web.message.response.MessageResponse;
import br.com.itau.portalmanager.workspace.feature.message.usecase.MessageCommandService;
import br.com.itau.portalmanager.workspace.feature.message.usecase.MessageQueryService;
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
@RequestMapping("/api/v1/messages")
@AuthorizationRequired(level = AuthorizationLevel.ADM)
public class MessageController {

    private final MessageCommandService commandService;
    private final MessageQueryService queryService;

    public MessageController(MessageCommandService commandService, MessageQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Auditable(
            resource = "MESSAGE",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<MessageResponse> create(@RequestBody CreateMessageRequest request) {
        MessageResponse response = MessageResponse.from(commandService.create(request.toInput()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{identifier}")
    public MessageResponse findByIdentifier(@PathVariable String identifier) {
        return MessageResponse.from(queryService.findByIdentifier(identifier));
    }

    @GetMapping
    public List<MessageResponse> findAll(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String messageKey
    ) {
        return queryService.findAll(service, active, code, messageKey).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @PutMapping("/{identifier}")
    @Auditable(
            resource = "MESSAGE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public MessageResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateMessageRequest request
    ) {
        return MessageResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    @Auditable(
            resource = "MESSAGE",
            action = "ACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public MessageResponse activate(@PathVariable String identifier) {
        return MessageResponse.from(commandService.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    @Auditable(
            resource = "MESSAGE",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public MessageResponse inactivate(@PathVariable String identifier) {
        return MessageResponse.from(commandService.inactivate(identifier));
    }

    @DeleteMapping("/{identifier}")
    @Auditable(
            resource = "MESSAGE",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
