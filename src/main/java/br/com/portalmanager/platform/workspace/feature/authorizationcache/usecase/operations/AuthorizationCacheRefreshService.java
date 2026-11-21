package br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.integration.AuthorizationViewRepository;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.integration.RedisAuthorizationCache;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheRefreshService {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCacheRefreshService.class);
    private final AuthorizationViewRepository view;
    private final RedisAuthorizationCache cache;
    private final LockProvider lockProvider;

    public AuthorizationCacheRefreshService(AuthorizationViewRepository view, RedisAuthorizationCache cache,
                                            LockProvider lockProvider) {
        this.view = view;
        this.cache = cache;
        this.lockProvider = lockProvider;
    }

    public Result refresh(List<UUID> workspaces, boolean all) {
        var lock = lockProvider.lock(new LockConfiguration(Instant.now(),
                "workspace-authorization-cache-refresh", Duration.ofMinutes(10), Duration.ZERO));
        if (lock.isEmpty()) {
            return new Result(true, 0, List.of());
        }
        List<UUID> failures = new ArrayList<>();
        try {
            int refreshed = all ? refreshAll(failures) : refreshPage(workspaces, failures);
            return new Result(false, refreshed, List.copyOf(failures));
        } finally {
            lock.get().unlock();
        }
    }

    private int refreshAll(List<UUID> failures) {
        int refreshed = 0;
        String cursor = "";
        List<UUID> page = view.findWorkspacePage(cursor);
        while (!page.isEmpty()) {
            refreshed += refreshPage(page, failures);
            cursor = page.getLast().toString();
            page = view.findWorkspacePage(cursor);
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
        for (var row : view.findGroups(workspace)) {
            String key = prefix + scope(row.environment()) + "::" + scope(row.application());
            if (row.group() == null || row.group().isBlank()) {
                throw new IllegalStateException("Authorization view returned an empty group");
            }
            String existing = groups.putIfAbsent(key, row.group());
            if (existing != null && !existing.equals(row.group())) {
                throw new IllegalStateException("Authorization view returned conflicting groups for " + key);
            }
        }
        cache.reconcile(prefix, groups);
    }

    private String scope(UUID identifier) {
        return identifier == null ? "NONE" : identifier.toString();
    }

    public record Result(boolean busy, int refreshed, List<UUID> failedWorkspaces) {}
}
