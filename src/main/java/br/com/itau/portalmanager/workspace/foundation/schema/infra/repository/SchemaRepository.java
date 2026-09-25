package br.com.itau.portalmanager.workspace.foundation.schema.infra.repository;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchemaRepository extends JpaRepository<Schema, Long> {
    Optional<Schema> findByIdentifier(String identifier);
    Optional<Schema> findBySchemaType_CodeAndScope_ValueAndWorkspaceIdentifier(
            String schemaTypeCode,
            String scopeCode,
            String workspaceIdentifier
    );
    Optional<Schema> findBySchemaType_CodeAndScope_ValueAndWorkspaceIdentifierAndCode(
            String schemaTypeCode,
            String scopeCode,
            String workspaceIdentifier,
            String code
    );
}
