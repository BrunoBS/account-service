package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.UpdateFeatureGroupInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup.FeatureGroupValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FeatureGroupCommandService {
    private final FeatureGroupRepository repository;
    private final FeatureGroupFinder finder;
    private final FeatureGroupValidator validator;

    public FeatureGroupCommandService(
            FeatureGroupRepository repository, FeatureGroupFinder finder, FeatureGroupValidator validator) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_GROUP_CREATED", resourceType = "GROUP")
    public FeatureGroupOutput create(CreateFeatureGroupInput input) {
        validator.validateForCreate(input);
        FeatureGroup featureGroupEntity =
                repository.saveAndFlush(
                        new FeatureGroup(
                                input.code(),
                                input.name(),
                                input.description(),
                                LocalDateTime.now()));
        return FeatureGroupOutput.from(featureGroupEntity);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_GROUP_UPDATED", resourceType = "GROUP")
    public FeatureGroupOutput update(String featureGroupIdentifier, UpdateFeatureGroupInput input) {
        FeatureGroup featureGroupEntity = finder.findForUpdate(featureGroupIdentifier);
        featureGroupEntity.requireActive();
        validator.validateForUpdate(featureGroupEntity, input);
        featureGroupEntity.update(input.name(), input.description(), LocalDateTime.now());
        return FeatureGroupOutput.from(repository.saveAndFlush(featureGroupEntity));
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_GROUP_ACTIVATED", resourceType = "GROUP")
    public FeatureGroupOutput activate(String featureGroupIdentifier) {
        FeatureGroup featureGroupEntity = finder.findForUpdate(featureGroupIdentifier);
        featureGroupEntity.activate(LocalDateTime.now());
        return FeatureGroupOutput.from(repository.saveAndFlush(featureGroupEntity));
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "FEATURE_GROUP_DEACTIVATED", resourceType = "GROUP")
    public FeatureGroupOutput inactivate(String featureGroupIdentifier) {
        FeatureGroup featureGroupEntity = finder.findForUpdate(featureGroupIdentifier);
        featureGroupEntity.inactivate(LocalDateTime.now());
        return FeatureGroupOutput.from(repository.saveAndFlush(featureGroupEntity));
    }

    @Transactional
    @Auditable(action = AuditAction.PURGE, event = "FEATURE_GROUP_PURGED", resourceType = "GROUP")
    public FeatureGroupOutput purge(String featureGroupIdentifier) {
        FeatureGroup featureGroupEntity = finder.findForUpdate(featureGroupIdentifier);
        validator.validateForPurge(featureGroupEntity);
        FeatureGroupOutput removedGroup = FeatureGroupOutput.from(featureGroupEntity);
        repository.delete(featureGroupEntity);
        repository.flush();
        return removedGroup;
    }
}
