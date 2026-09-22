package br.com.itau.portalmanager.workspace.foundation.catalog.schematype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.domain.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.repository.SchemaScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.repository.SchemaTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.validation.BaseRelatedCatalogValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import org.springframework.stereotype.Component;

@Component
public class SchemaTypeValidator extends BaseRelatedCatalogValidator<SchemaTypeDTO, String> {

    private final SchemaTypeRepository repository;
    private final SchemaScopeTypeRepository scopeRepository;
    private final CatalogSchemaValidationSupport settingsValidator;

    public SchemaTypeValidator(
            SchemaTypeRepository repository,
            SchemaScopeTypeRepository scopeRepository,
            CatalogSchemaValidationSupport settingsValidator) {
        super(repository);
        this.repository = repository;
        this.scopeRepository = scopeRepository;
        this.settingsValidator = settingsValidator;
    }

    @Override protected void validateSettings(SchemaTypeDTO dto, CatalogValidationResult result) {
        settingsValidator.validateSettings(dto.settings(), result);
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
