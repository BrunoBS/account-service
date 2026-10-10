package br.com.portalmanager.platform.workspace.foundation.integration;

import java.util.List;

public record SharedEligibilityResult(boolean eligible, String publicationModeCode,
                                      List<String> destinationEnvironmentIdentifiers) {
    public SharedEligibilityResult {
        destinationEnvironmentIdentifiers = destinationEnvironmentIdentifiers == null
                ? List.of() : List.copyOf(destinationEnvironmentIdentifiers);
    }
}
