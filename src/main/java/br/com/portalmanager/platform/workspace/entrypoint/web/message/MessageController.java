package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageResponse;
import br.com.portalmanager.platform.workspace.feature.message.facade.MessageFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageFacade facade;

    public MessageController(MessageFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> create(AuthorizationContext context, @RequestBody CreateMessageRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            MessageResponse.from(facade.create(context, r.toInput()))
        );
    }

    @GetMapping("/{identifier}")
    public MessageResponse findByIdentifier(AuthorizationContext context, @PathVariable String identifier) {
        return MessageResponse.from(facade.findByIdentifier(context, identifier));
    }

    @GetMapping
    public List<MessageResponse> findAll(
        AuthorizationContext context,
        @RequestParam(required = false) String microserviceIdentifier,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) String code,
        @RequestParam(required = false) String messageKey
    ) {
        return facade
            .findAll(context, microserviceIdentifier, active, code, messageKey)
            .stream()
            .map(MessageResponse::from)
            .toList();
    }

    @PutMapping("/{identifier}")
    public MessageResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateMessageRequest r
    ) {
        return MessageResponse.from(facade.update(context, identifier, r.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public MessageResponse activate(AuthorizationContext context, @PathVariable String identifier) {
        return MessageResponse.from(facade.activate(context, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public MessageResponse inactivate(AuthorizationContext context, @PathVariable String identifier) {
        return MessageResponse.from(facade.inactivate(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
