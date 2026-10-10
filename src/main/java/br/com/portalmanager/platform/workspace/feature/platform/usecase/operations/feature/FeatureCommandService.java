package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelation;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelationId;
import br.com.portalmanager.platform.workspace.feature.platform.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureContextRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.MicroserviceRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.FeatureValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.FeatureSharingReferenceGuard;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@org.springframework.stereotype.Service
public class FeatureCommandService {

    private final FeatureRepository features;
    private final MicroserviceRepository microservices;
    private final FeatureContextRepository contexts;
    private final FeatureValidator validator;
    private final FeatureContextRelationRepository relations;
    private final FeatureSharingReferenceGuard sharingReferences;

    public FeatureCommandService(
        FeatureRepository features,
        MicroserviceRepository microservices,
        FeatureContextRepository contexts,
        FeatureContextRelationRepository relations
    ) {
        this(features, microservices, contexts, relations, new FeatureValidator(), featureId -> false);
    }

    @Autowired
    public FeatureCommandService(
        FeatureRepository features,
        MicroserviceRepository microservices,
        FeatureContextRepository contexts,
        FeatureContextRelationRepository relations,
        FeatureValidator validator,
        FeatureSharingReferenceGuard sharingReferences
    ) {
        this.features = features;
        this.microservices = microservices;
        this.contexts = contexts;
        this.relations = relations;
        this.validator = validator;
        this.sharingReferences = sharingReferences;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "FEATURE_CREATED", resourceType = "FEATURE")
    public FeatureOutput create(CreateFeatureInput input) {
        validator.validateCreate(
            input,
            input != null && features.existsByCode(input.code()),
            input != null && features.existsByName(input.name())
        );
        validator.validateSettings(input.code(), input.settings());
        Microservice microservice = requiredMicroservice(input.microserviceIdentifier());
        validator.validateMicroservice(microservice);
        Feature feature = new Feature(
            input.code(),
            input.name(),
            input.description(),
            microservice,
            input.settings(),
            now()
        );
        feature.setShareable(input.shareable(), now());
        return FeatureOutput.from(features.save(feature));
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_UPDATED", resourceType = "FEATURE")
    public FeatureOutput update(String identifier, UpdateFeatureInput input) {
        Feature feature = features
            .findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
        validator.validateUpdate(
            input,
            input != null && !feature.getName().equals(input.name()) && features.existsByName(input.name())
        );
        if (feature.isShareable() && !input.shareable() && sharingReferences.hasSharedContracts(feature.getId())) {
            throw new ValidationException(PlatformMessageKeys.FEATURE_HAS_SHARED_CONTRACTS);
        }
        validator.validateSettings(feature.getCode(), input.settings());
        Microservice microservice = requiredMicroservice(input.microserviceIdentifier());
        validator.validateMicroservice(microservice);
        if (!feature.getMicroservice().getIdentifier().equals(microservice.getIdentifier())) {
            feature.changeMicroservice(microservice, now());
        }
        feature.update(input.name(), input.description(), input.settings(), now());
        feature.setShareable(input.shareable(), now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.ACTIVATE, event = "FEATURE_ACTIVATED", resourceType = "FEATURE")
    public FeatureOutput activate(String identifier) {
        Feature feature = requiredFeature(identifier);
        validator.validateMicroservice(feature.getMicroservice());
        feature.activate(now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "FEATURE_DEACTIVATED", resourceType = "FEATURE")
    public FeatureOutput inactivate(String identifier) {
        Feature feature = requiredFeature(identifier);
        feature.inactivate(now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "FEATURE_DELETED", resourceType = "FEATURE")
    public FeatureOutput delete(String identifier) {
        Feature feature = requiredFeature(identifier);
        feature.quarantine(now());
        return FeatureOutput.from(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_ASSOCIATED", resourceType = "FEATURE")
    public FeatureOutput associateContext(String identifier, String contextIdentifier) {
        FeatureContext context = contexts
            .findByIdentifierForUpdate(contextIdentifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
        Feature feature = requiredFeatureForUpdate(identifier);
        validator.validateContext(context);
        FeatureContextRelationId relationId = new FeatureContextRelationId(feature.getId(), context.getId());
        if (!relations.existsById(relationId)) {
            relations.save(new FeatureContextRelation(feature, context));
            feature.markUpdated(now());
        }
        return FeatureOutput.from(feature);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_REMOVED", resourceType = "FEATURE")
    public FeatureOutput removeContext(String identifier, String contextIdentifier) {
        FeatureContext context = contexts
            .findByIdentifierForUpdate(contextIdentifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
        Feature feature = requiredFeatureForUpdate(identifier);
        FeatureContextRelationId relationId = new FeatureContextRelationId(feature.getId(), context.getId());
        relations.findById(relationId).ifPresent(relation -> {
            relations.delete(relation);
            feature.markUpdated(now());
        });
        return FeatureOutput.from(feature);
    }

    private Feature requiredFeatureForUpdate(String identifier) {
        return features
            .findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
    }

    private Feature requiredFeature(String identifier) {
        return features
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.FEATURE_NOT_FOUND));
    }

    private Microservice requiredMicroservice(String identifier) {
        return microservices
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.MICROSERVICE_NOT_FOUND));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
