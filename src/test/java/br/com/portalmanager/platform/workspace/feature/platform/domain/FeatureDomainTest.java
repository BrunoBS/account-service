package br.com.portalmanager.platform.workspace.feature.platform.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FeatureDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 30);

    @Test
    void shouldAssociateAndRemoveActiveContext() {
        Service service = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("audit", "audit", null, service, "{}", NOW);
        FeatureContext context = new FeatureContext("administration", "administration", null, NOW);

        feature.addContext(context);
        assertThat(feature.getContexts()).containsExactly(context);
        assertThat(context.getFeatures()).containsExactly(feature);

        feature.removeContext(context);
        assertThat(feature.getContexts()).isEmpty();
        assertThat(context.getFeatures()).isEmpty();
    }

    @Test
    void shouldMoveFeatureBetweenActiveServicesMaintainingBidirectionalRelation() {
        Service original = new Service("workspace-service", "Workspace Service", null, NOW);
        Service target = new Service("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("audit", "audit", null, original, "{}", NOW);

        feature.changeService(target, NOW.plusMinutes(1));

        assertThat(original.getFeatures()).doesNotContain(feature);
        assertThat(target.getFeatures()).containsExactly(feature);
        assertThat(feature.getService()).isSameAs(target);
    }
}
