package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SchemaTypeQueryService {

    private final SchemaTypeRepository repository;

    public SchemaTypeQueryService(SchemaTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SchemaTypeOutput> findAll() {
        return repository.findAll().stream().map(SchemaTypeOutput::from).toList();
    }

    @Transactional(readOnly = true)
    public SchemaTypeOutput findByIdentifier(String identifier) {
        return repository.findByIdentifier(identifier)
                .map(SchemaTypeOutput::from)
                .orElseThrow(() -> new IllegalArgumentException("Schema type not found"));
    }
}
