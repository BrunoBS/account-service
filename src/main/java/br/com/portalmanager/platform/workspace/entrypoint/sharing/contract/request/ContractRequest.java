package br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.request;

import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractInput;

public record ContractRequest(String description, String featureIdentifier) {
    public ContractInput toInput() {
        return new ContractInput(description, featureIdentifier);
    }
}
