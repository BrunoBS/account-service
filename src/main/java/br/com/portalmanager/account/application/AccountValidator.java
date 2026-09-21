package br.com.portalmanager.account.application;

import br.com.portalmanager.account.application.model.ApproverCommand;
import br.com.portalmanager.account.application.model.CreateAccountCommand;
import br.com.portalmanager.account.application.model.UpdateAccountCommand;
import br.com.portalmanager.account.domain.AccountType;
import br.com.portalmanager.account.persistence.AccountRepository;
import br.com.portalmanager.core.messaging.exception.ValidationException;
import br.com.portalmanager.core.messaging.validation.ValidationResult;
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
            result.addLiteralError("request", "A requisição é obrigatória.");
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
            result.addLiteralError("name", "Já existe uma conta com esse nome.");
        }

        rejectIfInvalid(result);
    }

    public void validateForUpdate(Long id, UpdateAccountCommand command) {
        ValidationResult result = new ValidationResult();

        if (command == null) {
            result.addLiteralError("request", "A requisição é obrigatória.");
            rejectIfInvalid(result);
            return;
        }

        if (command.version() == null || command.version() < 0) {
            result.addLiteralError("version", "A versão da conta é obrigatória para atualização.");
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
            result.addLiteralError("name", "Já existe uma conta com esse nome.");
        }

        rejectIfInvalid(result);
    }

    public void validateTypeFilter(String normalizedTypeName) {
        if (normalizedTypeName == null) {
            return;
        }

        ValidationResult result = new ValidationResult();
        if (!isValidAccountType(normalizedTypeName)) {
            result.addLiteralError("typeName", "O filtro de tipo deve ser ADMIN ou MANAGER.");
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
            result.addLiteralError("accountType", "O tipo de conta deve ser ADMIN ou MANAGER.");
        }

        if (name == null || name.isBlank()) {
            result.addLiteralError("name", "O nome da conta é obrigatório.");
        } else if (name.length() < 3 || name.length() > 100) {
            result.addLiteralError("name", "O nome da conta deve possuir entre 3 e 100 caracteres.");
        }

        if (description == null || description.isBlank()
                || description.length() < 10 || description.length() > 500) {
            result.addLiteralError("description", "A descrição deve possuir entre 10 e 500 caracteres.");
        }

        if (requester == null || requester.isBlank() || requester.length() < 5) {
            result.addLiteralError("requester", "O requester deve possuir pelo menos 5 caracteres.");
        }

        if (acronym == null || acronym.isBlank()) {
            result.addLiteralError("acronym", "O acrônimo é obrigatório.");
        } else if (acronym.length() > 5) {
            result.addLiteralError("acronym", "O acrônimo deve possuir no máximo 5 caracteres.");
        }

        if (!isEmail(emailGroup)) {
            result.addLiteralError("emailGroup", "O e-mail informado é inválido.");
        }

        validateApprovers(approvers, result);
    }

    private void validateApprovers(List<ApproverCommand> approvers, ValidationResult result) {
        if (approvers == null || approvers.isEmpty()) {
            result.addLiteralError("approvers", "A conta deve possuir pelo menos um approver.");
            return;
        }

        for (int index = 0; index < approvers.size(); index++) {
            ApproverCommand approver = approvers.get(index);
            String path = "approvers[" + index + "]";

            if (approver == null) {
                result.addLiteralError(path, "O approver é obrigatório.");
                continue;
            }

            if (approver.functional() == null || approver.functional().isBlank()) {
                result.addLiteralError(path + ".functional", "O funcional do approver é obrigatório.");
            }

            if (!isEmail(approver.email())) {
                result.addLiteralError(path + ".email", "O e-mail do approver é inválido.");
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
