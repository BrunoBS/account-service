package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup;

import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRelationRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.UpdateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.ServiceContextValidation;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

@Component
public class FeatureGroupValidator {
    private static final Set<String> SORTABLE_PROPERTIES =
            Set.of("id", "identifier", "code", "name", "createdAt", "updatedAt");
    private final FeatureGroupRepository repository;
    private final FeatureGroupRelationRepository relations;

    public FeatureGroupValidator(FeatureGroupRepository repository, FeatureGroupRelationRepository relations) {
        this.repository = repository;
        this.relations = relations;
    }

    public void validateForCreate(CreateFeatureGroupInput input) {
        ServiceContextValidation.requireInput(input);
        if (input.version() != null)
            ServiceContextValidation.reject(
                    "version", ServiceContextMessageKeys.VERSION_NOT_ALLOWED_ON_CREATE);
        ValidationResult result = new ValidationResult();
        if (repository.existsByCode(input.code()))
            result.addError("code", ServiceContextMessageKeys.CODE_DUPLICATE);
        if (repository.existsByName(input.name()))
            result.addError("name", ServiceContextMessageKeys.NAME_DUPLICATE);
        reject(result);
    }

    public void validateForUpdate(FeatureGroup featureGroupEntity, UpdateFeatureGroupInput input) {
        ServiceContextValidation.requireInput(input);
        if (input.version() == null)
            ServiceContextValidation.reject("version", ServiceContextMessageKeys.VERSION_REQUIRED);
        if (!Objects.equals(featureGroupEntity.getVersion(), input.version()))
            throw new ResourceVersionConflictException();
        ValidationResult result = new ValidationResult();
        if (!featureGroupEntity.getCode().equals(input.code()))
            result.addError("code", ServiceContextMessageKeys.CODE_IMMUTABLE);
        if (repository.existsByNameAndIdNot(input.name(), featureGroupEntity.getId()))
            result.addError("name", ServiceContextMessageKeys.NAME_DUPLICATE);
        reject(result);
    }

    public void validateForPurge(FeatureGroup featureGroupEntity) {
        if (relations.findFirstByFeatureGroupId(featureGroupEntity.getId()).isPresent()) {
            ServiceContextValidation.reject("featureGroup", ServiceContextMessageKeys.FEATURE_GROUP_HAS_FEATURES);
        }
    }

    public void validateQuery(Pageable pageable) {
        if (pageable.isUnpaged()
                || pageable.getOffset() > Integer.MAX_VALUE
                || pageable.getSort().stream()
                        .anyMatch(order -> !SORTABLE_PROPERTIES.contains(order.getProperty()))) {
            throw new ValidationException(ServiceContextMessageKeys.FEATURE_GROUP_PAGE_INVALID);
        }
    }

    private void reject(ValidationResult result) {
        if (result.hasErrors()) throw new ValidationException(result);
    }
}
