package br.com.portalmanager.account.application.model;

import java.util.List;

public record UpdateAccountCommand(
        Long version,
        String accountType,
        String name,
        String description,
        String requester,
        String acronym,
        String authorizerGroup,
        String settings,
        String emailGroup,
        List<ApproverCommand> approvers,
        List<String> tags
) {
}
