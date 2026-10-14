package br.com.portalmanager.platform.workspace.core.publisher.repository;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Optional<Publisher> findByIdentifier(String identifier);
    boolean existsByCode(String code);

    @Query("select p from Publisher p where p.lifecycle.value = :lifecycle")
    Page<Publisher> findByLifecycle(@Param("lifecycle") String lifecycle, Pageable pageable);

    @Query("select p from Publisher p where p.lifecycle.value = :lifecycle and p.scope.value = :scope")
    Page<Publisher> findByLifecycleAndScope(
        @Param("lifecycle") String lifecycle,
        @Param("scope") String scope,
        Pageable pageable
    );
}
