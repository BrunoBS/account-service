package br.com.portalmanager.platform.workspace.entrypoint.scheduling;

import br.com.portalmanager.platform.workspace.feature.authorizationcache.integration.AuthorizationCacheScheduler;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations.AuthorizationCacheRefreshService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheRefreshJob {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCacheRefreshJob.class);
    private final AuthorizationCacheRefreshService refresh;
    private final AuthorizationCacheScheduler scheduler;
    private final Duration interval;
    private ScheduledFuture<?> scheduledRefresh;

    public AuthorizationCacheRefreshJob(AuthorizationCacheRefreshService refresh, AuthorizationCacheScheduler scheduler,
                                        @Value("${workspace.authorization-cache.scheduler.interval:PT25M}") Duration interval) {
        this.refresh = refresh;
        this.scheduler = scheduler;
        this.interval = interval;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        scheduledRefresh = scheduler.schedulePeriodic(this::refresh, interval);
    }

    @PreDestroy
    void stop() {
        if (scheduledRefresh != null) {
            scheduledRefresh.cancel(false);
        }
    }

    private void refresh() {
        var result = refresh.refresh(List.of(), true);
        LOG.info("Authorization cache refresh finished: refreshed={}, failures={}",
                result.refreshed(), result.failedWorkspaces().size());
    }
}
