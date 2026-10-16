package br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.request.CreateMessageTranslatorRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.request.UpdateMessageTranslatorRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.dynamicbundle.messagetranslator.response.MessageTranslatorResponse;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.facade.messagetranslator.MessageTranslatorFacade;

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
@RequestMapping("/api/v1/dynamic-bundles/messages/{messageIdentifier}/translators")
public class MessageTranslatorController {

    private final MessageTranslatorFacade facade;

    public MessageTranslatorController(MessageTranslatorFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public PagedModel<MessageTranslatorResponse> findAll(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) String lifecycle,
            Pageable pageable) {
        return new PagedModel<>(
                facade.findAll(context, messageIdentifier, locale, lifecycle, pageable)
                        .map(MessageTranslatorResponse::from));
    }

    @GetMapping("/{translatorIdentifier}")
    public MessageTranslatorResponse findByIdentifier(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @PathVariable String translatorIdentifier) {
        return MessageTranslatorResponse.from(
                facade.findByIdentifier(context, messageIdentifier, translatorIdentifier));
    }

    @PostMapping
    public ResponseEntity<MessageTranslatorResponse> create(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @RequestBody CreateMessageTranslatorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        MessageTranslatorResponse.from(
                                facade.create(context, messageIdentifier, request.toInput())));
    }

    @PutMapping("/{translatorIdentifier}")
    public MessageTranslatorResponse update(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @PathVariable String translatorIdentifier,
            @RequestBody UpdateMessageTranslatorRequest request) {
        return MessageTranslatorResponse.from(
                facade.update(context, messageIdentifier, translatorIdentifier, request.toInput()));
    }

    @PatchMapping("/{translatorIdentifier}/activate")
    public MessageTranslatorResponse activate(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @PathVariable String translatorIdentifier) {
        return MessageTranslatorResponse.from(
                facade.activate(context, messageIdentifier, translatorIdentifier));
    }

    @PatchMapping("/{translatorIdentifier}/inactivate")
    public MessageTranslatorResponse inactivate(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @PathVariable String translatorIdentifier) {
        return MessageTranslatorResponse.from(
                facade.inactivate(context, messageIdentifier, translatorIdentifier));
    }

    @DeleteMapping("/{translatorIdentifier}")
    public ResponseEntity<Void> delete(
            AuthorizationContext context,
            @PathVariable String messageIdentifier,
            @PathVariable String translatorIdentifier) {
        facade.delete(context, messageIdentifier, translatorIdentifier);
        return ResponseEntity.noContent().build();
    }
}
