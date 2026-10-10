package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageTranslationResponse;
import br.com.portalmanager.platform.workspace.feature.message.facade.MessageTranslationFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages/{messageIdentifier}/translations")
public class MessageTranslationController {

    private final MessageTranslationFacade facade;

    public MessageTranslationController(MessageTranslationFacade f) {
        facade = f;
    }

    @GetMapping
    public List<MessageTranslationResponse> findAll(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @RequestParam(required = false) String locale,
        @RequestParam(required = false) Boolean active
    ) {
        return facade
            .findAll(context, messageIdentifier, locale, active)
            .stream()
            .map(MessageTranslationResponse::from)
            .toList();
    }

    @GetMapping("/{translationIdentifier}")
    public MessageTranslationResponse findByIdentifier(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @PathVariable String translationIdentifier
    ) {
        return MessageTranslationResponse.from(
            facade.findByIdentifier(context, messageIdentifier, translationIdentifier)
        );
    }

    @PostMapping
    public ResponseEntity<MessageTranslationResponse> create(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @RequestBody CreateMessageTranslationRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            MessageTranslationResponse.from(facade.create(context, messageIdentifier, r.toInput()))
        );
    }

    @PutMapping("/{translationIdentifier}")
    public MessageTranslationResponse update(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @PathVariable String translationIdentifier,
        @RequestBody UpdateMessageTranslationRequest r
    ) {
        return MessageTranslationResponse.from(
            facade.update(context, messageIdentifier, translationIdentifier, r.toInput())
        );
    }

    @PatchMapping("/{translationIdentifier}/activate")
    public MessageTranslationResponse activate(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @PathVariable String translationIdentifier
    ) {
        return MessageTranslationResponse.from(facade.activate(context, messageIdentifier, translationIdentifier));
    }

    @PatchMapping("/{translationIdentifier}/inactivate")
    public MessageTranslationResponse inactivate(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @PathVariable String translationIdentifier
    ) {
        return MessageTranslationResponse.from(facade.inactivate(context, messageIdentifier, translationIdentifier));
    }

    @DeleteMapping("/{translationIdentifier}")
    public ResponseEntity<Void> delete(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @PathVariable String translationIdentifier
    ) {
        facade.delete(context, messageIdentifier, translationIdentifier);
        return ResponseEntity.noContent().build();
    }
}
