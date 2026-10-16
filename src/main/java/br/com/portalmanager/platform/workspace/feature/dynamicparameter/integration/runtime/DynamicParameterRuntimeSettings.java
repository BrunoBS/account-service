package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.scheduler.DynamicParameterReconciliationSchedulerSettings;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DynamicParameterRuntimeProperties.class)
@Import(DynamicParameterReconciliationSchedulerSettings.class)
public class DynamicParameterRuntimeSettings {}
