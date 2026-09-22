package br.com.itau.portalmanager.workspace.foundation.catalog.domain.onboarding;

import br.com.portalmanager.platform.catalog.model.BaseCatalogEntity;
import jakarta.persistence.*;

@Entity
@Table(
        name = "type_onboardings",
        uniqueConstraints = @UniqueConstraint(name = "uk_type_onboardings_name", columnNames = "name")
)
public class OnboardingPhase extends BaseCatalogEntity {

    @Column(name = "orientation", nullable = false, length = 255)
    private String orientation;

    public String getOrientation() { return orientation; }
    public void setOrientation(String orientation) { this.orientation = orientation; }
}
