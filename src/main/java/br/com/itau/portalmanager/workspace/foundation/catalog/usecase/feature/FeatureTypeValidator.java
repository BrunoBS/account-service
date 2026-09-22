package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.feature;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.feature.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.featurescope.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.feature.FeatureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.featurescope.FeatureScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.validation.BaseRelatedCatalogValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import org.springframework.stereotype.Component;

@Component
public class FeatureTypeValidator extends BaseRelatedCatalogValidator<FeatureTypeDTO, Long> {

    private final FeatureTypeRepository repository;
    private final FeatureScopeTypeRepository scopeRepository;
    private final SchemaValidator settingsValidator;

    public FeatureTypeValidator(
            FeatureTypeRepository repository,
            FeatureScopeTypeRepository scopeRepository,
            SchemaValidator settingsValidator) {
        super(repository);
        this.repository = repository;
        this.scopeRepository = scopeRepository;
        this.settingsValidator = settingsValidator;
    }

    @Override
    protected void validateSettings(FeatureTypeDTO dto, CatalogValidationResult result) {
        settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result);
    }

    @Override protected Long relatedValue(FeatureTypeDTO dto) { return dto.featureScopeId(); }
    @Override protected String relatedField() { return "featureScopeId"; }
    @Override protected String relatedEntityName() { return FeatureScopeType.class.getSimpleName(); }
    @Override protected boolean relatedExistsAndIsActive(Long id) {
        return scopeRepository.findByIdAndActiveTrue(id).isPresent();
    }
    @Override protected boolean existsByNameAndRelatedValue(String name, Long id, long currentId) {
        return repository.existsByNameAndFeatureScopeIdAndIdNot(name, id, currentId);
    }
    @Override public String entityName() { return FeatureType.class.getSimpleName(); }
}
