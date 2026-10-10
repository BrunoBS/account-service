package br.com.portalmanager.platform.workspace.core.application.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationCommandService;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import java.util.List;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

@Service
public class ApplicationFacade {

    private final ApplicationCommandService command;
    private final ApplicationQueryService query;

    public ApplicationFacade(ApplicationCommandService command, ApplicationQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ApplicationOutput create(AuthorizationContext context, String w, JsonNode p)
        throws tools.jackson.core.JacksonException {
        return command.create(w, p);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public ApplicationOutput find(AuthorizationContext context, String w, String i) {
        return query.findByIdentifier(w, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<ApplicationOutput> findAll(AuthorizationContext context, String w, Boolean a, String t) {
        return query.findAll(w, a, t);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<ApplicationSummary> summary(AuthorizationContext context, String w) {
        return query.summary(w);
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.UPDATE)
    public ApplicationOutput update(AuthorizationContext context, String w, String i, JsonNode p)
        throws tools.jackson.core.JacksonException {
        return command.update(w, i, p);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String w, String i) {
        command.inactivate(w, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public ApplicationOutput restore(AuthorizationContext context, String w, String i) {
        return command.restore(w, i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String w, String i) {
        command.delete(w, i);
    }
}
