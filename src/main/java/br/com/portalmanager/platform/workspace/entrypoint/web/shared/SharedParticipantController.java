package br.com.portalmanager.platform.workspace.entrypoint.web.shared;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedContractPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedParticipationPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedParticipationResponse;
import br.com.portalmanager.platform.workspace.feature.shared.facade.SharedParticipantFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}")
public class SharedParticipantController {

    private final SharedParticipantFacade facade;

    public SharedParticipantController(SharedParticipantFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/shared-contracts/{contractIdentifier}/participations")
    public ResponseEntity<SharedParticipationResponse> requestParticipation(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SharedParticipationResponse.from(
                facade.requestParticipation(context, workspaceIdentifier, applicationIdentifier, contractIdentifier)
            )
        );
    }

    @GetMapping("/shared-participations")
    public SharedParticipationPageResponse listParticipations(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String contractIdentifier,
        @RequestParam(defaultValue = "0") String page,
        @RequestParam(defaultValue = "20") String size
    ) {
        return SharedParticipationPageResponse.from(
            facade.listParticipations(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                status,
                contractIdentifier,
                page,
                size
            )
        );
    }

    @GetMapping("/shared-participations/{participationIdentifier}")
    public SharedParticipationResponse findParticipation(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String participationIdentifier
    ) {
        return SharedParticipationResponse.from(
            facade.findParticipation(context, workspaceIdentifier, applicationIdentifier, participationIdentifier)
        );
    }

    @PostMapping("/shared-participations/{participationIdentifier}/request")
    public SharedParticipationResponse requestParticipationAgain(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String participationIdentifier
    ) {
        return SharedParticipationResponse.from(
            facade.requestParticipationAgain(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                participationIdentifier
            )
        );
    }

    @DeleteMapping("/shared-participations/{participationIdentifier}")
    public ResponseEntity<Void> deleteParticipation(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String participationIdentifier
    ) {
        facade.deleteParticipation(context, workspaceIdentifier, applicationIdentifier, participationIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/shared-contracts/available")
    public SharedContractPageResponse listContracts(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @RequestParam(defaultValue = "0") String page,
        @RequestParam(defaultValue = "20") String size,
        @RequestParam(required = false) String ownerWorkspaceIdentifier,
        @RequestParam(required = false) String ownerApplicationIdentifier
    ) {
        return SharedContractPageResponse.from(
            facade.listContracts(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                page,
                size,
                ownerWorkspaceIdentifier,
                ownerApplicationIdentifier
            )
        );
    }

    @GetMapping("/shared-contracts/available/{contractIdentifier}")
    public SharedContractResponse findContract(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier
    ) {
        return SharedContractResponse.from(
            facade.findContract(context, workspaceIdentifier, applicationIdentifier, contractIdentifier)
        );
    }
}
