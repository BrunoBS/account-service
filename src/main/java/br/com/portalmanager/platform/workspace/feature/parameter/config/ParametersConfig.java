package br.com.portalmanager.platform.workspace.feature.parameter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ParameterProperties.class)
public class ParametersConfig {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "workspace.parameter.reconciliation",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    @EnableScheduling
    static class SchedulingConfiguration {}
}
