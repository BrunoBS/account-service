package br.com.portalmanager.account.api.request;

import br.com.portalmanager.account.application.model.ApproverCommand;

public record ApproverRequest(
        String functional,
        String email
) {
    public ApproverCommand toCommand() {
        return new ApproverCommand(functional, email);
    }
}
