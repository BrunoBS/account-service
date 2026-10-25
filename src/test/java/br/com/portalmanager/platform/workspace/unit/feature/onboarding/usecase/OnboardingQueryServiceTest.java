package br.com.portalmanager.platform.workspace.unit.feature.onboarding.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.exception.CatalogTechnicalException;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.OnboardingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.WorkspaceOnboardingCompletion;
import br.com.portalmanager.platform.workspace.feature.onboarding.repository.WorkspaceOnboardingCompletionRepository;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.mapping.OnboardingProgressMapper;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase.OnboardingPhaseOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase.OnboardingPhaseStatus;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.progress.OnboardingQueryService;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.OnboardingPhaseTypePort;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.WorkspaceOnboardingPort;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.WorkspaceOnboardingPort.WorkspaceOnboardingReference;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

class OnboardingQueryServiceTest {

    private final WorkspaceOnboardingPort workspaceQueryService =
            mock(WorkspaceOnboardingPort.class);
    private final OnboardingPhaseTypePort phaseCatalogService = mock(OnboardingPhaseTypePort.class);
    private final WorkspaceOnboardingCompletionRepository completionRepository =
            mock(WorkspaceOnboardingCompletionRepository.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final OnboardingQueryService queryService =
            new OnboardingQueryService(
                    workspaceQueryService,
                    phaseCatalogService,
                    completionRepository,
                    new OnboardingValidator(phaseCatalogService),
                    new OnboardingProgressMapper());

    @BeforeEach
    void prepare() {
        when(workspaceQueryService.findOnboardingReference("workspace"))
                .thenReturn(new WorkspaceOnboardingReference(1L, "MANAGER", false));
        when(phaseCatalogService.findByCodes(anyList()))
                .thenReturn(phases(json.createObjectNode(), 1, 2, 3, 4));
        when(completionRepository.findByWorkspaceId(1L)).thenReturn(List.of());
    }

    @Test
    void ordersTiedPhasesDeterministicallyAndKeepsOutOfOrderCompletions() {
        when(phaseCatalogService.findByCodes(anyList()))
                .thenReturn(phases(json.createObjectNode(), 5, 5, 5, 5));
        when(completionRepository.findByWorkspaceId(1L))
                .thenReturn(
                        List.of(
                                completion("WORKSPACE_REGISTRATION"),
                                completion("FIRST_APPLICATION_REGISTRATION")));

        OnboardingProgressOutput progress = queryService.findProgress("workspace");
        assertThat(progress.phases())
                .extracting(OnboardingPhaseOutput::code)
                .containsExactly(
                        "APPLICATION_FIRST_ENVIRONMENT", "FIRST_APPLICATION_REGISTRATION",
                        "WORKSPACE_FIRST_ENVIRONMENT", "WORKSPACE_REGISTRATION");
        assertThat(progress.phases())
                .extracting(OnboardingPhaseOutput::status)
                .containsExactly(
                        OnboardingPhaseStatus.IN_PROGRESS, OnboardingPhaseStatus.COMPLETED,
                        OnboardingPhaseStatus.PENDING, OnboardingPhaseStatus.COMPLETED);
        assertThat(progress.readyToComplete()).isFalse();
    }

    @Test
    void distinguishesReadinessFromFinalCompletion() {
        when(completionRepository.findByWorkspaceId(1L))
                .thenReturn(
                        List.of(
                                completion("WORKSPACE_REGISTRATION"),
                                        completion("WORKSPACE_FIRST_ENVIRONMENT"),
                                completion("FIRST_APPLICATION_REGISTRATION"),
                                        completion("APPLICATION_FIRST_ENVIRONMENT")));

        OnboardingProgressOutput progress = queryService.findProgress("workspace");
        assertThat(progress.readyToComplete()).isTrue();
        assertThat(progress.completed()).isFalse();
    }

    @Test
    void rejectsAnEmptyCatalogInsteadOfReportingReady() {
        when(phaseCatalogService.findByCodes(anyList())).thenReturn(List.of());
        assertConfigurationFailure(OnboardingMessageKeys.PHASES_NOT_CONFIGURED);
    }

    @Test
    void rejectsInvalidStoredSettingsWithoutCoercingOrientation() {
        for (String settings :
                List.of(
                        "null",
                        "[]",
                        "123",
                        "{\"orientation\":123}",
                        "{\"orientation\":true}",
                        "{\"orientation\":[]}")) {
            when(phaseCatalogService.findByCodes(anyList()))
                    .thenReturn(phases(json.readTree(settings), 1, 2, 3, 4));
            assertConfigurationFailure(OnboardingMessageKeys.SETTINGS_INVALID);
        }
    }

    @Test
    void acceptsOptionalNullOrientation() {
        when(phaseCatalogService.findByCodes(anyList()))
                .thenReturn(phases(json.readTree("{\"orientation\":null}"), 1, 2, 3, 4));
        assertThat(queryService.findProgress("workspace").phases().getFirst().orientation())
                .isNull();
    }

    @Test
    void preservesTheCauseOfMalformedCatalogJson() {
        RuntimeException parsingFailure = new IllegalArgumentException("Invalid stored JSON");
        CatalogTechnicalException catalogFailure =
                new CatalogTechnicalException(
                        CatalogMessageKeys.SETTINGS_INVALID_STORED_JSON, Map.of(), parsingFailure);
        when(phaseCatalogService.findByCodes(anyList())).thenThrow(catalogFailure);
        assertThatThrownBy(() -> queryService.findProgress("workspace"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        failure -> {
                            assertThat(failure.getMessageKey())
                                    .isEqualTo(OnboardingMessageKeys.SETTINGS_INVALID);
                            assertThat(failure.getCause()).isSameAs(catalogFailure);
                            assertThat(failure.getCause().getCause()).isSameAs(parsingFailure);
                        });
        verifyNoInteractions(completionRepository);
    }

    private void assertConfigurationFailure(String messageKey) {
        assertThatThrownBy(() -> queryService.findProgress("workspace"))
                .isInstanceOfSatisfying(
                        ApiException.class,
                        failure -> assertThat(failure.getMessageKey()).isEqualTo(messageKey));
        verifyNoInteractions(completionRepository);
    }

    private List<CatalogDTO> phases(
            JsonNode firstPhaseSettings,
            int firstOrder,
            int secondOrder,
            int thirdOrder,
            int fourthOrder) {
        return List.of(
                new CatalogDTO(
                        "WORKSPACE_REGISTRATION",
                        "Registration",
                        null,
                        firstOrder,
                        firstPhaseSettings),
                new CatalogDTO(
                        "WORKSPACE_FIRST_ENVIRONMENT",
                        "Workspace environment",
                        null,
                        secondOrder,
                        json.createObjectNode()),
                new CatalogDTO(
                        "FIRST_APPLICATION_REGISTRATION",
                        "Application",
                        null,
                        thirdOrder,
                        json.createObjectNode()),
                new CatalogDTO(
                        "APPLICATION_FIRST_ENVIRONMENT",
                        "Application environment",
                        null,
                        fourthOrder,
                        json.createObjectNode()));
    }

    private WorkspaceOnboardingCompletion completion(String code) {
        return new WorkspaceOnboardingCompletion(
                1L,
                OnboardingPhaseTypeCode.of(code),
                "phase-user",
                LocalDateTime.of(2026, 10, 1, 12, 0));
    }
}
