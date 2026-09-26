package br.com.itau.portalmanager.workspace.foundation.schema.repository;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SchemaRepository extends JpaRepository<Schema, Long> {

    Optional<Schema> findByIdentifier(String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Schema s where s.identifier = :identifier")
    Optional<Schema> findByIdentifierForUpdate(@Param("identifier") String identifier);

    boolean existsBySchemaType_CodeAndScope_ValueAndWorkspaceIdentifierAndCode(
            String schemaTypeCode,
            String scopeCode,
            String workspaceIdentifier,
            String code
    );

    @Query("""
            select s from Schema s
             where s.schemaType.code = :schemaTypeCode
               and s.scope.value = :scopeCode
               and ((:workspaceIdentifier is null and s.workspaceIdentifier is null)
                    or s.workspaceIdentifier = :workspaceIdentifier)
            """)
    Optional<Schema> findByTypeAndScope(
            @Param("schemaTypeCode") String schemaTypeCode,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceIdentifier") String workspaceIdentifier
    );

    @Query("""
            select s from Schema s
             where s.schemaType.code = :schemaTypeCode
               and s.scope.value = :scopeCode
               and ((:workspaceIdentifier is null and s.workspaceIdentifier is null)
                    or s.workspaceIdentifier = :workspaceIdentifier)
               and s.code = :code
            """)
    Optional<Schema> findByTypeScopeAndCode(
            @Param("schemaTypeCode") String schemaTypeCode,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceIdentifier") String workspaceIdentifier,
            @Param("code") String code
    );
}
