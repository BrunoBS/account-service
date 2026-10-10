package br.com.portalmanager.platform.workspace.feature.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FeatureDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 30);

    @Test
    void shouldAssociateAndRemoveActiveContext() {
        Microservice microservice = new Microservice("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("audit", "audit", null, microservice, "{}", NOW);
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
        Microservice original = new Microservice("workspace-service", "Workspace Service", null, NOW);
        Microservice target = new Microservice("audit-service", "Audit Service", null, NOW);
        Feature feature = new Feature("audit", "audit", null, original, "{}", NOW);

        feature.changeMicroservice(target, NOW.plusMinutes(1));

        assertThat(original.getFeatures()).doesNotContain(feature);
        assertThat(target.getFeatures()).containsExactly(feature);
        assertThat(feature.getMicroservice()).isSameAs(target);
    }
}
