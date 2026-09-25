package br.com.itau.portalmanager.workspace.foundation.schema.infra.repository;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersion;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SchemaVersionRepository extends JpaRepository<SchemaVersion, Long> {

    Optional<SchemaVersion> findByIdentifier(String identifier);

    Optional<SchemaVersion> findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
            Long schemaId,
            SchemaVersionStatus status
    );

    List<SchemaVersion> findBySchema_IdOrderBySchemaVersionDesc(Long schemaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from SchemaVersion v where v.schema.id = :schemaId order by v.schemaVersion desc")
    List<SchemaVersion> findAllForUpdate(@Param("schemaId") Long schemaId);
}
