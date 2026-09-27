package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SchemaTypeRepository extends JpaRepository<SchemaType, Long> {
    Optional<SchemaType> findByIdentifier(String identifier);
    Optional<SchemaType> findByCode(String code);
    boolean existsByCode(String code);
}
