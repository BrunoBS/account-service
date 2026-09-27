package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.validation.SchemaTypeValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class SchemaTypeCommandService {

    private final SchemaTypeRepository repository;
    private final SchemaRepository schemaRepository;
    private final SchemaTypeValidator validator;

    public SchemaTypeCommandService(
            SchemaTypeRepository repository,
            SchemaRepository schemaRepository,
            SchemaTypeValidator validator
    ) {
        this.repository = repository;
        this.schemaRepository = schemaRepository;
        this.validator = validator;
    }

    @Transactional
    public SchemaTypeOutput create(CreateSchemaTypeInput input) {
        String code = validator.canonicalCode(input == null ? null : input.code());
        SchemaScopeTypeCode requestedScope = validator.validateForCreate(
                input,
                code != null && repository.existsByCode(code)
        );
        SchemaType schemaType = repository.save(new SchemaType(
                code,
                input.name(),
                input.description(),
                requestedScope,
                LocalDateTime.now()
        ));
        return SchemaTypeOutput.from(schemaType);
    }

    @Transactional
    public SchemaTypeOutput update(String identifier, UpdateSchemaTypeInput input) {
        SchemaType schemaType = required(identifier);
        SchemaScopeTypeCode requestedScope = validator.validateForUpdate(schemaType, input);
        validateChangedScopeNotInUse(schemaType, requestedScope);
        schemaType.update(input.name(), input.description(), requestedScope, LocalDateTime.now());
        return SchemaTypeOutput.from(schemaType);
    }

    @Transactional
    public SchemaTypeOutput activate(String identifier) {
        SchemaType schemaType = required(identifier);
        schemaType.activate(LocalDateTime.now());
        return SchemaTypeOutput.from(schemaType);
    }

    @Transactional
    public SchemaTypeOutput inactivate(String identifier) {
        SchemaType schemaType = required(identifier);
        validator.validateInactivation(schemaType);
        schemaType.inactivate(LocalDateTime.now());
        return SchemaTypeOutput.from(schemaType);
    }

    @Transactional
    public void delete(String identifier) {
        SchemaType schemaType = required(identifier);
        validator.validateDeletion(
                schemaType,
                schemaRepository.existsBySchemaTypeCode(schemaType.getCode())
        );
        repository.delete(schemaType);
    }

    private SchemaType required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.SCHEMA_TYPE_NOT_FOUND));
    }

    private void validateChangedScopeNotInUse(
            SchemaType schemaType,
            SchemaScopeTypeCode requestedScope
    ) {
        SchemaScopeTypeCode currentScope = schemaType.getScope();
        if (Objects.equals(currentScope, requestedScope) || currentScope == null) {
            return;
        }
        validator.validateRemovedScope(
                schemaRepository.existsBySchemaTypeCodeAndScope(
                        schemaType.getCode(),
                        currentScope.value()
                )
        );
    }
}
