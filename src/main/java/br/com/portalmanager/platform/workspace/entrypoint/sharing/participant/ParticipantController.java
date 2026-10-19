package br.com.portalmanager.platform.workspace.entrypoint.sharing.participant;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response.ParticipantPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response.ParticipantResponse;
import br.com.portalmanager.platform.workspace.feature.sharing.facade.participant.ParticipantFacade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}")
public class ParticipantController {

    private final ParticipantFacade facade;

    public ParticipantController(ParticipantFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/resource-sharing/contracts/{contractIdentifier}/participants")
    public ResponseEntity<ParticipantResponse> requestParticipant(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ParticipantResponse.from(
                                facade.requestParticipant(
                                        context,
                                        workspaceIdentifier,
                                        applicationIdentifier,
                                        contractIdentifier)));
    }

    @GetMapping("/resource-sharing/participants")
    public ParticipantPageResponse listParticipants(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String contractIdentifier,
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "20") String size) {
        return ParticipantPageResponse.from(
                facade.listParticipants(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        status,
                        contractIdentifier,
                        page,
                        size));
    }

    @GetMapping("/resource-sharing/participants/{participantIdentifier}")
    public ParticipantResponse findParticipant(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String participantIdentifier) {
        return ParticipantResponse.from(
                facade.findParticipant(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        participantIdentifier));
    }

    @PostMapping("/resource-sharing/participants/{participantIdentifier}/request")
    public ParticipantResponse requestParticipantAgain(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String participantIdentifier) {
        return ParticipantResponse.from(
                facade.requestParticipantAgain(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        participantIdentifier));
    }

    @DeleteMapping("/resource-sharing/participants/{participantIdentifier}")
    public ResponseEntity<Void> deleteParticipant(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String participantIdentifier) {
        facade.deleteParticipant(
                context, workspaceIdentifier, applicationIdentifier, participantIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/resource-sharing/contracts/available")
    public ContractPageResponse listContracts(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "20") String size,
            @RequestParam(required = false) String ownerWorkspaceIdentifier,
            @RequestParam(required = false) String ownerApplicationIdentifier) {
        return ContractPageResponse.from(
                facade.listContracts(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        page,
                        size,
                        ownerWorkspaceIdentifier,
                        ownerApplicationIdentifier));
    }

    @GetMapping("/resource-sharing/contracts/available/{contractIdentifier}")
    public ContractResponse findContract(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier) {
        return ContractResponse.from(
                facade.findContract(
                        context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }
}
