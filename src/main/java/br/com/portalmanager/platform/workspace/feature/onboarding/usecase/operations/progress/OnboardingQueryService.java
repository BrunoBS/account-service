package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.progress;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.catalog.exception.CatalogTechnicalException;
import br.com.portalmanager.platform.library.catalog.message.CatalogMessageKeys;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOnboardingReferenceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.OnboardingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.completion.WorkspaceOnboardingCompletion;
import br.com.portalmanager.platform.workspace.feature.onboarding.repository.completion.WorkspaceOnboardingCompletionRepository;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase.OnboardingPhaseOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.phase.OnboardingPhaseStatus;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.progress.OnboardingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OnboardingQueryService {

    private static final List<String> REQUIRED_PHASE_CODES =
            List.of(
                    OnboardingPhaseTypeCode.workspaceRegistration().value(),
                    OnboardingPhaseTypeCode.workspaceFirstEnvironment().value(),
                    OnboardingPhaseTypeCode.firstApplicationRegistration().value(),
                    OnboardingPhaseTypeCode.applicationFirstEnvironment().value());

    private static final Comparator<CatalogDTO> PHASE_ORDER =
            Comparator.comparing(CatalogDTO::sortOrder).thenComparing(CatalogDTO::code);

    private final WorkspaceQueryService workspaceQueryService;
    private final OnboardingPhaseTypeService phaseCatalogService;
    private final WorkspaceOnboardingCompletionRepository completionRepository;
    private final OnboardingValidator validator;

    public OnboardingQueryService(
            WorkspaceQueryService workspaceQueryService,
            OnboardingPhaseTypeService phaseCatalogService,
            WorkspaceOnboardingCompletionRepository completionRepository,
            OnboardingValidator validator) {
        this.workspaceQueryService = workspaceQueryService;
        this.phaseCatalogService = phaseCatalogService;
        this.completionRepository = completionRepository;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public OnboardingProgressOutput findProgress(String workspaceIdentifier) {
        WorkspaceOnboardingReferenceOutput workspace =
                workspaceQueryService.findOnboardingReference(workspaceIdentifier);
        validator.requireSupportedWorkspace(workspace);

        List<CatalogDTO> configuredPhases = findConfiguredPhases();
        Map<String, WorkspaceOnboardingCompletion> completionsByPhase =
                findCompletionsByPhase(workspace.id());
        List<OnboardingPhaseOutput> phaseOutputs =
                buildPhaseOutputs(configuredPhases, completionsByPhase);
        boolean readyToComplete =
                phaseOutputs.stream()
                        .allMatch(phase -> phase.status() == OnboardingPhaseStatus.COMPLETED);
        return new OnboardingProgressOutput(
                workspaceIdentifier,
                workspace.workspaceType(),
                workspace.onboarding(),
                readyToComplete,
                phaseOutputs);
    }

    private Map<String, WorkspaceOnboardingCompletion> findCompletionsByPhase(Long workspaceId) {
        return completionRepository.findByWorkspaceId(workspaceId).stream()
                .collect(
                        Collectors.toMap(
                                WorkspaceOnboardingCompletion::getPhaseCode, Function.identity()));
    }

    private List<OnboardingPhaseOutput> buildPhaseOutputs(
            List<CatalogDTO> configuredPhases,
            Map<String, WorkspaceOnboardingCompletion> completionsByPhase) {
        String inProgressPhaseCode =
                configuredPhases.stream()
                        .map(CatalogDTO::code)
                        .filter(phaseCode -> !completionsByPhase.containsKey(phaseCode))
                        .findFirst()
                        .orElse(null);

        return configuredPhases.stream()
                .map(
                        phase ->
                                toPhaseOutput(
                                        phase,
                                        completionsByPhase.get(phase.code()),
                                        inProgressPhaseCode))
                .toList();
    }

    private OnboardingPhaseOutput toPhaseOutput(
            CatalogDTO phase,
            WorkspaceOnboardingCompletion completion,
            String inProgressPhaseCode) {
        JsonNode orientation = phase.settings().get("orientation");
        return new OnboardingPhaseOutput(
                phase.code(),
                phase.label(),
                phase.description(),
                phase.sortOrder(),
                orientation == null || orientation.isNull() ? null : orientation.asString(),
                phase.settings(),
                determinePhaseStatus(phase.code(), completion, inProgressPhaseCode),
                completion == null ? null : completion.getRequester(),
                completion == null ? null : completion.getCompletedAt());
    }

    private OnboardingPhaseStatus determinePhaseStatus(
            String phaseCode,
            WorkspaceOnboardingCompletion completion,
            String inProgressPhaseCode) {
        if (completion != null) return OnboardingPhaseStatus.COMPLETED;
        if (phaseCode.equals(inProgressPhaseCode)) return OnboardingPhaseStatus.IN_PROGRESS;
        return OnboardingPhaseStatus.PENDING;
    }

    private List<CatalogDTO> findConfiguredPhases() {
        List<CatalogDTO> configuredPhases;
        try {
            configuredPhases = phaseCatalogService.findByCodes(REQUIRED_PHASE_CODES);
        } catch (CatalogTechnicalException exception) {
            if (!CatalogMessageKeys.SETTINGS_INVALID_STORED_JSON.equals(exception.getMessageKey()))
                throw exception;
            throw new ApiException(OnboardingMessageKeys.SETTINGS_INVALID, exception);
        }
        validator.requireConfiguredPhases(configuredPhases, REQUIRED_PHASE_CODES.size());
        return configuredPhases.stream().sorted(PHASE_ORDER).toList();
    }
}
