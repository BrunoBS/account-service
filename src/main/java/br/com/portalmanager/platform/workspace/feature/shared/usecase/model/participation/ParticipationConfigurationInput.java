package br.com.portalmanager.platform.workspace.feature.shared.usecase.model.participation;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.mapping.EnvironmentMappingInput;

public record ParticipationConfigurationInput(
    String publicationModeCode,
    EnvironmentMappingInput environmentMappings
) {}
