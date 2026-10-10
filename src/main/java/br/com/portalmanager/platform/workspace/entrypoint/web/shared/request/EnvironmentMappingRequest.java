package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.mapping.EnvironmentMappingInput;
import java.util.List;

public record EnvironmentMappingRequest(List<Mapping> mappings) {
    public record Mapping(String sourceEnvironmentIdentifier, List<String> destinationEnvironmentIdentifiers) {}

    public EnvironmentMappingInput toInput() {
        return new EnvironmentMappingInput(
            mappings == null
                ? null
                : mappings
                      .stream()
                      .map(mapping ->
                          mapping == null
                              ? null
                              : new EnvironmentMappingInput.Mapping(
                                    mapping.sourceEnvironmentIdentifier(),
                                    mapping.destinationEnvironmentIdentifiers()
                                )
                      )
                      .toList()
        );
    }
}
