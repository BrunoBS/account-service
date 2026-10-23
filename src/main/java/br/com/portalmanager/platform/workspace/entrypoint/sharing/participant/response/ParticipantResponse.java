package br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response;

import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.EnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.ParticipantOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentOutput;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

public record ParticipantResponse(
        String identifier,
        String contractIdentifier,
        ContractSummary contract,
        String participantWorkspaceIdentifier,
        String participantApplicationIdentifier,
        String status,
        String publicationMode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<EnvironmentMapping> mappings,
        @JsonInclude(JsonInclude.Include.NON_NULL)
                List<SourceEnvironmentResponse> sourceEnvironments) {
    public record ContractSummary(
            String identifier,
            String name,
            String lifecycle,
            String ownerWorkspaceIdentifier,
            String ownerApplicationIdentifier,
            String featureIdentifier) {}

    public record EnvironmentMapping(
            String sourceEnvironmentIdentifier, String destinationEnvironmentIdentifier) {}

    public static ParticipantResponse from(ParticipantOutput output) {
        return new ParticipantResponse(
                output.identifier(),
                output.contractIdentifier(),
                new ContractSummary(
                        output.contract().identifier(),
                        output.contract().name(),
                        output.contract().lifecycle(),
                        output.contract().ownerWorkspaceIdentifier(),
                        output.contract().ownerApplicationIdentifier(),
                        output.contract().featureIdentifier()),
                output.participantWorkspaceIdentifier(),
                output.participantApplicationIdentifier(),
                output.status(),
                output.publicationMode(),
                output.createdAt(),
                output.updatedAt(),
                output.mappings().stream()
                        .map(
                                mapping ->
                                        new EnvironmentMapping(
                                                mapping.sourceEnvironmentIdentifier(),
                                                mapping.destinationEnvironmentIdentifier()))
                        .toList(),
                output.sourceEnvironments() == null
                        ? null
                        : output.sourceEnvironments().stream()
                                .map(SourceEnvironmentResponse::from)
                                .toList());
    }

    public record SourceEnvironmentResponse(
            Long version,
            String identifier,
            String workspaceIdentifier,
            String name,
            String description,
            String authorizationType,
            String environmentTypeCode,
            String parentIdentifier,
            Integer sortOrder,
            String authorizerGroup,
            JsonNode settings,
            String lifecycle,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        public static SourceEnvironmentResponse from(EnvironmentOutput output) {
            return new SourceEnvironmentResponse(
                    output.version(),
                    output.identifier(),
                    output.workspaceIdentifier(),
                    output.name(),
                    output.description(),
                    output.authorizationType(),
                    output.environmentTypeCode(),
                    output.parentIdentifier(),
                    output.sortOrder(),
                    output.authorizerGroup(),
                    output.settings(),
                    output.lifecycle(),
                    output.createdAt(),
                    output.updatedAt());
        }
    }
}
