package br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class FeatureGroupDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 15, 0);

    @Test
    void shouldCreateActiveGroupWithFriendlyNameAndValidateCode() {
        FeatureGroup group = new FeatureGroup("manager-account", "Manager Account", "Manager account", NOW);

        assertThat(group.isActive()).isTrue();
        assertThat(group.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
    }

    @Test
    void shouldTransitionLifecycle() {
        FeatureGroup group = new FeatureGroup("manager-account", "manager-account", null, NOW);

        group.inactivate(NOW.plusMinutes(1));
        assertThat(group.isActive()).isFalse();

        group.activate(NOW.plusMinutes(2));
        assertThat(group.isActive()).isTrue();

        group.inactivate(NOW.plusMinutes(3));
        assertThat(group.getLifecycle()).isEqualTo(LifecycleTypeCode.inactive());
    }

    @Test
    void activeContextsCannotBeActivatedAgainAndInactiveOnesCannotBeUpdated() {
        FeatureGroup group = new FeatureGroup("manager-account", "Manager", null, NOW);
        assertThatThrownBy(() -> group.activate(NOW)).isInstanceOf(ValidationException.class);
        group.inactivate(NOW.plusSeconds(1));
        assertThatThrownBy(() -> group.update("Changed", null, NOW))
                .isInstanceOf(NotFoundException.class);
        group.activate(NOW.plusSeconds(2));
        assertThat(group.isActive()).isTrue();
        assertThat(group.getName()).isEqualTo("Manager");
    }
}
