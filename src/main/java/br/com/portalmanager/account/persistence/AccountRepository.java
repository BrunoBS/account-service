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
    @Query("""
            select distinct a
              from Account a
             where a.lifecycle = :lifecycle
               and (:accountType is null or a.accountType = :accountType)
               and (:tagName is null or exists (
                    select t.id
                      from Tag t
                     where t.ownerType = :tagOwnerType
                       and t.ownerId = a.identifier
                       and lower(t.name) like lower(concat('%', :tagName, '%'))
               ))
             order by a.id
            """)
    List<Account> findFiltered(
            @Param("lifecycle") AccountLifecycle lifecycle,
            @Param("accountType") AccountType accountType,
            @Param("tagName") String tagName,
            @Param("tagOwnerType") String tagOwnerType
    );

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);
}
