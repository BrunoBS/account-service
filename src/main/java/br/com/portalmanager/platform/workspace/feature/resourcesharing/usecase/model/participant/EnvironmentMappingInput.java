package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant;

import java.util.List;

public record EnvironmentMappingInput(List<Mapping> mappings) {
    public record Mapping(
            String sourceEnvironmentIdentifier, List<String> destinationEnvironmentIdentifiers) {}
}
