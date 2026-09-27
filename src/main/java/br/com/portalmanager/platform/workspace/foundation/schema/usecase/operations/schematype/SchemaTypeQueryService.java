package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaTypeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SchemaTypeQueryService {

    private final SchemaTypeRepository repository;
    private final SchemaTypeValidator validator;

    public SchemaTypeQueryService(
            SchemaTypeRepository repository,
            SchemaTypeValidator validator
    ) {
        this.repository = repository;
        this.validator = validator;
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

    @Transactional(readOnly = true)
    public Optional<SchemaType> findActiveAllowed(String code, SchemaScopeTypeCode scope) {
        return repository.findByCode(code == null ? null : code.trim().toUpperCase())
                .map(schemaType -> {
                    validator.validateActive(schemaType);
                    validator.validateAllowedScope(schemaType, scope);
                    return schemaType;
                });
    }

    @Transactional(readOnly = true)
    public SchemaType requireActiveAllowed(String code, SchemaScopeTypeCode scope) {
        SchemaType schemaType = requireActive(code);
        validator.validateAllowedScope(schemaType, scope);
        return schemaType;
    }

    @Transactional(readOnly = true)
    public SchemaType requireActive(String code) {
        SchemaType schemaType = repository.findByCode(code == null ? null : code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.SCHEMA_TYPE_NOT_FOUND));
        validator.validateActive(schemaType);
        return schemaType;
    }
}
