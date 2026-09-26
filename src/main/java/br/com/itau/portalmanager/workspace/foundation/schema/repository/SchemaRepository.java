package br.com.itau.portalmanager.workspace.foundation.schema.repository;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchemaRepository extends JpaRepository<Schema, Long> {

    Optional<Schema> findByIdentifier(String identifier);

    @Query("""
            select s from Schema s
             where s.identifier = :identifier
               and s.scope.value = :scopeCode
               and ((:workspaceIdentifier is null and s.workspaceIdentifier is null)
                    or s.workspaceIdentifier = :workspaceIdentifier)
            """)
    Optional<Schema> findByIdentifierAndScope(
            @Param("identifier") String identifier,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceIdentifier") String workspaceIdentifier
    );

    @Query("""
            select s from Schema s
             where s.scope.value = :scopeCode
               and ((:workspaceIdentifier is null and s.workspaceIdentifier is null)
                    or s.workspaceIdentifier = :workspaceIdentifier)
             order by s.code
            """)
    List<Schema> findAllByScope(
            @Param("scopeCode") String scopeCode,
            @Param("workspaceIdentifier") String workspaceIdentifier
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Schema s where s.identifier = :identifier")
    Optional<Schema> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Schema s
             where s.identifier = :identifier
               and s.scope.value = :scopeCode
               and ((:workspaceIdentifier is null and s.workspaceIdentifier is null)
                    or s.workspaceIdentifier = :workspaceIdentifier)
            """)
    Optional<Schema> findByIdentifierAndScopeForUpdate(
            @Param("identifier") String identifier,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceIdentifier") String workspaceIdentifier
    );

    
    @Query("""
            select s from Schema s
             where s.schemaTypeCode = :schemaTypeCode
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
             where s.schemaTypeCode = :schemaTypeCode
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
