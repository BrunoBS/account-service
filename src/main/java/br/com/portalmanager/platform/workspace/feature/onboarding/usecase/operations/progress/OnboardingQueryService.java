package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.operations.progress;

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
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.OnboardingPhaseTypePort;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.WorkspaceOnboardingPort;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.WorkspaceOnboardingPort.WorkspaceOnboardingReference;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.OnboardingValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final WorkspaceOnboardingPort workspaces;
    private final OnboardingPhaseTypePort phaseCatalogService;
    private final WorkspaceOnboardingCompletionRepository completionRepository;
    private final OnboardingValidator validator;
    private final OnboardingProgressMapper mapper;

    public OnboardingQueryService(
            WorkspaceOnboardingPort workspaces,
            OnboardingPhaseTypePort phaseCatalogService,
            WorkspaceOnboardingCompletionRepository completionRepository,
            OnboardingValidator validator,
            OnboardingProgressMapper mapper) {
        this.workspaces = workspaces;
        this.phaseCatalogService = phaseCatalogService;
        this.completionRepository = completionRepository;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public OnboardingProgressOutput findProgress(String workspaceIdentifier) {
        WorkspaceOnboardingReference workspace =
                workspaces.findOnboardingReference(workspaceIdentifier);
        validator.requireSupportedWorkspace(workspace);

        List<CatalogDTO> configuredPhases = findConfiguredPhases();
        Map<String, WorkspaceOnboardingCompletion> completionsByPhase =
                findCompletionsByPhase(workspace.id());
        List<OnboardingPhaseOutput> phaseOutputs =
                buildPhaseOutputs(configuredPhases, completionsByPhase);
        boolean readyToComplete =
                phaseOutputs.stream()
                        .allMatch(phase -> phase.status() == OnboardingPhaseStatus.COMPLETED);
        return mapper.toOutput(
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
                                mapper.toPhaseOutput(
                                        phase,
                                        completionsByPhase.get(phase.code()),
                                        determinePhaseStatus(
                                                phase.code(),
                                                completionsByPhase.get(phase.code()),
                                                inProgressPhaseCode)))
                .toList();
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
