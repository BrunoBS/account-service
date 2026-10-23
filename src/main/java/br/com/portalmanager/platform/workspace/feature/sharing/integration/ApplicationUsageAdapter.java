package br.com.portalmanager.platform.workspace.feature.sharing.integration;

import br.com.portalmanager.platform.workspace.core.application.usecase.port.ApplicationUsagePort;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.contract.ContractRepository;

import org.springframework.stereotype.Component;

@Component
public class ApplicationUsageAdapter implements ApplicationUsagePort {

    private final ContractRepository contracts;

    public ApplicationUsageAdapter(ContractRepository contracts) {
        this.contracts = contracts;
    }

    @Override
    public boolean isInUse(Long applicationId) {
        return contracts.findFirstContractIdByOwnerApplicationForUpdate(applicationId).isPresent();
    }
}
