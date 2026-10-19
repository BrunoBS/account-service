package br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.mapping.feature;

import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.ServiceContextMessageKeys;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.FeatureDetails;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.feature.FeatureRepository.FeatureReference;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.FeatureReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.feature.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupAssociationOutput;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class FeatureMapper {
    private final ObjectMapper objectMapper;

    public FeatureMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public FeatureDetails toDetails(CreateFeatureInput input) {
        return new FeatureDetails(
                input.name(), input.description(), input.settings(), input.shareable());
    }

    public Feature toDomain(CreateFeatureInput input, Microservice microservice, java.time.LocalDateTime now) {
        return new Feature(input.code(), toDetails(input), microservice, now);
    }

    public FeatureDetails toDetails(UpdateFeatureInput input) {
        return new FeatureDetails(
                input.name(), input.description(), input.settings(), input.shareable());
    }

    public FeatureOutput toOutput(Feature feature) {
        return new FeatureOutput(
                feature.getVersion(),
                feature.getIdentifier(),
                feature.getCode(),
                feature.getName(),
                feature.getDescription(),
                feature.getMicroservice().getIdentifier(),
                feature.getMicroservice().getCode(),
                feature.getLifecycle().value(),
                feature.isShareable(),
                readSettings(feature.getSettings()),
                feature.getCreatedAt(),
                feature.getUpdatedAt());
    }

    public FeatureGroupAssociationOutput toAssociationOutput(
            Feature feature, String featureGroupIdentifier) {
        return new FeatureGroupAssociationOutput(toOutput(feature), featureGroupIdentifier);
    }

    public Map<Long, FeatureReferenceOutput> toReferences(List<FeatureReference> references) {
        return references.stream()
                .collect(
                        Collectors.toMap(
                                FeatureReference::getId,
                                reference ->
                                        new FeatureReferenceOutput(
                                                reference.getIdentifier(), reference.getName())));
    }

    private JsonNode readSettings(String settings) {
        try {
            JsonNode parsedSettings = objectMapper.readTree(settings);
            if (parsedSettings == null || !parsedSettings.isObject()) {
                throw new ApiException(ServiceContextMessageKeys.FEATURE_SETTINGS_INVALID);
            }
            return parsedSettings;
        } catch (JacksonException exception) {
            throw new ApiException(ServiceContextMessageKeys.FEATURE_SETTINGS_INVALID, exception);
        }
    }
}
