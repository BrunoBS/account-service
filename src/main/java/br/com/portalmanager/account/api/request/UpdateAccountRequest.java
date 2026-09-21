package br.com.portalmanager.account.api.request;

import br.com.portalmanager.account.application.model.UpdateAccountCommand;

import java.util.List;

public record UpdateAccountRequest(
        Long version,
        String accountType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        List<ApproverRequest> approvers,
        List<String> tags
) {
    public UpdateAccountCommand toCommand() {
        return new UpdateAccountCommand(
                version,
                accountType,
                name,
                description,
                requester,
                acronym,
                authorizerGroup,
                settings,
                emailGroup,
                approvers == null ? null : approvers.stream()
                        .map(value -> value == null ? null : value.toCommand())
                        .toList(),
                tags
        );
    }
}
