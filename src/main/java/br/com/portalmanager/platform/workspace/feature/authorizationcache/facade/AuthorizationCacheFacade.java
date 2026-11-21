package br.com.portalmanager.platform.workspace.feature.authorizationcache.facade;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations.AuthorizationCacheRefreshService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheFacade {
    private final AuthorizationCacheRefreshService service;

    public AuthorizationCacheFacade(AuthorizationCacheRefreshService service) {
        this.service = service;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public AuthorizationCacheRefreshService.Result refresh(AuthorizationContext context, List<UUID> workspaces,
                                                           boolean all) {
        return service.refresh(workspaces, all);
    }
}
