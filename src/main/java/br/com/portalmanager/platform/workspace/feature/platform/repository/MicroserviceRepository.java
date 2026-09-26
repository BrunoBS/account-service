package br.com.portalmanager.platform.workspace.feature.platform.repository;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MicroserviceRepository extends JpaRepository<Microservice, Long> {
    Optional<Microservice> findByIdentifier(String identifier);
    Optional<Microservice> findByCode(String code);
    boolean existsByCode(String code);
    boolean existsByName(String name);
}
