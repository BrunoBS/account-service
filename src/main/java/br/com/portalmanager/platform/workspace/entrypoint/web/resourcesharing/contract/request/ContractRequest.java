package br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.contract.request;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractInput;

public record ContractRequest(String description, String featureIdentifier) {
    public ContractInput toInput() {
        return new ContractInput(description, featureIdentifier);
    }
}
