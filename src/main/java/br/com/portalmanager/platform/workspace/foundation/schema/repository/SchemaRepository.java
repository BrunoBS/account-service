package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SchemaRepository extends JpaRepository<Schema, Long> {

    Optional<Schema> findByIdentifier(String identifier);

    @Query("""
            select s from Schema s
             where s.identifier = :identifier
               and s.scope.value = :scopeCode
               and ((:workspaceId is null and s.workspaceId is null)
                    or s.workspaceId = :workspaceId)
            """)
    Optional<Schema> findByIdentifierAndScope(
            @Param("identifier") String identifier,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceId") Long workspaceId
    );

    @Query("""
            select s from Schema s
             where s.scope.value = :scopeCode
               and ((:workspaceId is null and s.workspaceId is null)
                    or s.workspaceId = :workspaceId)
             order by s.code
            """)
    List<Schema> findAllByScope(
            @Param("scopeCode") String scopeCode,
            @Param("workspaceId") Long workspaceId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Schema s where s.identifier = :identifier")
    Optional<Schema> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from Schema s
             where s.identifier = :identifier
               and s.scope.value = :scopeCode
               and ((:workspaceId is null and s.workspaceId is null)
                    or s.workspaceId = :workspaceId)
            """)
    Optional<Schema> findByIdentifierAndScopeForUpdate(
            @Param("identifier") String identifier,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceId") Long workspaceId
    );

    @Query("""
            select s from Schema s
             where s.schemaType.value = :schemaTypeCode
               and s.scope.value = :scopeCode
               and ((:workspaceId is null and s.workspaceId is null)
                    or s.workspaceId = :workspaceId)
            """)
    Optional<Schema> findByTypeAndScope(
            @Param("schemaTypeCode") String schemaTypeCode,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceId") Long workspaceId
    );

    @Query("""
            select s from Schema s
             where s.schemaType.value = :schemaTypeCode
               and s.scope.value = :scopeCode
               and ((:workspaceId is null and s.workspaceId is null)
                    or s.workspaceId = :workspaceId)
               and s.code = :code
            """)
    Optional<Schema> findByTypeScopeAndCode(
            @Param("schemaTypeCode") String schemaTypeCode,
            @Param("scopeCode") String scopeCode,
            @Param("workspaceId") Long workspaceId,
            @Param("code") String code
    );

    @Query("select count(s) > 0 from Schema s where s.schemaType.value = :schemaTypeCode")
    boolean existsBySchemaTypeCode(@Param("schemaTypeCode") String schemaTypeCode);

    @Query("""
            select count(s) > 0 from Schema s
             where s.schemaType.value = :schemaTypeCode
               and s.scope.value = :scopeCode
            """)
    boolean existsBySchemaTypeCodeAndScope(
            @Param("schemaTypeCode") String schemaTypeCode,
            @Param("scopeCode") String scopeCode
    );
}

