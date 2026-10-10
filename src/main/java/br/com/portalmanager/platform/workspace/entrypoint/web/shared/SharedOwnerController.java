package br.com.portalmanager.platform.workspace.entrypoint.web.shared;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedContractPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedParticipationPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedParticipationResponse;
import br.com.portalmanager.platform.workspace.feature.shared.facade.SharedOwnerFacade;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/shared-contracts")
public class SharedOwnerController {

    private final SharedOwnerFacade facade;

    public SharedOwnerController(SharedOwnerFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<SharedContractResponse> create(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @RequestBody SharedContractRequest input
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SharedContractResponse.from(
                facade.create(
                    context,
                    workspaceIdentifier,
                    applicationIdentifier,
                    input == null ? null : input.toInput()
                )
            )
        );
    }

    @GetMapping
    public SharedContractPageResponse list(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @RequestParam(defaultValue = "0") String page,
        @RequestParam(defaultValue = "20") String size
    ) {
        return SharedContractPageResponse.from(
            facade.list(context, workspaceIdentifier, applicationIdentifier, page, size)
        );
    }

    @GetMapping("/{contractIdentifier}")
    public SharedContractResponse find(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier
    ) {
        return SharedContractResponse.from(
            facade.find(context, workspaceIdentifier, applicationIdentifier, contractIdentifier)
        );
    }

    @PutMapping("/{contractIdentifier}")
    public SharedContractResponse update(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier,
        @RequestBody SharedContractRequest input
    ) {
        return SharedContractResponse.from(
            facade.update(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                contractIdentifier,
                input == null ? null : input.toInput()
            )
        );
    }

    @PatchMapping("/{contractIdentifier}/activate")
    public SharedContractResponse activate(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier
    ) {
        return SharedContractResponse.from(
            facade.activate(context, workspaceIdentifier, applicationIdentifier, contractIdentifier)
        );
    }

    @PatchMapping("/{contractIdentifier}/inactivate")
    public SharedContractResponse inactivate(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier
    ) {
        return SharedContractResponse.from(
            facade.inactivate(context, workspaceIdentifier, applicationIdentifier, contractIdentifier)
        );
    }

    @DeleteMapping("/{contractIdentifier}")
    public ResponseEntity<Void> delete(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier
    ) {
        facade.delete(context, workspaceIdentifier, applicationIdentifier, contractIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{contractIdentifier}/participations")
    public SharedParticipationPageResponse participants(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier,
        @RequestParam(required = false) String participantName,
        @RequestParam(required = false) String participantApplicationIdentifier,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") String page,
        @RequestParam(defaultValue = "20") String size
    ) {
        return SharedParticipationPageResponse.from(
            facade.participants(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                contractIdentifier,
                participantName,
                participantApplicationIdentifier,
                status,
                page,
                size
            )
        );
    }

    @PatchMapping("/{contractIdentifier}/participations/{participationIdentifier}/status")
    public SharedParticipationResponse changeParticipationStatus(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier,
        @PathVariable String participationIdentifier,
        @RequestBody(required = false) ParticipationStatusRequest input
    ) {
        return SharedParticipationResponse.from(
            facade.changeParticipationStatus(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                contractIdentifier,
                participationIdentifier,
                input == null ? null : input.toInput()
            )
        );
    }

    @GetMapping("/{contractIdentifier}/participations/{participationIdentifier}")
    public SharedParticipationResponse findParticipation(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier,
        @PathVariable String participationIdentifier
    ) {
        return SharedParticipationResponse.from(
            facade.participation(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                contractIdentifier,
                participationIdentifier
            )
        );
    }

    @PutMapping("/{contractIdentifier}/participations/{participationIdentifier}/configuration")
    public SharedParticipationResponse updateConfiguration(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String applicationIdentifier,
        @PathVariable String contractIdentifier,
        @PathVariable String participationIdentifier,
        @RequestBody(required = false) ParticipationConfigurationRequest input
    ) {
        return SharedParticipationResponse.from(
            facade.updateConfiguration(
                context,
                workspaceIdentifier,
                applicationIdentifier,
                contractIdentifier,
                participationIdentifier,
                input == null ? null : input.toInput()
            )
        );
    }
}
