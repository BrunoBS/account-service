package br.com.itau.portalmanager.workspace.feature.platform.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeatureContextDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 15, 0);

    @Test
    void shouldCreateActiveContextAndValidateName() {
        FeatureContext context = new FeatureContext(
                "MANAGER_ACCOUNT",
                "MANAGER_ACCOUNT",
                "Manager account",
                NOW
        );

        assertThat(context.isActive()).isTrue();
        assertThat(context.getLifecycle()).isEqualTo(LifecycleTypeCode.active());

        assertThatThrownBy(() -> new FeatureContext(
                "INVALID",
                "Manager Account",
                null,
                NOW
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldTransitionLifecycle() {
        FeatureContext context = new FeatureContext("MANAGER_ACCOUNT", "MANAGER_ACCOUNT", null, NOW);

        context.inactivate(NOW.plusMinutes(1));
        assertThat(context.isActive()).isFalse();

        context.activate(NOW.plusMinutes(2));
        assertThat(context.isActive()).isTrue();

        context.quarantine(NOW.plusMinutes(3));
        assertThat(context.getLifecycle()).isEqualTo(LifecycleTypeCode.quarantined());
    }
}
