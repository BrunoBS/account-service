package br.com.portalmanager.platform.workspace.feature.shared.integration;

import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.ApplicationSharingReferencePort;
import org.springframework.stereotype.Component;

@Component
public class SharedApplicationReferenceAdapter implements ApplicationSharingReferencePort {

    private final SharedContractRepository contracts;

    public SharedApplicationReferenceAdapter(SharedContractRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public boolean hasSharedContracts(Long applicationId) {
        return contracts.findFirstContractIdByOwnerApplicationForUpdate(applicationId).isPresent();
    }
}
