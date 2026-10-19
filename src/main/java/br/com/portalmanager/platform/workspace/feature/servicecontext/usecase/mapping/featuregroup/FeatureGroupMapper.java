package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.featuregroup;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.CreateFeatureGroupInput;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class FeatureGroupMapper {

    public FeatureGroup toDomain(CreateFeatureGroupInput input, LocalDateTime now) {
        return new FeatureGroup(input.code(), input.name(), input.description(), now);
    }

    public FeatureGroupOutput toOutput(FeatureGroup featureGroup) {
        return new FeatureGroupOutput(
                featureGroup.getVersion(),
                featureGroup.getIdentifier(),
                featureGroup.getCode(),
                featureGroup.getName(),
                featureGroup.getDescription(),
                featureGroup.getLifecycle().value(),
                featureGroup.getCreatedAt(),
                featureGroup.getUpdatedAt());
    }
}
