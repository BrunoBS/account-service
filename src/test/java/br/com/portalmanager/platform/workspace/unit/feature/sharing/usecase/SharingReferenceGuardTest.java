package br.com.portalmanager.platform.workspace.unit.feature.sharing.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.portalmanager.platform.library.authorization.exception.UnauthorizedAccessException;
import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithUserContextIsolation;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.contract.ContractRepository;
import br.com.portalmanager.platform.workspace.feature.sharing.repository.participant.ParticipantRepository;
import br.com.portalmanager.platform.workspace.feature.sharing.usecase.validation.SharingReferenceGuard;

import org.junit.jupiter.api.Test;

import java.util.*;

@WithUserContextIsolation
class SharingReferenceGuardTest {
    private final ContractRepository contracts = mock(ContractRepository.class);
    private final ParticipantRepository participants = mock(ParticipantRepository.class);
    private final SharingReferenceGuard guard = new SharingReferenceGuard(contracts, participants);

    @Test
    void refusesPrivilegedReferencesWithoutAnAuthenticatedContext() {
        UserContext.clear();
        assertThatThrownBy(() -> guard.requireApplications(List.of(1L)))
                .isInstanceOf(UnauthorizedAccessException.class);
        verifyNoInteractions(contracts, participants);
    }

    @Test
    void requiresPersistedContractOrParticipationAndTheExactWorkspaceApplicationPair() {
        UserContext.set(new UserSession());
        ContractRepository.ApplicationLink owner = mock(ContractRepository.ApplicationLink.class);
        when(owner.getApplicationId()).thenReturn(2L);
        when(owner.getWorkspaceId()).thenReturn(1L);
        when(contracts.findApplicationLinks(List.of(2L))).thenReturn(List.of(owner));
        when(participants.findApplicationLinks(List.of(2L))).thenReturn(List.of());
        assertThatCode(() -> guard.requireApplication(1L, 2L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> guard.requireApplication(9L, 2L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> guard.requireResolvedApplication(Map.of(2L, Set.of(1L)), 9L, 2L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void refusesUnlinkedApplicationWorkspaceAndEnvironmentReferences() {
        UserContext.set(new UserSession());
        when(contracts.findApplicationLinks(List.of(3L))).thenReturn(List.of());
        when(participants.findApplicationLinks(List.of(3L))).thenReturn(List.of());
        assertThatThrownBy(() -> guard.requireApplications(List.of(3L)))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> guard.requireWorkspace(9L)).isInstanceOf(NotFoundException.class);
        when(participants.findSourceEnvironmentLinks(List.of(7L))).thenReturn(List.of());
        when(participants.findDestinationEnvironmentLinks(List.of(7L))).thenReturn(List.of());
        assertThatThrownBy(() -> guard.requireEnvironmentMappings(List.of(7L)))
                .isInstanceOf(NotFoundException.class);
    }
}
