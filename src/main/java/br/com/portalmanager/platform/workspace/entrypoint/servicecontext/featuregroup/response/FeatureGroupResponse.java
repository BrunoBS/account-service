package br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.response;

import br.com.portalmanager.platform.workspace.feature.servicecontext.usecase.model.featuregroup.FeatureGroupOutput;

import java.time.LocalDateTime;

public record FeatureGroupResponse(
        Long version,
        String identifier,
        String code,
        String name,
        String description,
        String lifecycle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    public static FeatureGroupResponse from(FeatureGroupOutput output) {
        return new FeatureGroupResponse(
                output.version(),
                output.identifier(),
                output.code(),
                output.name(),
                output.description(),
                output.lifecycle(),
                output.createdAt(),
                output.updatedAt());
    }
}
