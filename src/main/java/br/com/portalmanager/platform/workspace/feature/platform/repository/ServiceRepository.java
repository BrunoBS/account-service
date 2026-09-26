package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceRepository extends JpaRepository<Service, Long> {
    Optional<Service> findByIdentifier(String identifier);
    Optional<Service> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
