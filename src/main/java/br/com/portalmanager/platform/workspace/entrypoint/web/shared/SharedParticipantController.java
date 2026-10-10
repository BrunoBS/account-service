package br.com.portalmanager.platform.workspace.entrypoint.web.shared;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.feature.shared.facade.SharedParticipantFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedParticipationResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedContractResponse;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedContractOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}")
public class SharedParticipantController {
    private final SharedParticipantFacade facade;
    public SharedParticipantController(SharedParticipantFacade facade) { this.facade = facade; }

    @PostMapping("/shared-contracts/{contractIdentifier}/participations/destinations/{ownerWorkspaceIdentifier}/applications/{ownerApplicationIdentifier}")
    public ResponseEntity<SharedParticipationResponse> request(AuthorizationContext context,
            @PathVariable String workspaceIdentifier, @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier, @PathVariable String ownerWorkspaceIdentifier,
            @PathVariable String ownerApplicationIdentifier) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SharedParticipationResponse.from(facade.request(context,
                workspaceIdentifier, applicationIdentifier, contractIdentifier, ownerWorkspaceIdentifier, ownerApplicationIdentifier)));
    }
    @GetMapping("/participations")
    public List<SharedParticipationResponse> list(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier) { return facade.list(context, workspaceIdentifier, applicationIdentifier)
            .stream().map(SharedParticipationResponse::from).toList(); }
    @GetMapping("/participations/{participationIdentifier}")
    public SharedParticipationResponse find(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String participationIdentifier) {
        return SharedParticipationResponse.from(facade.find(context, workspaceIdentifier, applicationIdentifier, participationIdentifier));
    }
    @PostMapping("/participations/{participationIdentifier}/resubmission")
    public SharedParticipationResponse resubmit(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String participationIdentifier) {
        return SharedParticipationResponse.from(facade.resubmit(context, workspaceIdentifier, applicationIdentifier, participationIdentifier));
    }

    @DeleteMapping("/participations/{participationIdentifier}")
    public ResponseEntity<Void> leave(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String participationIdentifier) {
        facade.leave(context, workspaceIdentifier, applicationIdentifier, participationIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shared-contracts/available")
    public List<SharedContractResponse> available(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier) {
        return facade.available(context, workspaceIdentifier, applicationIdentifier).stream()
                .map(SharedContractResponse::from).toList();
    }

    @GetMapping("/shared-contracts/{contractIdentifier}")
    public SharedContractResponse available(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier) {
        return SharedContractResponse.from(facade.available(context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }
}
