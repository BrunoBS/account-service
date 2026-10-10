package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.SharedContractInput;

public record SharedContractRequest(String name, String description) {
    public SharedContractInput toInput() {
        return new SharedContractInput(name, description);
    }
}
