package br.com.portalmanager.platform.workspace.feature.shared.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedContractOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedParticipationOutput;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.SharedCommandService;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.SharedQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SharedParticipantFacade {

    private final SharedCommandService command;
    private final SharedQueryService query;

    public SharedParticipantFacade(SharedCommandService command, SharedQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.CREATE)
    public SharedParticipationOutput request(
        AuthorizationContext context,
        String pw,
        String pa,
        String c,
        String ow,
        String oa
    ) {
        return command.requestParticipation(pw, pa, c, ow, oa);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public SharedParticipationOutput resubmit(AuthorizationContext context, String w, String a, String p) {
        return command.resubmitParticipation(w, a, p);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<SharedParticipationOutput> list(AuthorizationContext context, String w, String a) {
        return query.listParticipantParticipations(w, a);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<SharedContractOutput> available(AuthorizationContext context, String w, String a) {
        return query.listAvailableContracts(w, a);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedContractOutput available(AuthorizationContext context, String w, String a, String c) {
        return query.findAvailableContract(w, a, c);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public SharedParticipationOutput find(AuthorizationContext context, String w, String a, String p) {
        return query.findParticipantParticipation(w, a, p);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.DELETE)
    public void leave(AuthorizationContext context, String w, String a, String p) {
        command.leave(w, a, p);
    }
}
