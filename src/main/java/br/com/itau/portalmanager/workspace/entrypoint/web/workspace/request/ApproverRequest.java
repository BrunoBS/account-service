package br.com.itau.portalmanager.workspace.entrypoint.web.workspace.request;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.ApproverInput;

public record ApproverRequest(
        String functional,
        String email
) {
    public ApproverInput toInput() {
        return new ApproverInput(functional, email);
    }
}
