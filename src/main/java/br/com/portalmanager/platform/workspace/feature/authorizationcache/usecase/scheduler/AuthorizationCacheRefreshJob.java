package br.com.portalmanager.platform.workspace.feature.authorizationcache.usecase.scheduler;

import br.com.portalmanager.platform.library.scheduling.api.annotation.DisableScheduledLock;
import br.com.portalmanager.platform.workspace.feature.authorizationcache.integration.AuthorizationCacheScheduler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(prefix = "workspace.authorization-cache", name = "enabled", havingValue = "true")
public class AuthorizationCacheRefreshJob {
    private final AuthorizationCacheScheduler scheduler;

    public AuthorizationCacheRefreshJob(AuthorizationCacheScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @DisableScheduledLock
    @Scheduled(fixedRateString = "${workspace.authorization-cache.scheduler.interval:PT25M}")
    public void refresh() {
        scheduler.submitFullRefresh();
    }
}
