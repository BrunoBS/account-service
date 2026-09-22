package br.com.itau.portalmanager.workspace.entrypoint.web.workspace.response;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.ApproverOutput;

public record ApproverResponse(
        String functional,
        String email
) {
    public static ApproverResponse from(ApproverOutput output) {
        return new ApproverResponse(output.functional(), output.email());
    }
}
