package br.com.portalmanager.platform.workspace.feature.parameter.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.messaging.exception.ResourceVersionConflictException;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.ParameterKeyRepository;
import br.com.portalmanager.platform.workspace.feature.parameter.repository.projection.ParameterKeyReadProjection;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterKeyMetadata;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.MicroserviceReferencePort.Reference;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.validation.ParameterKeyValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class ParameterKeyPublicationCommandService {
    private final ParameterKeyRepository repository;
    private final ParameterKeyValidator validator;
    private final MicroserviceReferencePort microservices;

    public ParameterKeyPublicationCommandService(ParameterKeyRepository repository,
            ParameterKeyValidator validator, MicroserviceReferencePort microservices) {
        this.repository = repository;
        this.validator = validator;
        this.microservices = microservices;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(action = AuditAction.UPDATE, event = "PARAMETER_KEY_PUBLISHED", resourceType = "PARAMETER_KEY")
    public List<ParameterKeyMetadata> publishBatch(Collection<Long> ids, Collection<Long> microserviceIds) {
        String draft = ParameterKeyStatusTypeCode.draft().value();
        String published = ParameterKeyStatusTypeCode.published().value();
        // Short, ordered row locks serialize bulk publication with @Version-based edits.
        // No Redis calls occur inside this transaction.
        List<ParameterKeyReadProjection> drafts = repository.findDraftsForPublication(ids, microserviceIds, draft);
        if (drafts.isEmpty()) return List.of();
        validator.validatePublications(drafts);
        Map<Long, Reference> references = microservices.findByInternalIds(
                drafts.stream().map(ParameterKeyReadProjection::getMicroserviceId).distinct().toList());
        LocalDateTime now = LocalDateTime.now();
        int affected = repository.publishDrafts(drafts.stream().map(ParameterKeyReadProjection::getId).toList(),
                microserviceIds, draft, published, now);
        if (affected != drafts.size()) throw new ResourceVersionConflictException();
        // Platform Audit captures each returned metadata item, within this same transaction.
        // Never include editable/published values in audit snapshots.
        return drafts.stream().map(resource -> {
            Reference reference = references.get(resource.getMicroserviceId());
            return new ParameterKeyMetadata(resource.getVersion() + 1, resource.getIdentifier(),
                    reference.identifier(), resource.getKey(), reference.code() + "." + resource.getKey(),
                    resource.getDescription(), resource.getType(), published, true, resource.getCreatedAt(), now);
        }).toList();
    }
}
