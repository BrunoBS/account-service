package br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.CreateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.UpdateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.response.MessageResponse;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.facade.message.MessageFacade;

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
@RequestMapping("/api/v1/dynamic-bundles/messages")
public class MessageController {

    private final MessageFacade facade;

    public MessageController(MessageFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> create(
            AuthorizationContext context, @RequestBody CreateMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.from(facade.create(context, request.toInput())));
    }

    @GetMapping("/{identifier}")
    public MessageResponse findByIdentifier(
            AuthorizationContext context, @PathVariable("identifier") String messageIdentifier) {
        return MessageResponse.from(facade.findByIdentifier(context, messageIdentifier));
    }

    @GetMapping
    public PagedModel<MessageResponse> findAll(
            AuthorizationContext context,
            @RequestParam(required = false) String microserviceIdentifier,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String messageKey,
            Pageable pageable) {
        return new PagedModel<>(
                facade.findAll(
                                context,
                                microserviceIdentifier,
                                lifecycle,
                                code,
                                messageKey,
                                pageable)
                        .map(MessageResponse::from));
    }

    @PutMapping("/{identifier}")
    public MessageResponse update(
            AuthorizationContext context,
            @PathVariable("identifier") String messageIdentifier,
            @RequestBody UpdateMessageRequest request) {
        return MessageResponse.from(facade.update(context, messageIdentifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public MessageResponse activate(
            AuthorizationContext context, @PathVariable("identifier") String messageIdentifier) {
        return MessageResponse.from(facade.activate(context, messageIdentifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public MessageResponse inactivate(
            AuthorizationContext context, @PathVariable("identifier") String messageIdentifier) {
        return MessageResponse.from(facade.inactivate(context, messageIdentifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(
            AuthorizationContext context, @PathVariable("identifier") String messageIdentifier) {
        facade.delete(context, messageIdentifier);
        return ResponseEntity.noContent().build();
    }
}
