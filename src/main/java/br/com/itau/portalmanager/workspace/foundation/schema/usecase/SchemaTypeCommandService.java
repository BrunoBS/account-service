package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchemaTypeCommandService {

    private final SchemaTypeRepository repository;

    public SchemaTypeCommandService(SchemaTypeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public SchemaTypeOutput create(CreateSchemaTypeInput input) {
        if (repository.existsByCode(input.code())) {
            throw new IllegalArgumentException("Schema type code already exists");
        }
        if (repository.existsByName(input.name())) {
            throw new IllegalArgumentException("Schema type name already exists");
        }
        SchemaType type = repository.save(
                new SchemaType(input.code(), input.name(), input.description(), LocalDateTime.now())
        );
        return SchemaTypeOutput.from(type);
    }

    @Transactional
    public SchemaTypeOutput activate(String identifier) {
        SchemaType type = required(identifier);
        type.activate(LocalDateTime.now());
        return SchemaTypeOutput.from(type);
    }

    @Transactional
    public SchemaTypeOutput inactivate(String identifier) {
        SchemaType type = required(identifier);
        type.inactivate(LocalDateTime.now());
        return SchemaTypeOutput.from(type);
    }

    private SchemaType required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Schema type not found"));
    }
}
