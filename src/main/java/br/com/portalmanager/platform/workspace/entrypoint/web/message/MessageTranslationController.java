package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.CreateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.UpdateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageTranslationResponse;
import br.com.portalmanager.platform.workspace.feature.message.facade.MessageTranslationFacade;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
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

@RestController
@RequestMapping("/api/v1/messages/{messageIdentifier}/translations")
public class MessageTranslationController {

    private final MessageTranslationFacade facade;

    public MessageTranslationController(MessageTranslationFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public PagedModel<MessageTranslationResponse> findAll(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @RequestParam(required = false) String locale,
        @RequestParam(required = false) String lifecycle,
        Pageable pageable
    ) {
        return new PagedModel<>(
            facade.findAll(context, messageIdentifier, locale, lifecycle, pageable)
                .map(MessageTranslationResponse::from)
        );
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
        @RequestBody CreateMessageTranslationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            MessageTranslationResponse.from(facade.create(context, messageIdentifier, request.toInput()))
        );
    }

    @PutMapping("/{translationIdentifier}")
    public MessageTranslationResponse update(
        AuthorizationContext context,
        @PathVariable String messageIdentifier,
        @PathVariable String translationIdentifier,
        @RequestBody UpdateMessageTranslationRequest request
    ) {
        return MessageTranslationResponse.from(
            facade.update(context, messageIdentifier, translationIdentifier, request.toInput())
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
