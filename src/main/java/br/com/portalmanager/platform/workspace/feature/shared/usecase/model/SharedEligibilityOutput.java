package br.com.portalmanager.platform.workspace.feature.shared.usecase.model;

import java.util.List;

public record SharedEligibilityOutput(boolean eligible, String publicationModeCode,
                                     List<String> destinationEnvironmentIdentifiers) {}
