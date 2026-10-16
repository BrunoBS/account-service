package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.validation.progress;

import br.com.portalmanager.platform.library.authorization.model.UserContext;
import br.com.portalmanager.platform.library.authorization.model.UserSession;
import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.messaging.exception.ApiException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOnboardingReferenceOutput;
import br.com.portalmanager.platform.workspace.feature.onboarding.domain.OnboardingMessageKeys;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.model.progress.OnboardingProgressOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;

import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;

import java.util.List;

@Component
public class OnboardingValidator {

    private static final int REQUESTER_MAX_LENGTH = 10;

    private final OnboardingPhaseTypeService phaseCatalogService;

    public OnboardingValidator(OnboardingPhaseTypeService phaseCatalogService) {
        this.phaseCatalogService = phaseCatalogService;
    }

    public void requireSupportedWorkspace(WorkspaceOnboardingReferenceOutput workspace) {
        if (!WorkspaceTypeCode.manager().value().equals(workspace.workspaceType())) {
            throw new ValidationException(OnboardingMessageKeys.WORKSPACE_TYPE_NOT_SUPPORTED);
        }
    }

    public void requireConfiguredPhases(List<CatalogDTO> configuredPhases, int requiredPhaseCount) {
        if (configuredPhases.size() != requiredPhaseCount) {
            throw new ApiException(OnboardingMessageKeys.PHASES_NOT_CONFIGURED);
        }
        configuredPhases.forEach(phase -> requireValidSettings(phase.settings()));
    }

    public void requireReadyToComplete(OnboardingProgressOutput progress) {
        if (!progress.readyToComplete())
            throw new ValidationException(OnboardingMessageKeys.INCOMPLETE);
    }

    public String requireActivePhase(OnboardingPhaseTypeCode phaseCode) {
        String code = phaseCode == null ? null : phaseCode.value();
        if (!phaseCatalogService.existsActive(code)) {
            throw new ApiException(OnboardingMessageKeys.PHASES_NOT_CONFIGURED);
        }
        return code;
    }

    public String requireAuthenticatedRequester() {
        String requester =
                UserContext.get()
                        .map(UserSession::getUserName)
                        .filter(name -> !name.isBlank())
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                OnboardingMessageKeys
                                                        .AUTHORIZATION_CONTEXT_UNAVAILABLE));
        if (requester.codePointCount(0, requester.length()) > REQUESTER_MAX_LENGTH) {
            throw new ApiException(OnboardingMessageKeys.REQUESTER_INVALID);
        }
        return requester;
    }

    private void requireValidSettings(JsonNode settings) {
        if (settings == null || !settings.isObject())
            throw new ApiException(OnboardingMessageKeys.SETTINGS_INVALID);
        JsonNode orientation = settings.get("orientation");
        if (orientation != null && !orientation.isNull() && !orientation.isString()) {
            throw new ApiException(OnboardingMessageKeys.SETTINGS_INVALID);
        }
    }
}
