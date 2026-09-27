package br.com.portalmanager.platform.workspace.core.publisher.repository;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublisherRepository extends JpaRepository<Publisher, Long> {
    Optional<Publisher> findByIdentifier(String identifier);
    boolean existsByCode(String code);
    @Query("select p from Publisher p where p.lifecycle.value = :lifecycle order by p.code")
    List<Publisher> findByLifecycle(@Param("lifecycle") String lifecycle);
    @Query("select p from Publisher p where p.lifecycle.value = :lifecycle and p.scope.value = :scope order by p.code")
    List<Publisher> findByLifecycleAndScope(@Param("lifecycle") String lifecycle, @Param("scope") String scope);
}
