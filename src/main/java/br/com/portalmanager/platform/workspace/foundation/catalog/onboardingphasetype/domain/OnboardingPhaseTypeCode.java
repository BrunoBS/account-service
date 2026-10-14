package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class OnboardingPhaseTypeCode extends AbstractCatalogCode {

    protected OnboardingPhaseTypeCode() {}

    private OnboardingPhaseTypeCode(String value) {
        super(value);
    }

    public static OnboardingPhaseTypeCode of(String value) {
        return of(requireEnumValue(value, OnboardingPhaseTypeEnum.class));
    }

    public static OnboardingPhaseTypeCode of(OnboardingPhaseTypeEnum value) {
        return new OnboardingPhaseTypeCode(value == null ? null : value.name());
    }

    public static OnboardingPhaseTypeCode workspaceRegistration() {
        return of(OnboardingPhaseTypeEnum.WORKSPACE_REGISTRATION);
    }

    public static OnboardingPhaseTypeCode workspaceFirstEnvironment() {
        return of(OnboardingPhaseTypeEnum.WORKSPACE_FIRST_ENVIRONMENT);
    }

    public static OnboardingPhaseTypeCode firstApplicationRegistration() {
        return of(OnboardingPhaseTypeEnum.FIRST_APPLICATION_REGISTRATION);
    }

    public static OnboardingPhaseTypeCode applicationFirstEnvironment() {
        return of(OnboardingPhaseTypeEnum.APPLICATION_FIRST_ENVIRONMENT);
    }
}
