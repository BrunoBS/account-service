package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain;

import br.com.portalmanager.platform.library.catalog.api.contract.model.CatalogEnum;

public enum OnboardingPhaseTypeEnum implements CatalogEnum<OnboardingPhaseTypeEnum> {
        WORKSPACE_REGISTRATION,
        WORKSPACE_FIRST_ENVIRONMENT,
        FIRST_APPLICATION_REGISTRATION,
        APPLICATION_FIRST_ENVIRONMENT,
}
