package br.com.portalmanager.platform.workspace.entrypoint.scheduling;

import br.com.portalmanager.platform.library.scheduling.api.annotation.DisableScheduledLock;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.operations.AuthorizationCacheRefreshService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheRefreshJob {
    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationCacheRefreshJob.class);
    private final AuthorizationCacheRefreshService refresh;

    public AuthorizationCacheRefreshJob(AuthorizationCacheRefreshService refresh) {
        this.refresh = refresh;
    }

    @DisableScheduledLock
    @Scheduled(fixedRateString = "${workspace.authorization-cache.scheduler.interval:PT25M}")
    public void refresh() {
        var result = refresh.refresh(List.of(), true);
        LOG.info("Authorization cache refresh finished: busy={}, refreshed={}, failures={}",
                result.busy(), result.refreshed(), result.failedWorkspaces().size());
    }
}
