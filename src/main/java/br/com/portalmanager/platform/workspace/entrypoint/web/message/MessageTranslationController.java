package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.workspace.feature.message.facade.MessageTranslationFacade;

import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.CreateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.UpdateMessageTranslationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageTranslationResponse;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationCommandService;
import br.com.portalmanager.platform.workspace.feature.message.usecase.operations.translation.MessageTranslationQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/messages/{messageIdentifier}/translations")
public class MessageTranslationController {
    private final MessageTranslationFacade facade;


    public MessageTranslationController(MessageTranslationFacade facade) { this.facade = facade; }

    @GetMapping

    public List<MessageTranslationResponse> findAll(
            @PathVariable String messageIdentifier,
            @RequestParam(required = false) String locale,
            @RequestParam(required = false) Boolean active
    ) {
        return facade.findAll(messageIdentifier, locale, active);
    }

    @GetMapping("/{translationIdentifier}")

    public MessageTranslationResponse findByIdentifier(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return facade.findByIdentifier(messageIdentifier, translationIdentifier);
    }

    @PostMapping

    public ResponseEntity<MessageTranslationResponse> create(
            @PathVariable String messageIdentifier,
            @RequestBody CreateMessageTranslationRequest request
    ) {
        return facade.create(messageIdentifier, request);
    }

    @PutMapping("/{translationIdentifier}")

    public MessageTranslationResponse update(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier,
            @RequestBody UpdateMessageTranslationRequest request
    ) {
        return facade.update(messageIdentifier, translationIdentifier, request);
    }

    @PatchMapping("/{translationIdentifier}/activate")

    public MessageTranslationResponse activate(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return facade.activate(messageIdentifier, translationIdentifier);
    }

    @PatchMapping("/{translationIdentifier}/inactivate")

    public MessageTranslationResponse inactivate(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return facade.inactivate(messageIdentifier, translationIdentifier);
    }

    @DeleteMapping("/{translationIdentifier}")

    public ResponseEntity<Void> delete(
            @PathVariable String messageIdentifier,
            @PathVariable String translationIdentifier
    ) {
        return facade.delete(messageIdentifier, translationIdentifier);
    }
}
