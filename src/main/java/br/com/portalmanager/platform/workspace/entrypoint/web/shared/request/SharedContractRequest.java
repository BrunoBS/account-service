package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractInput;

public record SharedContractRequest(String description, String featureIdentifier) {
    public SharedContractInput toInput() {
        return new SharedContractInput(description, featureIdentifier);
    }
}
