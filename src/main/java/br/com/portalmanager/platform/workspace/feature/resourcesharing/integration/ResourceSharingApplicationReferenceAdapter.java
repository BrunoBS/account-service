package br.com.portalmanager.platform.workspace.feature.resourcesharing.integration;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.ApplicationSharingReferencePort;

import org.springframework.stereotype.Component;

@Component
public class ResourceSharingApplicationReferenceAdapter implements ApplicationSharingReferencePort {

    private final ContractRepository contracts;

    public ResourceSharingApplicationReferenceAdapter(ContractRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public boolean hasResourceSharingContracts(Long applicationId) {
        return contracts.findFirstContractIdByOwnerApplicationForUpdate(applicationId).isPresent();
    }
}
