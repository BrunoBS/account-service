package br.com.portalmanager.account.application.model;

import br.com.portalmanager.account.domain.Account;
import br.com.portalmanager.account.domain.AccountLifecycle;
import br.com.portalmanager.account.domain.AccountType;
import br.com.portalmanager.core.authorization.resource.AuthorizableResource;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record AccountResult(
        Long id,
        Long version,
        String identifier,
        AccountType accountType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        boolean onboarding,
        AccountLifecycle lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ApproverResult> approvers,
        List<String> tags
) implements AuthorizableResource {

    public static AccountResult from(Account account) {
        return from(account, List.of());
    }

    public static AccountResult from(Account account, List<String> manualTags) {
        List<ApproverResult> approvers = account.getApprovers().stream()
                .map(value -> new ApproverResult(value.getFunctional(), value.getEmail()))
                .sorted(Comparator.comparing(ApproverResult::functional)
                        .thenComparing(ApproverResult::email))
                .toList();

        return new AccountResult(
                account.getId(),
                account.getVersion(),
                account.getIdentifier(),
                account.getAccountType(),
                account.getName(),
                account.getDescription(),
                account.getRequester(),
                account.getAcronym(),
                account.getAuthorizerGroup(),
                account.getSettings(),
                account.getEmailGroup(),
                account.isOnboarding(),
                account.getLifecycle(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                approvers,
                manualTags == null ? List.of() : List.copyOf(manualTags)
        );
    }

    @Override
    public String getAuthorizerGroup() {
        return authorizerGroup;
    }
}
