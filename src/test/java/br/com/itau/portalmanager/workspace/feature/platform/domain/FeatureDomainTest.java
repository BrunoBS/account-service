package br.com.itau.portalmanager.workspace.feature.platform.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 30);

    @Test
    void shouldRequireActiveServiceOnCreation() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);
        service.inactivate(NOW.plusMinutes(1));

        assertThatThrownBy(() -> new Feature("AUDIT", "Audit", null, service, "{}", NOW.plusMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature requires an active service");
    }

    @Test
    void shouldRequireActiveServiceOnActivation() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("AUDIT", "Audit", null, service, "{}", NOW);
        feature.inactivate(NOW.plusMinutes(1));
        service.inactivate(NOW.plusMinutes(2));

        assertThatThrownBy(() -> feature.activate(NOW.plusMinutes(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Feature requires an active service");
    }

    @Test
    void shouldRequireActiveScopeAssociation() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("AUDIT", "Audit", null, service, "{}", NOW);
        FeatureScopeType scope = new FeatureScopeType();
        scope.setCode("ADMINISTRATION");
        scope.setActive(false);

        assertThatThrownBy(() -> feature.addScope(scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature scope must be active");
    }

    @Test
    void shouldAssociateAndRemoveActiveScope() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("AUDIT", "Audit", null, service, "{}", NOW);
        FeatureScopeType scope = new FeatureScopeType();
        scope.setCode("ADMINISTRATION");
        scope.setActive(true);

        feature.addScope(scope);
        assertThat(feature.getScopes()).containsExactly(scope);

        feature.removeScope(scope);
        assertThat(feature.getScopes()).isEmpty();
    }

    @Test
    void shouldMoveFeatureBetweenActiveServicesMaintainingBidirectionalRelation() {
        Service original = new Service("workspace-service", "Workspace Service", null, NOW);
        Service target = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("AUDIT", "Audit", null, original, "{}", NOW);

        feature.changeService(target, NOW.plusMinutes(1));

        assertThat(original.getFeatures()).doesNotContain(feature);
        assertThat(target.getFeatures()).containsExactly(feature);
        assertThat(feature.getService()).isSameAs(target);
    }
}
