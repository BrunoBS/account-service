package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
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
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.SCHEMA_TYPE_NOT_FOUND));
    }
}
