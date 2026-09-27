package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
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

    @Transactional(readOnly = true)
    public SchemaType requireActiveAllowed(String code, SchemaScopeTypeCode scope) {
        SchemaType schemaType = requireActive(code);
        if (!schemaType.allowsScope(scope)) {
            throw new ValidationException(new ValidationResult("scope", SchemaMessageKeys.SCHEMA_TYPE_SCOPE_NOT_ALLOWED));
        }
        return schemaType;
    }

    @Transactional(readOnly = true)
    public SchemaType requireActive(String code) {
        SchemaType schemaType = repository.findByCode(code == null ? null : code.trim().toUpperCase())
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.SCHEMA_TYPE_NOT_FOUND));
        if (!schemaType.isActive()) {
            throw new ValidationException(new ValidationResult("schemaTypeCode", SchemaMessageKeys.TYPE_INACTIVE));
        }
        return schemaType;
    }
}

