package br.com.itau.portalmanager.workspace.foundation.catalog.domain.onboarding;

import br.com.portalmanager.platform.catalog.model.CatalogEnum;

public enum OnboardingPhaseEnum implements CatalogEnum<OnboardingPhaseEnum> {
    WORKSPACE_REGISTRATION,
    WORKSPACE_FIRST_ENVIRONMENT,
    FIRST_APPLICATION_REGISTRATION,
    APPLICATION_FIRST_ENVIRONMENT
}
