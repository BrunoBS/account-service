package br.com.portalmanager.platform.workspace.feature.authorizationcache.integration;

import br.com.portalmanager.platform.library.scheduling.api.contract.task.PlatformTaskScheduler;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations.AuthorizationCacheRefreshService;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations.AuthorizationCacheRefreshService.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheScheduler {
    private static final String LOCK_NAME = "workspace-authorization-cache-refresh";
    private final PlatformTaskScheduler scheduler;
    private final AuthorizationCacheRefreshService refresh;

    public AuthorizationCacheScheduler(PlatformTaskScheduler scheduler, AuthorizationCacheRefreshService refresh) {
        this.scheduler = scheduler;
        this.refresh = refresh;
    }

    public ScheduledFuture<?> schedulePeriodic(Runnable task, Duration interval) {
        return scheduler.scheduleAtFixedRate(LOCK_NAME, task, interval);
    }

    public Result refreshNow(List<UUID> workspaces, boolean all) {
        var result = new CompletableFuture<Result>();
        var task = scheduler.schedule(LOCK_NAME,
                () -> result.complete(refresh.refresh(workspaces, all)), Instant.now());
        try {
            task.get();
            return result.getNow(new Result(true, 0, List.of()));
        } catch (InterruptedException exception) {
            task.cancel(false);
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while refreshing authorization cache", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new IllegalStateException("Authorization cache refresh failed", exception.getCause());
        }
    }
}
