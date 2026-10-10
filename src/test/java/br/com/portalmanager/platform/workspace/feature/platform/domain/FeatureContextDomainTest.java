package br.com.portalmanager.platform.workspace.feature.platform.domain;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FeatureContextDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 15, 0);

    @Test
    void shouldCreateActiveContextWithFriendlyNameAndValidateCode() {
        FeatureContext context = new FeatureContext("manager-account", "Manager Account", "Manager account", NOW);

        assertThat(context.isActive()).isTrue();
        assertThat(context.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
    }

    @Test
    void shouldTransitionLifecycle() {
        FeatureContext context = new FeatureContext("manager-account", "manager-account", null, NOW);

        context.inactivate(NOW.plusMinutes(1));
        assertThat(context.isActive()).isFalse();

        context.activate(NOW.plusMinutes(2));
        assertThat(context.isActive()).isTrue();

        context.quarantine(NOW.plusMinutes(3));
        assertThat(context.getLifecycle()).isEqualTo(LifecycleTypeCode.quarantined());
    }
}
