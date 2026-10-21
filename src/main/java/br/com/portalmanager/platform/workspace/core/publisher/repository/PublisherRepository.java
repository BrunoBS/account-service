package br.com.portalmanager.platform.workspace.core.publisher.repository;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Optional<Publisher> findByIdentifier(String identifier);
    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select p from Publisher p where p.identifier in :identifiers order by p.id")
    List<Publisher> findForAssociation(@Param("identifiers") Collection<String> identifiers);

    @Query("select p from Publisher p where p.lifecycle.value = :lifecycle")
    Page<Publisher> findByLifecycle(@Param("lifecycle") String lifecycle, Pageable pageable);

    @Query("select p from Publisher p where p.lifecycle.value = :lifecycle and p.scope.value = :scope")
    Page<Publisher> findByLifecycleAndScope(
        @Param("lifecycle") String lifecycle,
        @Param("scope") String scope,
        Pageable pageable
    );
}
