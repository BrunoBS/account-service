package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MicroserviceRepository extends JpaRepository<Microservice, Long> {
    Optional<Microservice> findByIdentifier(String identifier);

    Optional<Microservice> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
