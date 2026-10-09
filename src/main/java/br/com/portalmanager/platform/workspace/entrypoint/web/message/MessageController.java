package br.com.portalmanager.platform.workspace.entrypoint.web.message;

import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.CreateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.request.UpdateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.message.response.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.portalmanager.platform.workspace.feature.message.facade.MessageFacade;

@RestController
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageFacade facade;
    public MessageController(MessageFacade facade) { this.facade = facade; }

    @PostMapping
    public ResponseEntity<MessageResponse> create(@RequestBody CreateMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.create(request));
    }
    @GetMapping("/{identifier}")
    public MessageResponse findByIdentifier(@PathVariable String identifier) { return facade.findByIdentifier(identifier); }
    @GetMapping
    public List<MessageResponse> findAll(@RequestParam(required = false) String microserviceIdentifier,
                                          @RequestParam(required = false) Boolean active,
                                          @RequestParam(required = false) String code,
                                          @RequestParam(required = false) String messageKey) {
        return facade.findAll(microserviceIdentifier, active, code, messageKey);
    }
    @PutMapping("/{identifier}")
    public MessageResponse update(@PathVariable String identifier, @RequestBody UpdateMessageRequest request) { return facade.update(identifier, request); }
    @PatchMapping("/{identifier}/activate")
    public MessageResponse activate(@PathVariable String identifier) { return facade.activate(identifier); }
    @PatchMapping("/{identifier}/inactivate")
    public MessageResponse inactivate(@PathVariable String identifier) { return facade.inactivate(identifier); }
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) { facade.delete(identifier); return ResponseEntity.noContent().build(); }
}
