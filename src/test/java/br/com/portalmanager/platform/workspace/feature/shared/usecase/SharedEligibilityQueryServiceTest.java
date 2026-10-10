package br.com.portalmanager.platform.workspace.feature.shared.usecase;

import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedContract;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedEnvironmentMapping;
import br.com.portalmanager.platform.workspace.feature.shared.domain.SharedParticipation;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedContractRepository;
import br.com.portalmanager.platform.workspace.feature.shared.repository.SharedParticipationRepository;
import br.com.portalmanager.platform.workspace.feature.shared.usecase.operations.SharedEligibilityQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.publicationmodetype.domain.PublicationModeTypeEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SharedEligibilityQueryServiceTest {
    private static final String OWNER_WORKSPACE = "owner-workspace";
    private static final String OWNER_APPLICATION = "owner-application";
    private static final String PARTICIPANT_WORKSPACE = "participant-workspace";
    private static final String PARTICIPANT_APPLICATION = "participant-application";
    private static final String SOURCE_ENVIRONMENT = "source-environment";
    private static final String DESTINATION_ONE = "destination-one";
    private static final String DESTINATION_TWO = "destination-two";
    private static final String COMMON_BASE = "common-default-environment";

    private final SharedContractRepository contracts = mock(SharedContractRepository.class);
    private final SharedParticipationRepository participations = mock(SharedParticipationRepository.class);
    private final ApplicationQueryService applications = mock(ApplicationQueryService.class);
    private final EnvironmentQueryService environments = mock(EnvironmentQueryService.class);
    private final SharedEligibilityQueryService service = new SharedEligibilityQueryService(
            contracts, participations, applications, environments);
    private final Map<String, EnvironmentOutput> activeEnvironments = new LinkedHashMap<>();
    private SharedContract contract;
    private SharedParticipation participation;

    @BeforeEach
    void prepare() {
        LocalDateTime now = LocalDateTime.now();
        contract = new SharedContract(OWNER_WORKSPACE, OWNER_APPLICATION, "Contract", null, now);
        participation = new SharedParticipation(contract, PARTICIPANT_WORKSPACE, PARTICIPANT_APPLICATION, now);
        participation.approve(PublicationModeTypeCode.of(PublicationModeTypeEnum.AUTOMATIC), now);
        participation.replaceMappings(new LinkedHashSet<>(Set.of(
                new SharedEnvironmentMapping(SOURCE_ENVIRONMENT, DESTINATION_ONE, now),
                new SharedEnvironmentMapping(SOURCE_ENVIRONMENT, DESTINATION_TWO, now))), now);

        activeEnvironments.put(SOURCE_ENVIRONMENT, environment(SOURCE_ENVIRONMENT, PARTICIPANT_WORKSPACE, COMMON_BASE));
        activeEnvironments.put(DESTINATION_ONE, environment(DESTINATION_ONE, OWNER_WORKSPACE, COMMON_BASE));
        activeEnvironments.put(DESTINATION_TWO, environment(DESTINATION_TWO, OWNER_WORKSPACE, COMMON_BASE));

        when(applications.findActiveForShared(anyString(), anyString())).thenReturn(null);
        when(contracts.findByIdentifierAndOwnerWorkspaceIdentifierAndOwnerApplicationIdentifier(
                "contract", OWNER_WORKSPACE, OWNER_APPLICATION)).thenReturn(Optional.of(contract));
        when(participations.findByContractIdAndParticipantApplicationIdentifier(
                contract.getId(), PARTICIPANT_APPLICATION)).thenReturn(Optional.of(participation));
        when(environments.findActiveForShared(anyString(), anyString())).thenAnswer(invocation ->
                activeEnvironments.get(invocation.getArgument(1)));
    }

    @Test
    void resolvesAllActiveMappedDestinationsForAnApprovedParticipation() {
        var result = service.resolve(OWNER_WORKSPACE, OWNER_APPLICATION, "contract", PARTICIPANT_WORKSPACE,
                PARTICIPANT_APPLICATION, SOURCE_ENVIRONMENT);

        assertThat(result.eligible()).isTrue();
        assertThat(result.publicationModeCode()).isEqualTo("AUTOMATIC");
        assertThat(result.destinationEnvironmentIdentifiers()).containsExactlyInAnyOrder(DESTINATION_ONE, DESTINATION_TWO);
    }

    @Test
    void ignoresDestinationWithMismatchedBaseAndKeepsTheValidDestination() {
        activeEnvironments.put(DESTINATION_TWO,
                environment(DESTINATION_TWO, OWNER_WORKSPACE, "different-default-environment"));

        var result = service.resolve(OWNER_WORKSPACE, OWNER_APPLICATION, "contract", PARTICIPANT_WORKSPACE,
                PARTICIPANT_APPLICATION, SOURCE_ENVIRONMENT);

        assertThat(result.eligible()).isTrue();
        assertThat(result.destinationEnvironmentIdentifiers()).containsExactly(DESTINATION_ONE);
    }

    @Test
    void returnsIneligibleWhenTheContractIsInactive() {
        contract.inactivate(LocalDateTime.now());

        var result = service.resolve(OWNER_WORKSPACE, OWNER_APPLICATION, "contract", PARTICIPANT_WORKSPACE,
                PARTICIPANT_APPLICATION, SOURCE_ENVIRONMENT);

        assertThat(result.eligible()).isFalse();
        assertThat(result.destinationEnvironmentIdentifiers()).isEmpty();
    }

    private EnvironmentOutput environment(String identifier, String workspace, String parent) {
        return new EnvironmentOutput(0L, identifier, workspace, identifier, "Environment", "DEV", "CUSTOM",
                parent, 1, null, "{}", "ACTIVE", LocalDateTime.now(), LocalDateTime.now());
    }
}
