package br.com.portalmanager.account.persistence;

import br.com.portalmanager.account.domain.Account;
import br.com.portalmanager.account.domain.AccountLifecycle;
import br.com.portalmanager.account.domain.AccountType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = "approvers")
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findDetailedById(@Param("id") Long id);

    @EntityGraph(attributePaths = "approvers")
    Optional<Account> findByIdAndLifecycle(Long id, AccountLifecycle lifecycle);

    @EntityGraph(attributePaths = "approvers")
    List<Account> findByLifecycleOrderByIdAsc(AccountLifecycle lifecycle);

    @EntityGraph(attributePaths = "approvers")
    List<Account> findByLifecycleAndAccountTypeOrderByIdAsc(
            AccountLifecycle lifecycle,
            AccountType accountType
    );

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
