package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import jakarta.persistence.Embeddable;

@Embeddable
public class OnboardingPhaseTypeCode extends AbstractCatalogCode {
    protected OnboardingPhaseTypeCode() {}

    private OnboardingPhaseTypeCode(String value) { super(value); }

    public static OnboardingPhaseTypeCode of(String value) { return new OnboardingPhaseTypeCode(value); }

    public static OnboardingPhaseTypeCode of(OnboardingPhaseTypeEnum value) { return of(value.name()); }
}
