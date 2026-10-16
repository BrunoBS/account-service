package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.scheduler;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;

import org.springframework.scheduling.annotation.Scheduled;

public class DynamicParameterReconciliationScheduler {
    private final DynamicParameterRuntimeService service;

    public DynamicParameterReconciliationScheduler(DynamicParameterRuntimeService service) {
        this.service = service;
    }

    @Scheduled(
            fixedDelayString = "${platform.dynamic-parameter.reconciliation.interval:PT1H}",
            initialDelayString = "${platform.dynamic-parameter.reconciliation.interval:PT1H}")
    public void reconcile() {
        service.reconcile();
    }
}
