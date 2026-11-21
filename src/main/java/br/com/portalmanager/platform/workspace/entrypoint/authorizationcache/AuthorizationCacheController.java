package br.com.portalmanager.platform.workspace.entrypoint.authorizationcache;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.facade.AuthorizationCacheFacade;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations.AuthorizationCacheRefreshService;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/authorization-cache")
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheController {
    private final AuthorizationCacheFacade facade;

    public AuthorizationCacheController(AuthorizationCacheFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthorizationCacheRefreshService.Result> refresh(
            AuthorizationContext context, @RequestBody RefreshRequest request) {
        List<UUID> workspaces = request.workspaceIdentifiers() == null ? List.of() : request.workspaceIdentifiers();
        if (request.all() == !workspaces.isEmpty() || workspaces.stream().anyMatch(Objects::isNull)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Provide all=true or a non-empty list of workspace identifiers");
        }
        var result = facade.refresh(context, workspaces.stream().distinct().toList(), request.all());
        return ResponseEntity.ok(result);
    }

    public record RefreshRequest(boolean all, List<UUID> workspaceIdentifiers) {}
}
