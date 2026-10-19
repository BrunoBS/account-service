package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroupRelation;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRelationRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRepository;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.feature.FeatureFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature.FeatureMapper;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.operations.featuregroup.FeatureGroupFinder;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup.FeatureGroupAssociationValidator;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class FeatureGroupAssociationCommandService {
    private final FeatureGroupRelationRepository relations;
    private final FeatureGroupFinder groupFinder;
    private final FeatureGroupRepository groups;
    private final FeatureFinder featureFinder;
    private final FeatureMapper featureMapper;
    private final FeatureGroupAssociationValidator validator;
    private final FeatureGroupAssociationOperations associations;

    public FeatureGroupAssociationCommandService(
            FeatureGroupRelationRepository relations,
            FeatureGroupRepository groups,
            FeatureGroupFinder groupFinder,
            FeatureFinder featureFinder,
            FeatureMapper featureMapper,
            FeatureGroupAssociationValidator validator,
            FeatureGroupAssociationOperations associations) {
        this.relations = relations;
        this.groupFinder = groupFinder;
        this.groups = groups;
        this.featureFinder = featureFinder;
        this.featureMapper = featureMapper;
        this.validator = validator;
        this.associations = associations;
    }

    @Transactional
    public FeatureOutput associate(String featureGroupIdentifier, String featureIdentifier) {
        FeatureGroup featureGroupEntity = groupFinder.findForUpdate(featureGroupIdentifier);
        validator.validateFeatureGroupForAssociation(featureGroupEntity);
        Feature feature = featureFinder.findActiveForReference(featureIdentifier);
        // An atomic insert avoids gap locks from checking an absent relation under REPEATABLE_READ.
        if (relations.insertIfAbsent(feature.getId(), featureGroupEntity.getId()) == 0) {
            return featureMapper.toOutput(feature);
        }
        featureGroupEntity.markUpdated(LocalDateTime.now());
        FeatureGroup savedGroup = groups.saveAndFlush(featureGroupEntity);
        return associations.associate(feature, savedGroup).feature();
    }

    @Transactional
    public FeatureOutput remove(String featureGroupIdentifier, String featureIdentifier) {
        FeatureGroup featureGroupEntity = groupFinder.findForUpdate(featureGroupIdentifier);
        Feature feature = featureFinder.findForUpdate(featureIdentifier);
        Optional<FeatureGroupRelation> relation =
                relations.findByPairForUpdate(feature.getId(), featureGroupEntity.getId());
        if (relation.isEmpty()) {
            return featureMapper.toOutput(feature);
        }
        featureGroupEntity.markUpdated(LocalDateTime.now());
        groups.saveAndFlush(featureGroupEntity);
        return associations.remove(feature, relation.get()).feature();
    }
}
