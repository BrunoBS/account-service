package br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.parameterkey;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.model.parameterkey.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class ParameterKeyAfterCommitSynchronization {
    private final DynamicParameterRuntimeService runtime;

    public ParameterKeyAfterCommitSynchronization(DynamicParameterRuntimeService runtime) {
        this.runtime = runtime;
    }

    public void schedule(ParameterKeyMetadata parameterKey) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        runtime.remove(parameterKey);
                    }
                });
    }
}
