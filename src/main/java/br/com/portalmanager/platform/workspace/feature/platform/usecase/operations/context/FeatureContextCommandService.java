package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelation;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureContextOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.FeatureContextValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class FeatureContextCommandService {

    private final FeatureContextRepository repository;
    private final FeatureContextValidator validator;
    private final FeatureContextRelationRepository relations;
    private final FeatureRepository features;

    public FeatureContextCommandService(
        FeatureContextRepository repository,
        FeatureContextRelationRepository relations
    ) {
        this(repository, relations, new FeatureContextValidator(), null);
    }

    @Autowired
    public FeatureContextCommandService(
        FeatureContextRepository repository,
        FeatureContextRelationRepository relations,
        FeatureContextValidator validator,
        FeatureRepository features
    ) {
        this.repository = repository;
        this.relations = relations;
        this.validator = validator;
        this.features = features;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_CONTEXT_CREATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput create(CreateFeatureContextInput input) {
        validator.validateCreate(
            input,
            input != null && repository.existsByCode(input.code()),
            input != null && repository.existsByName(input.name())
        );
        FeatureContext context = repository.save(
            new FeatureContext(input.code(), input.name(), input.description(), now())
        );
        synchronizeFeatures(context, input.featureIdentifiers());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_UPDATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput update(String identifier, UpdateFeatureContextInput input) {
        FeatureContext context = required(identifier);
        validator.validateUpdate(
            input,
            input != null && !context.getName().equals(input.name()) && repository.existsByName(input.name())
        );
        synchronizeFeatures(context, input.featureIdentifiers());
        context.update(input.name(), input.description(), now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_CONTEXT_ACTIVATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput activate(String identifier) {
        FeatureContext context = required(identifier);
        context.activate(now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "FEATURE_CONTEXT_DEACTIVATED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput inactivate(String identifier) {
        FeatureContext context = required(identifier);
        context.inactivate(now());
        return FeatureContextOutput.from(context);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "FEATURE_CONTEXT_DELETED", resourceType = "FEATURE_CONTEXT")
    public FeatureContextOutput delete(String identifier) {
        FeatureContext context = required(identifier);
        validator.validateDelete(relations.existsByContextId(context.getId()));
        context.quarantine(now());
        return FeatureContextOutput.from(context);
    }

    private void synchronizeFeatures(FeatureContext context, List<String> featureIdentifiers) {
        if (featureIdentifiers == null) return;
        List<Feature> selectedFeatures = new ArrayList<>();
        validator.validateFeatureIdentifiers(featureIdentifiers);
        for (String identifier : featureIdentifiers.stream().distinct().sorted().toList()) {
            selectedFeatures.add(
                features
                    .findByIdentifierForUpdate(identifier)
                    .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND))
            );
        }
        List<FeatureContextRelation> currentRelations = relations.findByContextId(context.getId());
        HashSet<Long> selectedFeatureIds = new HashSet<Long>();
        HashSet<Long> currentFeatureIds = new HashSet<Long>();
        selectedFeatures.forEach(feature -> selectedFeatureIds.add(feature.getId()));
        currentRelations.forEach(relation -> currentFeatureIds.add(relation.getFeature().getId()));
        for (Feature feature : selectedFeatures) {
            if (!currentFeatureIds.contains(feature.getId())) {
                validator.validateAssociation(context);
                relations.save(new FeatureContextRelation(feature, context));
                feature.markUpdated(now());
            }
        }
        for (FeatureContextRelation relation : currentRelations) {
            if (!selectedFeatureIds.contains(relation.getFeature().getId())) {
                relations.delete(relation);
            }
        }
    }

    private FeatureContext required(String identifier) {
        return repository
            .findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
