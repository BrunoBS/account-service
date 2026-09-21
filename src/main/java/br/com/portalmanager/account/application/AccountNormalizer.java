package br.com.portalmanager.account.application;

import br.com.portalmanager.account.application.model.ApproverCommand;
import br.com.portalmanager.account.application.model.CreateAccountCommand;
import br.com.portalmanager.account.application.model.UpdateAccountCommand;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class AccountNormalizer {

    public CreateAccountCommand normalize(CreateAccountCommand command) {
        if (command == null) {
            return null;
        }

        return new CreateAccountCommand(
                normalizeType(command.accountType()),
                trim(command.name()),
                trim(command.description()),
                trim(command.requester()),
                trim(command.acronym()),
                trimOptional(command.authorizerGroup()),
                command.settings(),
                trim(command.emailGroup()),
                normalizeApprovers(command.approvers())
        );
    }

    public UpdateAccountCommand normalize(UpdateAccountCommand command) {
        if (command == null) {
            return null;
        }

        return new UpdateAccountCommand(
                command.version(),
                normalizeType(command.accountType()),
                trim(command.name()),
                trim(command.description()),
                trim(command.requester()),
                trim(command.acronym()),
                trimOptional(command.authorizerGroup()),
                command.settings(),
                trim(command.emailGroup()),
                normalizeApprovers(command.approvers())
        );
    }

    public String normalizeTypeFilter(String value) {
        return normalizeType(value);
    }

    private List<ApproverCommand> normalizeApprovers(List<ApproverCommand> approvers) {
        if (approvers == null) {
            return null;
        }

        return approvers.stream()
                .map(this::normalizeApprover)
                .toList();
    }

    private ApproverCommand normalizeApprover(ApproverCommand approver) {
        if (approver == null) {
            return null;
        }
        return new ApproverCommand(trim(approver.functional()), trim(approver.email()));
    }

    private String normalizeType(String value) {
        String normalized = trimOptional(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String trimOptional(String value) {
        String normalized = trim(value);
        return normalized == null || normalized.isBlank() ? null : normalized;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
