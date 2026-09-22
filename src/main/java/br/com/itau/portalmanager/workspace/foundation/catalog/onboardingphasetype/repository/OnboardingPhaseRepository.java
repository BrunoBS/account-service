package br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhase;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingPhaseRepository extends CatalogRepository<OnboardingPhase> {
}
