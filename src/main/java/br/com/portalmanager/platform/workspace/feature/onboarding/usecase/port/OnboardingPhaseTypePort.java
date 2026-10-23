package br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;

import java.util.List;

public interface OnboardingPhaseTypePort {
    boolean existsActive(String code);

    List<CatalogDTO> findByCodes(List<String> codes);
}
