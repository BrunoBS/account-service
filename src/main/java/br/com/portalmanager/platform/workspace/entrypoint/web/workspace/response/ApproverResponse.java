package br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverOutput;

public record ApproverResponse(
        String functional,
        String email
) {
    public static ApproverResponse from(ApproverOutput output) {
        return new ApproverResponse(output.functional(), output.email());
    }
}
