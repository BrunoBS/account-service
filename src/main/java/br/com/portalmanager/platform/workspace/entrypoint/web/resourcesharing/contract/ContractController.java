package br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.contract;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.contract.request.ContractRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.contract.response.ContractPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.contract.response.ContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.request.ParticipantConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.request.ParticipantStatusRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.response.ParticipantPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.participant.response.ParticipantResponse;
import br.com.portalmanager.platform.workspace.feature.resourcesharing.facade.contract.ContractFacade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/resource-sharing/contracts")
public class ContractController {

    private final ContractFacade facade;

    public ContractController(ContractFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<ContractResponse> create(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @RequestBody ContractRequest input) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ContractResponse.from(
                                facade.create(
                                        context,
                                        workspaceIdentifier,
                                        applicationIdentifier,
                                        input == null ? null : input.toInput())));
    }

    @GetMapping
    public ContractPageResponse list(
            AuthorizationContext context,
            @RequestParam(required = false) String lifecycle,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "20") String size) {
        return ContractPageResponse.from(
                facade.list(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        lifecycle,
                        page,
                        size));
    }

    @GetMapping("/{contractIdentifier}")
    public ContractResponse find(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier) {
        return ContractResponse.from(
                facade.find(
                        context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }

    @PutMapping("/{contractIdentifier}")
    public ContractResponse update(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier,
            @RequestBody ContractRequest input) {
        return ContractResponse.from(
                facade.update(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        contractIdentifier,
                        input == null ? null : input.toInput()));
    }

    @PatchMapping("/{contractIdentifier}/activate")
    public ContractResponse activate(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier) {
        return ContractResponse.from(
                facade.activate(
                        context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }

    @PatchMapping("/{contractIdentifier}/inactivate")
    public ContractResponse inactivate(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier) {
        return ContractResponse.from(
                facade.inactivate(
                        context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }

    @DeleteMapping("/{contractIdentifier}")
    public ResponseEntity<Void> delete(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier) {
        facade.delete(context, workspaceIdentifier, applicationIdentifier, contractIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contractIdentifier}/participants")
    public ParticipantPageResponse participants(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier,
            @RequestParam(required = false) String participantName,
            @RequestParam(required = false) String participantApplicationIdentifier,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "20") String size) {
        return ParticipantPageResponse.from(
                facade.participants(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        contractIdentifier,
                        participantName,
                        participantApplicationIdentifier,
                        status,
                        page,
                        size));
    }

    @PatchMapping("/{contractIdentifier}/participants/{participantIdentifier}/status")
    public ParticipantResponse changeParticipantStatus(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier,
            @PathVariable String participantIdentifier,
            @RequestBody(required = false) ParticipantStatusRequest input) {
        return ParticipantResponse.from(
                facade.changeParticipantStatus(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        contractIdentifier,
                        participantIdentifier,
                        input == null ? null : input.toInput()));
    }

    @GetMapping("/{contractIdentifier}/participants/{participantIdentifier}")
    public ParticipantResponse findParticipant(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier,
            @PathVariable String participantIdentifier) {
        return ParticipantResponse.from(
                facade.participant(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        contractIdentifier,
                        participantIdentifier));
    }

    @PutMapping("/{contractIdentifier}/participants/{participantIdentifier}/configuration")
    public ParticipantResponse updateConfiguration(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String contractIdentifier,
            @PathVariable String participantIdentifier,
            @RequestBody(required = false) ParticipantConfigurationRequest input) {
        return ParticipantResponse.from(
                facade.updateConfiguration(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        contractIdentifier,
                        participantIdentifier,
                        input == null ? null : input.toInput()));
    }
}
