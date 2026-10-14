package br.com.portalmanager.platform.workspace.feature.shared.integration;

import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureSharingReferencePort;
import org.springframework.stereotype.Component;

@Component
public class SharedFeatureReferenceAdapter implements FeatureSharingReferencePort {

    private final SharedContractRepository contracts;

    public SharedFeatureReferenceAdapter(SharedContractRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public boolean hasSharedContracts(Long featureId) {
        return contracts.findFirstContractIdByFeatureForShare(featureId).isPresent();
    }
}
