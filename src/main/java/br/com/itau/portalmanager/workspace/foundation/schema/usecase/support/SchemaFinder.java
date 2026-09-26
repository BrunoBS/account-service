package br.com.itau.portalmanager.workspace.foundation.schema.usecase.support;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaRepository;
import org.springframework.stereotype.Component;

@Component
public class SchemaFinder {

    private final SchemaRepository repository;

    public SchemaFinder(SchemaRepository repository) {
        this.repository = repository;
    }

    public Schema find(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found"));
    }

    public Schema findScoped(
            String identifier,
            String scope,
            String workspaceIdentifier
    ) {
        return repository.findByIdentifierAndScope(identifier, scope, workspaceIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found in requested scope"));
    }

    public Schema findScopedForUpdate(
            String identifier,
            String scope,
            String workspaceIdentifier
    ) {
        return repository.findByIdentifierAndScopeForUpdate(identifier, scope, workspaceIdentifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema not found in requested scope"));
    }
}
