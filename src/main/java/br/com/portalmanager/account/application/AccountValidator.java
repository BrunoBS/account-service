package br.com.portalmanager.account.application;

import br.com.portalmanager.account.application.model.ApproverCommand;
import br.com.portalmanager.account.application.model.CreateAccountCommand;
import br.com.portalmanager.account.application.model.UpdateAccountCommand;
import br.com.portalmanager.account.domain.AccountType;
import br.com.portalmanager.account.persistence.AccountRepository;
import br.com.portalmanager.platform.messaging.exception.ValidationException;
import br.com.portalmanager.platform.messaging.validation.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class AccountValidator {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final AccountRepository repository;

    public AccountValidator(AccountRepository repository) {
        this.repository = repository;
    }

    public void validateForCreate(CreateAccountCommand command) {
        ValidationResult result = new ValidationResult();

        if (command == null) {
            result.addError("request", AccountMessageKeys.NAME_REQUIRED);
            rejectIfInvalid(result);
            return;
        }

        validateCommon(
                command.accountType(),
                command.name(),
                command.description(),
                command.requester(),
                command.acronym(),
                command.emailGroup(),
                command.approvers(),
                result
        );

        if (command.name() != null && repository.existsByName(command.name())) {
            result.addError("name", AccountMessageKeys.NAME_DUPLICATE);
        }

        rejectIfInvalid(result);
    }

    public void validateForUpdate(Long id, UpdateAccountCommand command) {
        ValidationResult result = new ValidationResult();

        if (command == null) {
            result.addError("request", AccountMessageKeys.NAME_REQUIRED);
            rejectIfInvalid(result);
            return;
        }

        if (command.version() == null || command.version() < 0) {
            result.addError("version", AccountMessageKeys.VERSION_REQUIRED);
        }

        validateCommon(
                command.accountType(),
                command.name(),
                command.description(),
                command.requester(),
                command.acronym(),
                command.emailGroup(),
                command.approvers(),
                result
        );

        if (command.name() != null && repository.existsByNameAndIdNot(command.name(), id)) {
            result.addError("name", AccountMessageKeys.NAME_DUPLICATE);
        }

        rejectIfInvalid(result);
    }

    public void validateTypeFilter(String normalizedTypeName) {
        if (normalizedTypeName == null) {
            return;
        }

        ValidationResult result = new ValidationResult();
        if (!isValidAccountType(normalizedTypeName)) {
            result.addError("typeName", AccountMessageKeys.TYPE_FILTER_INVALID);
        }
        rejectIfInvalid(result);
    }

    private void validateCommon(
            String accountType,
            String name,
            String description,
            String requester,
            String acronym,
            String emailGroup,
            List<ApproverCommand> approvers,
            ValidationResult result
    ) {
        if (!isValidAccountType(accountType)) {
            result.addError("accountType", AccountMessageKeys.ACCOUNT_TYPE_INVALID);
        }

        if (name == null || name.isBlank()) {
            result.addError("name", AccountMessageKeys.NAME_REQUIRED);
        } else if (name.length() < 3 || name.length() > 100) {
            result.addError("name", AccountMessageKeys.NAME_SIZE);
        }

        if (description == null || description.isBlank()
                || description.length() < 10 || description.length() > 500) {
            result.addError("description", AccountMessageKeys.DESCRIPTION_SIZE);
        }

        if (requester == null || requester.isBlank() || requester.length() < 5) {
            result.addError("requester", AccountMessageKeys.REQUESTER_SIZE);
        }

        if (acronym == null || acronym.isBlank()) {
            result.addError("acronym", AccountMessageKeys.ACRONYM_REQUIRED);
        } else if (acronym.length() > 5) {
            result.addError("acronym", AccountMessageKeys.ACRONYM_SIZE);
        }

        if (!isEmail(emailGroup)) {
            result.addError("emailGroup", AccountMessageKeys.EMAIL_INVALID);
        }

        validateApprovers(approvers, result);
    }

    private void validateApprovers(List<ApproverCommand> approvers, ValidationResult result) {
        if (approvers == null || approvers.isEmpty()) {
            result.addError("approvers", AccountMessageKeys.APPROVERS_REQUIRED);
            return;
        }

        for (int index = 0; index < approvers.size(); index++) {
            ApproverCommand approver = approvers.get(index);
            String path = "approvers[" + index + "]";

            if (approver == null) {
                result.addError(path, AccountMessageKeys.APPROVERS_REQUIRED);
                continue;
            }

            if (approver.functional() == null || approver.functional().isBlank()) {
                result.addError(path + ".functional", AccountMessageKeys.APPROVER_FUNCTIONAL_REQUIRED);
            }

            if (!isEmail(approver.email())) {
                result.addError(path + ".email", AccountMessageKeys.EMAIL_INVALID);
            }
        }
    }

    private boolean isValidAccountType(String value) {
        if (value == null) {
            return false;
        }

        try {
            AccountType.valueOf(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    private void rejectIfInvalid(ValidationResult result) {
        if (result.hasErrors()) {
            throw new ValidationException(result);
        }
    }
}
