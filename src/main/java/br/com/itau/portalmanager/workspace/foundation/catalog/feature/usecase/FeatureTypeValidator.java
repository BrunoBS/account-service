package br.com.itau.portalmanager.workspace.foundation.catalog.feature.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.feature.domain.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescope.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.feature.repository.FeatureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescope.repository.FeatureScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.validation.BaseRelatedCatalogValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import org.springframework.stereotype.Component;

@Component
public class FeatureTypeValidator extends BaseRelatedCatalogValidator<FeatureTypeDTO, Long> {

    private final FeatureTypeRepository repository;
    private final FeatureScopeTypeRepository scopeRepository;
    private final CatalogSchemaValidationSupport settingsValidator;

    public FeatureTypeValidator(
            FeatureTypeRepository repository,
            FeatureScopeTypeRepository scopeRepository,
            CatalogSchemaValidationSupport settingsValidator) {
        super(repository);
        this.repository = repository;
        this.scopeRepository = scopeRepository;
        this.settingsValidator = settingsValidator;
    }

    @Override
    protected void validateSettings(FeatureTypeDTO dto, CatalogValidationResult result) {
        settingsValidator.validateSettings(dto.settings(), result);
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
