package br.com.portalmanager.platform.workspace.feature.sharing.integration;

import br.com.portalmanager.platform.workspace.feature.sharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureUsagePort;

import org.springframework.stereotype.Component;

@Component
public class FeatureUsageAdapter implements FeatureUsagePort {

    private final ContractRepository contracts;

    public FeatureUsageAdapter(ContractRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public boolean isInUse(Long featureId) {
        return contracts.findFirstContractIdByFeatureForShare(featureId).isPresent();
    }
}
