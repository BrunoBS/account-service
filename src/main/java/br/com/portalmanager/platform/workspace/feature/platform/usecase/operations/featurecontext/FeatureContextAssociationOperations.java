package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelation;
import br.com.portalmanager.platform.workspace.feature.platform.repository.feature.FeatureRepository;
import br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.featurecontext.FeatureContextAssociationOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureMapper;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Only actual changes reach these audited operations; retries return the existing representation. */
@Service
public class FeatureContextAssociationOperations {
    private final FeatureContextRelationRepository relations;
    private final FeatureRepository features;
    private final FeatureMapper mapper;

    public FeatureContextAssociationOperations(FeatureContextRelationRepository relations,
        FeatureRepository features, FeatureMapper mapper) {
        this.relations = relations;
        this.features = features;
        this.mapper = mapper;
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_ASSOCIATED", resourceType = "FEATURE")
    public FeatureContextAssociationOutput associate(Feature feature, Context context) {
        // The command has inserted the pair in this transaction; only real changes reach auditing.
        feature.markUpdated(LocalDateTime.now());
        features.flush();
        return new FeatureContextAssociationOutput(mapper.toOutput(feature), context.getIdentifier());
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "FEATURE_CONTEXT_REMOVED", resourceType = "FEATURE")
    public FeatureContextAssociationOutput remove(Feature feature, FeatureContextRelation relation) {
        String contextIdentifier = relation.getContext().getIdentifier();
        relations.delete(relation);
        feature.markUpdated(LocalDateTime.now());
        features.flush();
        return new FeatureContextAssociationOutput(mapper.toOutput(feature), contextIdentifier);
    }
}
