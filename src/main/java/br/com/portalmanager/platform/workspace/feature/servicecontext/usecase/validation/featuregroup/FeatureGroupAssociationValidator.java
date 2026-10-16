package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.validation.ServiceContextValidation;

import org.springframework.stereotype.Component;

@Component
public class FeatureGroupAssociationValidator {
    public void validateFeatureGroupForAssociation(FeatureGroup featureGroupEntity) {
        if (!featureGroupEntity.isActive()) {
            ServiceContextValidation.reject(
                    "featureGroupIdentifier", ServiceContextMessageKeys.FEATURE_GROUP_INACTIVE);
        }
    }
}
