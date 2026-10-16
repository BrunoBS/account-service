package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.scheduler;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "platform.dynamic-parameter.reconciliation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@EnableScheduling
public class DynamicParameterReconciliationSchedulerSettings {
    @Bean
    DynamicParameterReconciliationScheduler dynamicParameterReconciliationScheduler(
            DynamicParameterRuntimeService service) {
        return new DynamicParameterReconciliationScheduler(service);
    }
}
