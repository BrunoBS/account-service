package br.com.portalmanager.account.api.response;

import br.com.portalmanager.account.application.model.ApproverResult;

public record ApproverResponse(
        String functional,
        String email
) {
    public static ApproverResponse from(ApproverResult result) {
        return new ApproverResponse(result.functional(), result.email());
    }
}
