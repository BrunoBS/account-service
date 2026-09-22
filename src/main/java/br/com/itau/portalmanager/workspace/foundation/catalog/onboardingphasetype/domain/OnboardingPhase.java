package br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain;

import br.com.portalmanager.platform.catalog.model.CatalogEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "type_onboardings")
public class OnboardingPhase extends CatalogEntity {
}
