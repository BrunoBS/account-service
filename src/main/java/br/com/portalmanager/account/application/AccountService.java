package br.com.portalmanager.account.application;

import br.com.portalmanager.account.application.model.AccountResult;
import br.com.portalmanager.account.application.model.CreateAccountCommand;
import br.com.portalmanager.account.application.model.UpdateAccountCommand;
import br.com.portalmanager.account.application.tagging.AccountSystemTagProvider;
import br.com.portalmanager.account.application.tagging.AccountTagOwnerType;
import br.com.portalmanager.account.domain.Account;
import br.com.portalmanager.account.domain.AccountLifecycle;
import br.com.portalmanager.account.domain.AccountType;
import br.com.portalmanager.account.persistence.AccountRepository;
import br.com.portalmanager.core.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.core.messaging.exception.NotFoundException;
import br.com.portalmanager.core.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.core.messaging.exception.ValidationException;
import br.com.portalmanager.core.tagging.TagManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class AccountService {

    private final AccountRepository repository;
    private final AccountNormalizer normalizer;
    private final AccountValidator validator;
    private final TagManager tagManager;
    private final AccountSystemTagProvider systemTagProvider;

    public AccountService(
            AccountRepository repository,
            AccountNormalizer normalizer,
            AccountValidator validator,
            TagManager tagManager,
            AccountSystemTagProvider systemTagProvider
    ) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tagManager = tagManager;
        this.systemTagProvider = systemTagProvider;
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

        Account saved = repository.saveAndFlush(account);
        reconcileTags(saved, command.tags());
        return toResult(saved);
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public AccountResult findById(Long id) {
        return toResult(findActive(id));
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public List<AccountResult> findAll(Boolean active, String typeName, String tagName) {
        AccountLifecycle lifecycle = Boolean.FALSE.equals(active)
                ? AccountLifecycle.INACTIVE
                : AccountLifecycle.ACTIVE;

        String normalizedType = normalizer.normalizeTypeFilter(typeName);
        String normalizedTag = normalizer.normalizeTagFilter(tagName);
        validator.validateTypeFilter(normalizedType);

        AccountType accountType = normalizedType == null
                ? null
                : AccountType.valueOf(normalizedType);

        List<Account> accounts = repository.findFiltered(
                lifecycle,
                accountType,
                normalizedTag,
                AccountTagOwnerType.ACCOUNT.value()
        );

        Map<String, List<String>> manualTags = tagManager.findManualByOwners(
                AccountTagOwnerType.ACCOUNT,
                accounts.stream().map(Account::getIdentifier).toList()
        );

        return accounts.stream()
                .map(account -> AccountResult.from(
                        account,
                        manualTags.getOrDefault(account.getIdentifier(), List.of())
                ))
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

        Account saved = repository.saveAndFlush(account);
        reconcileTags(saved, command.tags());
        return toResult(saved);
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

        List<String> manualTags = tagManager.findManual(
                AccountTagOwnerType.ACCOUNT,
                account.getIdentifier()
        );

        account.restore(LocalDateTime.now());
        Account saved = repository.saveAndFlush(account);
        reconcileTags(saved, manualTags);
        return toResult(saved);
    }

    private AccountResult toResult(Account account) {
        return AccountResult.from(
                account,
                tagManager.findManual(AccountTagOwnerType.ACCOUNT, account.getIdentifier())
        );
    }

    private void reconcileTags(Account account, List<String> manualTags) {
        tagManager.reconcile(
                AccountTagOwnerType.ACCOUNT,
                account.getIdentifier(),
                manualTags,
                systemTagProvider.resolve(account)
        );
    }

    private Account findActive(Long id) {
        return repository.findByIdAndLifecycle(id, AccountLifecycle.ACTIVE)
                .orElseThrow(() -> new NotFoundException(AccountMessageKeys.NOT_FOUND));
    }
}
