package br.com.portalmanager.platform.workspace.entrypoint.web.shared;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.feature.shared.facade.SharedOwnerFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.shared.response.SharedParticipationResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/shared/contracts")
public class SharedOwnerController {
    private final SharedOwnerFacade facade;
    public SharedOwnerController(SharedOwnerFacade facade) { this.facade = facade; }

    @PostMapping
    public ResponseEntity<SharedContractResponse> create(AuthorizationContext context,
            @PathVariable String workspaceIdentifier, @PathVariable String applicationIdentifier,
            @RequestBody SharedContractRequest input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SharedContractResponse.from(
                facade.create(context, workspaceIdentifier, applicationIdentifier, input == null ? null : input.toInput())));
    }
    @GetMapping
    public List<SharedContractResponse> list(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier) { return facade.list(context, workspaceIdentifier, applicationIdentifier)
            .stream().map(SharedContractResponse::from).toList(); }
    @GetMapping("/{contractIdentifier}")
    public SharedContractResponse find(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier) {
        return SharedContractResponse.from(facade.find(context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }
    @PutMapping("/{contractIdentifier}")
    public SharedContractResponse update(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @RequestBody SharedContractRequest input) {
        return SharedContractResponse.from(facade.update(context, workspaceIdentifier, applicationIdentifier,
                contractIdentifier, input == null ? null : input.toInput()));
    }
    @PatchMapping("/{contractIdentifier}/activate")
    public SharedContractResponse activate(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier) {
        return SharedContractResponse.from(facade.activate(context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }
    @PatchMapping("/{contractIdentifier}/inactivate")
    public SharedContractResponse inactivate(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier) {
        return SharedContractResponse.from(facade.inactivate(context, workspaceIdentifier, applicationIdentifier, contractIdentifier));
    }
    @DeleteMapping("/{contractIdentifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier) {
        facade.delete(context, workspaceIdentifier, applicationIdentifier, contractIdentifier);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/{contractIdentifier}/participations")
    public List<SharedParticipationResponse> participants(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @RequestParam(required = false) String participantName,
            @RequestParam(required = false) String participantApplicationIdentifier,
            @RequestParam(required = false) String status) {
        return facade.participants(context, workspaceIdentifier, applicationIdentifier, contractIdentifier,
                        participantName, participantApplicationIdentifier, status)
                .stream().map(SharedParticipationResponse::from).toList();
    }
    @PostMapping("/{contractIdentifier}/participations/{participationIdentifier}/approval")
    public SharedParticipationResponse approve(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier, @RequestBody ParticipationApprovalRequest input) {
        return SharedParticipationResponse.from(facade.approve(context, workspaceIdentifier, applicationIdentifier, contractIdentifier,
                participationIdentifier, input == null ? null : input.toInput()));
    }
    @PostMapping("/{contractIdentifier}/participations/{participationIdentifier}/rejection")
    public SharedParticipationResponse reject(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier) {
        return SharedParticipationResponse.from(facade.reject(context, workspaceIdentifier, applicationIdentifier, contractIdentifier,
                participationIdentifier));
    }
    @PostMapping("/{contractIdentifier}/participations/{participationIdentifier}/revocation")
    public SharedParticipationResponse revoke(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier) {
        return SharedParticipationResponse.from(facade.revoke(context, workspaceIdentifier, applicationIdentifier, contractIdentifier,
                participationIdentifier));
    }
    @PutMapping("/{contractIdentifier}/participations/{participationIdentifier}/publication-mode")
    public SharedParticipationResponse publicationMode(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier, @RequestBody PublicationModeRequest input) {
        return SharedParticipationResponse.from(facade.publicationMode(context, workspaceIdentifier, applicationIdentifier, contractIdentifier,
                participationIdentifier, input == null ? null : input.toInput()));
    }
    @GetMapping("/{contractIdentifier}/participations/{participationIdentifier}/environment-mappings")
    public List<SharedParticipationResponse.EnvironmentMapping> mappings(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier) {
        return facade.participation(context, workspaceIdentifier, applicationIdentifier, contractIdentifier, participationIdentifier)
                .mappings().stream().map(m -> new SharedParticipationResponse.EnvironmentMapping(
                        m.sourceEnvironmentIdentifier(), m.destinationEnvironmentIdentifier())).toList();
    }
    @GetMapping("/{contractIdentifier}/participations/{participationIdentifier}/source-environments")
    public List<EnvironmentResponse> sourceEnvironments(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier) {
        return facade.sourceEnvironments(context, workspaceIdentifier, applicationIdentifier, contractIdentifier, participationIdentifier)
                .stream().map(EnvironmentResponse::from).toList();
    }
    @PutMapping("/{contractIdentifier}/participations/{participationIdentifier}/environment-mappings")
    public SharedParticipationResponse mappings(AuthorizationContext context, @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier, @PathVariable String contractIdentifier,
            @PathVariable String participationIdentifier, @RequestBody EnvironmentMappingRequest input) {
        return SharedParticipationResponse.from(facade.mappings(context, workspaceIdentifier, applicationIdentifier, contractIdentifier,
                participationIdentifier, input == null ? null : input.toInput()));
    }
}
