package br.com.portalmanager.platform.workspace.feature.shared.usecase.model;

import java.util.List;
public record EnvironmentMappingInput(List<Mapping> mappings) {
    public record Mapping(String sourceEnvironmentIdentifier, List<String> destinationEnvironmentIdentifiers) {}
}
