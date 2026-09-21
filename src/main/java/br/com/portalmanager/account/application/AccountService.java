package br.com.portalmanager.account.application;

import br.com.portalmanager.account.application.model.AccountResult;
import br.com.portalmanager.account.application.model.CreateAccountCommand;
import br.com.portalmanager.account.application.model.UpdateAccountCommand;
import br.com.portalmanager.account.domain.Account;
import br.com.portalmanager.account.domain.AccountLifecycle;
import br.com.portalmanager.account.domain.AccountType;
import br.com.portalmanager.account.persistence.AccountRepository;
import br.com.portalmanager.core.messaging.exception.NotFoundException;
import br.com.portalmanager.core.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.core.messaging.exception.ValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class AccountService {

    private final AccountRepository repository;
    private final AccountNormalizer normalizer;
    private final AccountValidator validator;

    public AccountService(
            AccountRepository repository,
            AccountNormalizer normalizer,
            AccountValidator validator
    ) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.validator = validator;
    }

    @Transactional
    public AccountResult create(CreateAccountCommand rawCommand) {
        CreateAccountCommand command = normalizer.normalize(rawCommand);
        validator.validateForCreate(command);

        LocalDateTime now = LocalDateTime.now();
        Account account = new Account(
                AccountType.valueOf(command.accountType()),
                command.name(),
                command.description(),
                command.requester(),
                command.acronym(),
                command.settings(),
                command.authorizerGroup(),
                command.emailGroup(),
                now
        );

        command.approvers().forEach(
                approver -> account.addApprover(approver.functional(), approver.email())
        );

        return AccountResult.from(repository.saveAndFlush(account));
    }

    @Transactional(readOnly = true)
    public AccountResult findById(Long id) {
        return AccountResult.from(findActive(id));
    }

    @Transactional(readOnly = true)
    public List<AccountResult> findAll(Boolean active, String typeName) {
        AccountLifecycle lifecycle = Boolean.FALSE.equals(active)
                ? AccountLifecycle.INACTIVE
                : AccountLifecycle.ACTIVE;

        String normalizedType = normalizer.normalizeTypeFilter(typeName);
        validator.validateTypeFilter(normalizedType);

        List<Account> accounts = normalizedType == null
                ? repository.findByLifecycleOrderByIdAsc(lifecycle)
                : repository.findByLifecycleAndAccountTypeOrderByIdAsc(
                        lifecycle,
                        AccountType.valueOf(normalizedType)
                );

        return accounts.stream()
                .map(AccountResult::from)
                .toList();
    }

    @Transactional
    public AccountResult update(Long id, UpdateAccountCommand rawCommand) {
        Account account = findActive(id);
        UpdateAccountCommand command = normalizer.normalize(rawCommand);
        validator.validateForUpdate(id, command);

        if (!Objects.equals(account.getVersion(), command.version())) {
            throw new ResourceVersionConflictException();
        }

        LocalDateTime now = LocalDateTime.now();
        account.update(
                AccountType.valueOf(command.accountType()),
                command.name(),
                command.description(),
                command.requester(),
                command.acronym(),
                command.settings(),
                command.authorizerGroup(),
                command.emailGroup(),
                now
        );
        account.clearApprovers();
        command.approvers().forEach(
                approver -> account.addApprover(approver.functional(), approver.email())
        );

        return AccountResult.from(repository.saveAndFlush(account));
    }

    @Transactional
    public void deactivate(Long id) {
        Account account = findActive(id);
        account.deactivate(LocalDateTime.now());
        repository.saveAndFlush(account);
    }

    @Transactional
    public AccountResult restore(Long id) {
        Account account = repository.findByIdAndLifecycle(id, AccountLifecycle.INACTIVE)
                .orElseThrow(() -> new ValidationException(AccountMessageKeys.RESTORE_INVALID));

        account.restore(LocalDateTime.now());
        return AccountResult.from(repository.saveAndFlush(account));
    }

    private Account findActive(Long id) {
        return repository.findByIdAndLifecycle(id, AccountLifecycle.ACTIVE)
                .orElseThrow(() -> new NotFoundException(AccountMessageKeys.NOT_FOUND));
    }
}
