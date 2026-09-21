package br.com.portalmanager.account.api.response;

import br.com.portalmanager.account.application.model.AccountResult;

import java.time.LocalDateTime;
import java.util.List;

public record AccountResponse(
        Long id,
        Long version,
        String identifier,
        String accountType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        boolean onboarding,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApproverResponse> approvers
) {
    public static AccountResponse from(AccountResult result) {
        return new AccountResponse(
                result.id(),
                result.version(),
                result.identifier(),
                result.accountType().name(),
                result.name(),
                result.description(),
                result.requester(),
                result.acronym(),
                result.authorizerGroup(),
                result.settings(),
                result.emailGroup(),
                result.onboarding(),
                result.lifecycle().name(),
                result.createdAt(),
                result.updatedAt(),
                result.approvers().stream().map(ApproverResponse::from).toList()
        );
    }
}
