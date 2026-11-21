package br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations;

import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.port.WorkspaceAuthorizationPort;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.port.WorkspaceAuthorizationPort.AuthorizationGroup;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.integration.RedisAuthorizationCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheRefreshService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCacheRefreshService.class);
    private final WorkspaceAuthorizationPort workspaces;
    private final RedisAuthorizationCache cache;

    public AuthorizationCacheRefreshService(WorkspaceAuthorizationPort workspaces, RedisAuthorizationCache cache) {
        this.workspaces = workspaces;
        this.cache = cache;
    }

    public Result refresh(List<UUID> workspaces, boolean all) {
        List<UUID> failures = new ArrayList<>();
        int refreshed = all ? refreshAll(failures) : refreshPage(workspaces, failures);
        return new Result(refreshed, List.copyOf(failures));
    }

    private int refreshAll(List<UUID> failures) {
        int refreshed = 0;
        String cursor = "";
        List<String> page = workspaces.findWorkspaceIdentifiers(cursor);
        while (!page.isEmpty()) {
            refreshed += refreshPage(page.stream().map(UUID::fromString).toList(), failures);
            cursor = page.getLast();
            page = workspaces.findWorkspaceIdentifiers(cursor);
        }
        return refreshed;
    }

    private int refreshPage(List<UUID> workspaces, List<UUID> failures) {
        int refreshed = 0;
        for (UUID workspace : workspaces) {
            try {
                refreshWorkspace(workspace);
                refreshed++;
            } catch (RuntimeException exception) {
                failures.add(workspace);
                LOG.error("Authorization cache refresh failed for workspace {}", workspace, exception);
            }
        }
        return refreshed;
    }

    private void refreshWorkspace(UUID workspace) {
        String prefix = "AUTH::" + workspace + "::";
        Map<String, String> groups = new HashMap<>();
        for (AuthorizationGroup row : workspaces.findGroups(workspace.toString())) {
            String key = prefix + scope(row.environmentIdentifier()) + "::" + scope(row.applicationIdentifier());
            String group = row.authorizerGroup();
            if (group == null || group.isBlank()) {
                throw new IllegalStateException("Authorization view returned an empty group");
            }
            String existing = groups.putIfAbsent(key, group);
            if (existing != null && !existing.equals(group)) {
                throw new IllegalStateException("Authorization view returned conflicting groups for " + key);
            }
        }
        cache.reconcile(prefix, groups);
    }

    private String scope(String identifier) {
        return identifier == null ? "NONE" : UUID.fromString(identifier).toString();
    }

    public record Result(int refreshed, List<UUID> failedWorkspaces) {}
}
