package br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.repository;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.catalog.repository.CatalogRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OnboardingPhaseTypeTypeRepository extends CatalogRepository<OnboardingPhaseType> {
}
