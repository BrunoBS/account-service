package br.com.portalmanager.platform.workspace.feature.sharing.usecase.operations.participant;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.participant.EnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.mapping.participant.ParticipantMapper;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant.EnvironmentMappingInput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.port.SharingReferencePort.EnvironmentReferenceOutput;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class EnvironmentMappingService {

    private final SharingReferencePort references;
    private final ParticipantMapper mapper;

    public EnvironmentMappingService(SharingReferencePort references, ParticipantMapper mapper) {
        this.references = references;
        this.mapper = mapper;
    }

    /** The command validates the complete input before resolving any references. */
    public LinkedHashSet<EnvironmentMapping> build(
            Long ownerWorkspaceId,
            Long participantWorkspaceId,
            String ownerWorkspaceIdentifier,
            String participantWorkspaceIdentifier,
            EnvironmentMappingInput input,
            LocalDateTime timestamp) {
        Map<String, EnvironmentReferenceOutput> sourceEnvironments =
                findSourceEnvironments(
                        participantWorkspaceId, participantWorkspaceIdentifier, input.mappings());
        Map<String, EnvironmentReferenceOutput> destinationEnvironments =
                findDestinationEnvironments(
                        ownerWorkspaceId, ownerWorkspaceIdentifier, input.mappings());
        LinkedHashSet<EnvironmentMapping> environmentMappings = new LinkedHashSet<>();
        for (EnvironmentMappingInput.Mapping mapping : input.mappings()) {
            EnvironmentReferenceOutput sourceEnvironment =
                    sourceEnvironments.get(mapping.sourceEnvironmentIdentifier());
            for (String destinationIdentifier : mapping.destinationEnvironmentIdentifiers()) {
                EnvironmentReferenceOutput destinationEnvironment =
                        destinationEnvironments.get(destinationIdentifier);
                environmentMappings.add(
                        createMapping(sourceEnvironment, destinationEnvironment, timestamp));
            }
        }
        return environmentMappings;
    }

    private Map<String, EnvironmentReferenceOutput> findSourceEnvironments(
            Long participantWorkspaceId,
            String participantWorkspaceIdentifier,
            List<EnvironmentMappingInput.Mapping> mappings) {
        List<String> sourceEnvironmentIdentifiers =
                mappings.stream()
                        .map(EnvironmentMappingInput.Mapping::sourceEnvironmentIdentifier)
                        .distinct()
                        .toList();
        return references.findAvailableReferencesForResourceSharing(
                participantWorkspaceId,
                participantWorkspaceIdentifier,
                sourceEnvironmentIdentifiers);
    }

    private Map<String, EnvironmentReferenceOutput> findDestinationEnvironments(
            Long ownerWorkspaceId,
            String ownerWorkspaceIdentifier,
            List<EnvironmentMappingInput.Mapping> mappings) {
        List<String> destinationEnvironmentIdentifiers =
                mappings.stream()
                        .flatMap(mapping -> mapping.destinationEnvironmentIdentifiers().stream())
                        .distinct()
                        .toList();
        return references.findAvailableReferencesForResourceSharing(
                ownerWorkspaceId, ownerWorkspaceIdentifier, destinationEnvironmentIdentifiers);
    }

    private EnvironmentMapping createMapping(
            EnvironmentReferenceOutput sourceEnvironment,
            EnvironmentReferenceOutput destinationEnvironment,
            LocalDateTime timestamp) {
        String sourceBaseIdentifier = baseIdentifier(sourceEnvironment.environment());
        String destinationBaseIdentifier = baseIdentifier(destinationEnvironment.environment());
        if (!Objects.equals(sourceBaseIdentifier, destinationBaseIdentifier)) {
            throw new ValidationException(ResourceSharingMessageKeys.MAPPING_INVALID);
        }
        return mapper.toDomain(sourceEnvironment.id(), destinationEnvironment.id(), timestamp);
    }

    private String baseIdentifier(EnvironmentOutput environment) {
        return environment.workspaceIdentifier() == null || environment.parentIdentifier() == null
                ? environment.identifier()
                : environment.parentIdentifier();
    }
}
