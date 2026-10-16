package br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.participant;

public record ParticipantConfigurationInput(
        String publicationModeCode, EnvironmentMappingInput environmentMappings) {}
