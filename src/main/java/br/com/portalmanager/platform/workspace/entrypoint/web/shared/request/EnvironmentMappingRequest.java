package br.com.portalmanager.platform.workspace.entrypoint.web.shared.request;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.EnvironmentMappingInput;
import java.util.List;

public record EnvironmentMappingRequest(List<Mapping> mappings) {
    public record Mapping(String sourceEnvironmentIdentifier, List<String> destinationEnvironmentIdentifiers) {}

    public EnvironmentMappingInput toInput() {
        return new EnvironmentMappingInput(
            mappings == null
                ? null
                : mappings
                      .stream()
                      .map(m ->
                          m == null
                              ? null
                              : new EnvironmentMappingInput.Mapping(
                                    m.sourceEnvironmentIdentifier(),
                                    m.destinationEnvironmentIdentifiers()
                                )
                      )
                      .toList()
        );
    }
}
