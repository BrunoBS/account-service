package br.com.portalmanager.account.api.request;

import br.com.portalmanager.account.application.model.CreateAccountCommand;

import java.util.List;

public record CreateAccountRequest(
        String accountType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        List<ApproverRequest> approvers
) {
    public CreateAccountCommand toCommand() {
        return new CreateAccountCommand(
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
                        .toList()
        );
    }
}
