package br.com.portalmanager.account.persistence;

import br.com.portalmanager.account.domain.Account;
import br.com.portalmanager.account.domain.AccountLifecycle;
import br.com.portalmanager.account.domain.AccountType;
import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@PlatformIntegrationTest
@WithMySql
class AccountPersistenceIT {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void shouldPersistAccountWithApproversAndDefaultLifecycle() {
        var account = newAccount("Portal Manager", "Primeira descrição válida");
        account.addApprover("123456", "approver@portalmanager.com");

        var saved = accountRepository.saveAndFlush(account);
        var reloaded = accountRepository.findDetailedById(saved.getId()).orElseThrow();

        assertThat(reloaded.getIdentifier()).hasSize(36);
        assertThat(reloaded.getVersion()).isNotNull();
        assertThat(reloaded.getAccountType()).isEqualTo(AccountType.ADMIN);
        assertThat(reloaded.getLifecycle()).isEqualTo(AccountLifecycle.ACTIVE);
        assertThat(reloaded.isOnboarding()).isFalse();
        assertThat(reloaded.getApprovers()).singleElement().satisfies(approver -> {
            assertThat(approver.getFunctional()).isEqualTo("123456");
            assertThat(approver.getEmail()).isEqualTo("approver@portalmanager.com");
        });
    }

    @Test
    void shouldEnforceUniqueAccountNameRegardlessOfLifecycle() {
        accountRepository.saveAndFlush(newAccount("Unique Account", "Descrição válida número um"));

        var duplicate = newAccount("Unique Account", "Descrição válida número dois");

        assertThrows(DataIntegrityViolationException.class, () -> accountRepository.saveAndFlush(duplicate));
    }

    @Test
    void shouldRejectStaleUpdateUsingJpaVersion() {
        var saved = accountRepository.saveAndFlush(newAccount("Optimistic Account", "Descrição inicial válida"));
        var initialVersion = saved.getVersion();

        var firstEntityManager = entityManagerFactory.createEntityManager();
        var staleEntityManager = entityManagerFactory.createEntityManager();

        try {
            firstEntityManager.getTransaction().begin();
            staleEntityManager.getTransaction().begin();

            var first = firstEntityManager.find(Account.class, saved.getId());
            var stale = staleEntityManager.find(Account.class, saved.getId());

            first.updateDescription("Descrição alterada pela primeira transação", LocalDateTime.of(2026, 9, 20, 11, 0));
            firstEntityManager.getTransaction().commit();

            stale.updateDescription("Descrição da transação obsoleta", LocalDateTime.of(2026, 9, 20, 12, 0));

            assertThrows(RollbackException.class, staleEntityManager.getTransaction()::commit);
        } finally {
            firstEntityManager.close();
            staleEntityManager.close();
        }

        var reloaded = accountRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getDescription()).isEqualTo("Descrição alterada pela primeira transação");
        assertThat(reloaded.getVersion()).isGreaterThan(initialVersion);
    }

    private Account newAccount(String name, String description) {
        return new Account(
                AccountType.ADMIN,
                name,
                description,
                "requester",
                "PM",
                "{\"theme\":\"default\"}",
                null,
                "account@portalmanager.com",
                LocalDateTime.of(2026, 9, 20, 10, 0)
        );
    }
}
