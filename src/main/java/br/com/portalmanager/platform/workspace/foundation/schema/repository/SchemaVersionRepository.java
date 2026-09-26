package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaVersion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchemaVersionRepository extends JpaRepository<SchemaVersion, Long> {

    Optional<SchemaVersion> findByIdentifier(String identifier);

    Optional<SchemaVersion> findByIdentifierAndSchema_Id(String identifier, Long schemaId);

    Optional<SchemaVersion> findFirstBySchema_IdAndStatusOrderBySchemaVersionDesc(
            Long schemaId,
            SchemaVersionStatusTypeCode status
    );

    List<SchemaVersion> findBySchema_IdOrderBySchemaVersionDesc(Long schemaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from SchemaVersion v where v.schema.id = :schemaId order by v.schemaVersion desc")
    List<SchemaVersion> findAllForUpdate(@Param("schemaId") Long schemaId);
}
