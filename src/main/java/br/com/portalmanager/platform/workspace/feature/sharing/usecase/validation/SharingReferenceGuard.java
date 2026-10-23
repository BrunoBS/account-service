package br.com.portalmanager.platform.workspace.feature.sharing.usecase.validation;

import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.message.AuthorizationMessageKeys;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.sharing.domain.ResourceSharingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.participant.ParticipantRepository;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class SharingReferenceGuard {
    private final ContractRepository contracts;
    private final ParticipantRepository participants;

    public SharingReferenceGuard(ContractRepository contracts, ParticipantRepository participants) {
        this.contracts = contracts;
        this.participants = participants;
    }

    public void requireContext() {
        UserContext.get()
                .orElseThrow(
                        () ->
                                new UnauthorizedAccessException(
                                        AuthorizationMessageKeys.SESSION_NOT_FOUND));
    }

    /** Retains inactive history: linkage, not lifecycle, authorizes reference resolution. */
    public Map<Long, Set<Long>> requireApplications(Collection<Long> ids) {
        requireContext();
        if (ids.isEmpty()) return Map.of();
        Map<Long, Set<Long>> links = new HashMap<>();
        Stream.concat(
                        contracts.findApplicationLinks(ids).stream(),
                        participants.findApplicationLinks(ids).stream())
                .forEach(
                        link ->
                                links.computeIfAbsent(
                                                link.getApplicationId(), ignored -> new HashSet<>())
                                        .add(link.getWorkspaceId()));
        if (!links.keySet().containsAll(ids)) deny();
        return links;
    }

    public void requireApplication(Long workspaceId, Long applicationId) {
        if (!requireApplications(List.of(applicationId)).get(applicationId).contains(workspaceId))
            deny();
    }

    public void requireResolvedApplication(
            Map<Long, Set<Long>> links, Long workspaceId, Long applicationId) {
        if (!links.getOrDefault(applicationId, Set.of()).contains(workspaceId)) deny();
    }

    public void requireWorkspace(Long workspaceId) {
        requireContext();
        if (!contracts.existsByOwnerWorkspaceId(workspaceId)
                && !participants.existsByParticipantWorkspaceId(workspaceId)) deny();
    }

    public void requireEnvironmentMappings(Collection<Long> ids) {
        requireContext();
        if (ids.isEmpty()) return;
        Set<Long> linked = new HashSet<>(participants.findSourceEnvironmentLinks(ids));
        linked.addAll(participants.findDestinationEnvironmentLinks(ids));
        if (!linked.containsAll(ids)) deny();
    }

    private void deny() {
        throw new NotFoundException(ResourceSharingMessageKeys.NOT_FOUND);
    }
}
