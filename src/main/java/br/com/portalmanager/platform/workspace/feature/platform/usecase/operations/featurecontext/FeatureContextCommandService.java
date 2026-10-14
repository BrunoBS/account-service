package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.featurecontext;

import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContextRelation;
import br.com.portalmanager.platform.workspace.feature.platform.repository.featurecontext.FeatureContextRelationRepository;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context.ContextFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureMapper;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.featurecontext.FeatureContextValidator;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeatureContextCommandService {
    private final FeatureContextRelationRepository relations;
    private final ContextFinder contextFinder;
    private final FeatureFinder featureFinder;
    private final FeatureMapper featureMapper;
    private final FeatureContextValidator validator;
    private final FeatureContextAssociationOperations associations;

    public FeatureContextCommandService(FeatureContextRelationRepository relations,
        ContextFinder contextFinder, FeatureFinder featureFinder, FeatureMapper featureMapper,
        FeatureContextValidator validator, FeatureContextAssociationOperations associations) {
        this.relations = relations;
        this.contextFinder = contextFinder;
        this.featureFinder = featureFinder;
        this.featureMapper = featureMapper;
        this.validator = validator;
        this.associations = associations;
    }

    @Transactional
    public FeatureOutput associate(String contextIdentifier, String featureIdentifier) {
        Context context = contextFinder.findForUpdate(contextIdentifier);
        validator.validateContextForAssociation(context);
        Feature feature = featureFinder.findActiveForReference(featureIdentifier);
        // An atomic insert avoids gap locks from checking an absent relation under REPEATABLE_READ.
        if (relations.insertIfAbsent(feature.getId(), context.getId()) == 0) {
            return featureMapper.toOutput(feature);
        }
        context.markUpdated(LocalDateTime.now());
        return associations.associate(feature, context).feature();
    }

    @Transactional
    public FeatureOutput remove(String contextIdentifier, String featureIdentifier) {
        Context context = contextFinder.findForUpdate(contextIdentifier);
        Feature feature = featureFinder.findForUpdate(featureIdentifier);
        Optional<FeatureContextRelation> relation = relations.findByPairForUpdate(feature.getId(), context.getId());
        if (relation.isEmpty()) {
            return featureMapper.toOutput(feature);
        }
        context.markUpdated(LocalDateTime.now());
        return associations.remove(feature, relation.get()).feature();
    }
}
