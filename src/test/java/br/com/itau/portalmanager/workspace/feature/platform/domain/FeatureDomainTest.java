package br.com.itau.portalmanager.workspace.feature.platform.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 30);

    @Test
    void shouldRequireActiveServiceOnCreation() {
        Service service = new Service("audit-service", "AUDIT_SERVICE", null, NOW);
        service.inactivate(NOW.plusMinutes(1));

        assertThatThrownBy(() -> new Feature("AUDIT", "AUDIT", null, service, "{}", NOW.plusMinutes(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature requires an active service");
    }

    @Test
    void shouldRequireActiveServiceOnActivation() {
        Service service = new Service("audit-service", "AUDIT_SERVICE", null, NOW);
        Feature feature = new Feature("AUDIT", "AUDIT", null, service, "{}", NOW);
        feature.inactivate(NOW.plusMinutes(1));
        service.inactivate(NOW.plusMinutes(2));

        assertThatThrownBy(() -> feature.activate(NOW.plusMinutes(3)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Feature requires an active service");
    }

    @Test
    void shouldRequireActiveContextAssociation() {
        Service service = new Service("audit-service", "AUDIT_SERVICE", null, NOW);
        Feature feature = new Feature("AUDIT", "AUDIT", null, service, "{}", NOW);
        FeatureContext context = new FeatureContext("ADMINISTRATION", "ADMINISTRATION", null, NOW);
        context.inactivate(NOW.plusMinutes(1));

        assertThatThrownBy(() -> feature.addContext(context))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Feature context must be active");
    }

    @Test
    void shouldAssociateAndRemoveActiveContext() {
        Service service = new Service("audit-service", "AUDIT_SERVICE", null, NOW);
        Feature feature = new Feature("AUDIT", "AUDIT", null, service, "{}", NOW);
        FeatureContext context = new FeatureContext("ADMINISTRATION", "ADMINISTRATION", null, NOW);

        feature.addContext(context);
        assertThat(feature.getContexts()).containsExactly(context);
        assertThat(context.getFeatures()).containsExactly(feature);

        feature.removeContext(context);
        assertThat(feature.getContexts()).isEmpty();
        assertThat(context.getFeatures()).isEmpty();
    }

    @Test
    void shouldRejectInvalidPlatformName() {
        Service service = new Service("audit-service", "AUDIT_SERVICE", null, NOW);

        assertThatThrownBy(() -> new Feature("AUDIT", "Audit Feature", null, service, "{}", NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldMoveFeatureBetweenActiveServicesMaintainingBidirectionalRelation() {
        Service original = new Service("workspace-service", "WORKSPACE_SERVICE", null, NOW);
        Service target = new Service("audit-service", "AUDIT_SERVICE", null, NOW);
        Feature feature = new Feature("AUDIT", "AUDIT", null, original, "{}", NOW);

        feature.changeService(target, NOW.plusMinutes(1));

        assertThat(original.getFeatures()).doesNotContain(feature);
        assertThat(target.getFeatures()).containsExactly(feature);
        assertThat(feature.getService()).isSameAs(target);
    }
}
