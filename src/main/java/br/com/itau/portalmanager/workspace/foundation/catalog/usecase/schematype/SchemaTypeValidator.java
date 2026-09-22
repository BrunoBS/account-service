package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schematype;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schematype.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.schemascope.SchemaScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.schematype.SchemaTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.validation.BaseRelatedCatalogValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import org.springframework.stereotype.Component;

@Component
public class SchemaTypeValidator extends BaseRelatedCatalogValidator<SchemaTypeDTO, String> {

    private final SchemaTypeRepository repository;
    private final SchemaScopeTypeRepository scopeRepository;
    private final SchemaValidator settingsValidator;

    public SchemaTypeValidator(
            SchemaTypeRepository repository,
            SchemaScopeTypeRepository scopeRepository,
            SchemaValidator settingsValidator) {
        super(repository);
        this.repository = repository;
        this.scopeRepository = scopeRepository;
        this.settingsValidator = settingsValidator;
    }

    @Override protected void validateSettings(SchemaTypeDTO dto, CatalogValidationResult result) {
        settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result);
    }
    @Override protected String relatedValue(SchemaTypeDTO dto) { return dto.scope(); }
    @Override protected String relatedField() { return "scope"; }
    @Override protected String relatedEntityName() { return SchemaScopeType.class.getSimpleName(); }
    @Override protected boolean relatedExistsAndIsActive(String scope) {
        return scopeRepository.findByNameAndActiveTrue(scope).isPresent();
    }
    @Override protected boolean existsByNameAndRelatedValue(String name, String scope, long id) {
        return repository.existsByNameAndSchemaScopeNameAndIdNot(name, scope, id);
    }
    @Override public String entityName() { return SchemaType.class.getSimpleName(); }
}
