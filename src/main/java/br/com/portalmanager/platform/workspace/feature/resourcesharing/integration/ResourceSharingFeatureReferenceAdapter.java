package br.com.portalmanager.platform.workspace.feature.resourcesharing.integration;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureSharingReferencePort;

import org.springframework.stereotype.Component;

@Component
public class ResourceSharingFeatureReferenceAdapter implements FeatureSharingReferencePort {

    private final ContractRepository contracts;

    public ResourceSharingFeatureReferenceAdapter(ContractRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public boolean hasResourceSharingContracts(Long featureId) {
        return contracts.findFirstContractIdByFeatureForShare(featureId).isPresent();
    }
}
