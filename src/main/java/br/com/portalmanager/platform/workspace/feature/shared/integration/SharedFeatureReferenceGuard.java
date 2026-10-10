package br.com.portalmanager.platform.workspace.feature.shared.integration;

import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureSharingReferenceGuard;
import org.springframework.stereotype.Component;

@Component
public class SharedFeatureReferenceGuard implements FeatureSharingReferenceGuard {

    private final SharedContractRepository contracts;

    public SharedFeatureReferenceGuard(SharedContractRepository contracts) {
        this.contracts = contracts;
    }

    public boolean hasSharedContracts(Long featureId) {
        return contracts.findFirstContractIdByFeatureForShare(featureId).isPresent();
    }
}
