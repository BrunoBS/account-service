package br.com.portalmanager.platform.workspace.feature.shared.integration;

import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.ApplicationSharingReferenceGuard;
import org.springframework.stereotype.Component;

@Component
public class SharedApplicationReferenceGuard implements ApplicationSharingReferenceGuard {

    private final SharedContractRepository contracts;

    public SharedApplicationReferenceGuard(SharedContractRepository contracts) {
        this.contracts = contracts;
    }

    public boolean hasSharedContracts(Long applicationId) {
        return contracts.findFirstContractIdByOwnerApplicationForUpdate(applicationId).isPresent();
    }
}
