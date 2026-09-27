package br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype;

import br.com.portalmanager.platform.library.messaging.exception.ConflictException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaMessageKeys;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SchemaTypeCommandService {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,49}$");

    private final SchemaTypeRepository repository;
    private final SchemaRepository schemaRepository;

    public SchemaTypeCommandService(SchemaTypeRepository repository, SchemaRepository schemaRepository) {
        this.repository = repository;
        this.schemaRepository = schemaRepository;
    }

    @Transactional
    public SchemaTypeOutput create(CreateSchemaTypeInput input) {
        validateCreate(input);
        String code = input.code().trim().toUpperCase();
        if (repository.existsByCode(code)) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DUPLICATE);
        }

        SchemaType schemaType = repository.save(new SchemaType(
                code,
                input.name(),
                input.description(),
                scopes(input.allowedScopes()),
                LocalDateTime.now()
        ));
        return SchemaTypeOutput.from(schemaType);
    }

    @Transactional
    public SchemaTypeOutput update(String identifier, UpdateSchemaTypeInput input) {
        SchemaType schemaType = required(identifier);
        if (input == null || input.version() == null) {
            throw invalid("version", SchemaMessageKeys.VERSION_INVALID);
        }
        if (!Objects.equals(input.version(), schemaType.getVersion())) {
            throw new ResourceVersionConflictException();
        }
        requireName(input.name());
        Set<SchemaScopeTypeCode> requestedScopes = scopes(input.allowedScopes());
        validateRemovedScopesNotInUse(schemaType, requestedScopes);
        schemaType.update(input.name(), input.description(), requestedScopes, LocalDateTime.now());
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
        schemaType.inactivate(LocalDateTime.now());
        return SchemaTypeOutput.from(schemaType);
    }

    @Transactional
    public void delete(String identifier) {
        SchemaType schemaType = required(identifier);
        if (schemaType.isActive()) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_DELETE_ACTIVE);
        }
        if (schemaRepository.existsBySchemaTypeCode(schemaType.getCode())) {
            throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_IN_USE);
        }
        repository.delete(schemaType);
    }

    private SchemaType required(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(SchemaMessageKeys.SCHEMA_TYPE_NOT_FOUND));
    }

    private void validateCreate(CreateSchemaTypeInput input) {
        if (input == null) throw invalid("schemaType", SchemaMessageKeys.REQUEST_INVALID);
        if (input.code() == null || !CODE.matcher(input.code().trim().toUpperCase()).matches()) {
            throw invalid("code", SchemaMessageKeys.TYPE_INVALID);
        }
        requireName(input.name());
        scopes(input.allowedScopes());
    }

    private void validateRemovedScopesNotInUse(
            SchemaType schemaType,
            Set<SchemaScopeTypeCode> requestedScopes
    ) {
        for (SchemaScopeTypeCode currentScope : schemaType.getAllowedScopes()) {
            if (requestedScopes.contains(currentScope)) {
                continue;
            }
            if (schemaRepository.existsBySchemaTypeCodeAndScope(
                    schemaType.getCode(),
                    currentScope.value()
            )) {
                throw new ConflictException(SchemaMessageKeys.SCHEMA_TYPE_SCOPE_IN_USE);
            }
        }
    }

    private void requireName(String name) {
        if (name == null || name.isBlank()) {
            throw invalid("name", SchemaMessageKeys.NAME_REQUIRED);
        }
    }

    private Set<SchemaScopeTypeCode> scopes(Set<String> values) {
        if (values == null || values.isEmpty()) {
            throw invalid("allowedScopes", SchemaMessageKeys.SCHEMA_TYPE_SCOPES_REQUIRED);
        }
        Set<SchemaScopeTypeCode> result = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                throw invalid("allowedScopes", SchemaMessageKeys.SCOPE_INVALID);
            }
            String normalized = value.trim().toUpperCase();
            if ("PLATFORM".equals(normalized)) {
                result.add(SchemaScopeTypeCode.platform());
            } else if ("WORKSPACE".equals(normalized)) {
                result.add(SchemaScopeTypeCode.workspace());
            } else {
                throw invalid("allowedScopes", SchemaMessageKeys.SCOPE_INVALID);
            }
        }
        return result;
    }

    private ValidationException invalid(String field, String key) {
        return new ValidationException(new ValidationResult(field, key));
    }
}
