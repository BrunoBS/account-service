package br.com.portalmanager.platform.workspace.feature.shared.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.SharedCommandService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.SharedQueryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SharedOwnerFacade {
    private final SharedCommandService command;
    private final SharedQueryService query;
    public SharedOwnerFacade(SharedCommandService command, SharedQueryService query) { this.command = command; this.query = query; }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.CREATE)
    public SharedContractOutput create(AuthorizationContext context, String w, String a, SharedContractInput input) { return command.createContract(w,a,input); }
    @AuthorizationRequired(level=AuthorizationLevel.DEV, action=AuthorizationAction.READ)
    public List<SharedContractOutput> list(AuthorizationContext context, String w, String a) { return query.listContracts(w,a); }
    @AuthorizationRequired(level=AuthorizationLevel.DEV, action=AuthorizationAction.READ)
    public SharedContractOutput find(AuthorizationContext context, String w, String a, String c) { return query.findContract(w,a,c); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.UPDATE)
    public SharedContractOutput update(AuthorizationContext context, String w, String a, String c, SharedContractInput input) { return command.updateContract(w,a,c,input); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.ACTIVATE)
    public SharedContractOutput activate(AuthorizationContext context, String w, String a, String c) { return command.activateContract(w,a,c); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.DEACTIVATE)
    public SharedContractOutput inactivate(AuthorizationContext context, String w, String a, String c) { return command.inactivateContract(w,a,c); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String w, String a, String c) { command.deleteContract(w,a,c); }
    @AuthorizationRequired(level=AuthorizationLevel.DEV, action=AuthorizationAction.READ)
    public List<SharedParticipationOutput> participants(AuthorizationContext context, String w, String a, String c,
            String name, String participantApplication, String status) {
        return query.listOwnerParticipations(w,a,c,name,participantApplication,status);
    }
    @AuthorizationRequired(level=AuthorizationLevel.DEV, action=AuthorizationAction.READ)
    public SharedParticipationOutput participation(AuthorizationContext context, String w, String a, String c, String p) {
        return query.findOwnerParticipation(w,a,c,p);
    }
    @AuthorizationRequired(level=AuthorizationLevel.DEV, action=AuthorizationAction.READ)
    public List<EnvironmentOutput> sourceEnvironments(AuthorizationContext context, String w, String a, String c, String p) {
        return query.listSourceEnvironments(w,a,c,p);
    }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.UPDATE)
    public SharedParticipationOutput approve(AuthorizationContext context, String w, String a, String c, String p, ParticipationApprovalInput input) { return command.approve(w,a,c,p,input); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.UPDATE)
    public SharedParticipationOutput reject(AuthorizationContext context, String w, String a, String c, String p) { return command.reject(w,a,c,p); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.UPDATE)
    public SharedParticipationOutput revoke(AuthorizationContext context, String w, String a, String c, String p) { return command.revoke(w,a,c,p); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.UPDATE)
    public SharedParticipationOutput publicationMode(AuthorizationContext context, String w, String a, String c, String p, PublicationModeInput input) { return command.changePublicationMode(w,a,c,p,input == null ? null : input.publicationModeCode()); }
    @AuthorizationRequired(level=AuthorizationLevel.ADM, action=AuthorizationAction.UPDATE)
    public SharedParticipationOutput mappings(AuthorizationContext context, String w, String a, String c, String p, EnvironmentMappingInput input) { return command.replaceMappings(w,a,c,p,input); }
}
