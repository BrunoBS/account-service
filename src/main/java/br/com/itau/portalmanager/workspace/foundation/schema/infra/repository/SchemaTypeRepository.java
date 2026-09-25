package br.com.itau.portalmanager.workspace.foundation.schema.infra.repository;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchemaTypeRepository extends JpaRepository<SchemaType, Long> {
    Optional<SchemaType> findByIdentifier(String identifier);
    Optional<SchemaType> findByCode(String code);
}
