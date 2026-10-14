package br.com.portalmanager.platform.workspace.feature.platform.domain.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;

import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ContextDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 15, 0);

    @Test
    void shouldCreateActiveContextWithFriendlyNameAndValidateCode() {
        Context context = new Context("manager-account", "Manager Account", "Manager account", NOW);

        assertThat(context.isActive()).isTrue();
        assertThat(context.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
    }

    @Test
    void shouldTransitionLifecycle() {
        Context context = new Context("manager-account", "manager-account", null, NOW);

        context.inactivate(NOW.plusMinutes(1));
        assertThat(context.isActive()).isFalse();

        context.activate(NOW.plusMinutes(2));
        assertThat(context.isActive()).isTrue();

        context.inactivate(NOW.plusMinutes(3));
        assertThat(context.getLifecycle()).isEqualTo(LifecycleTypeCode.inactive());
    }
    @Test
    void activeContextsCannotBeActivatedAgainAndInactiveOnesCannotBeUpdated() {
        Context context = new Context("manager-account", "Manager", null, NOW);
        assertThatThrownBy(() -> context.activate(NOW)).isInstanceOf(ValidationException.class);
        context.inactivate(NOW.plusSeconds(1));
        assertThatThrownBy(() -> context.update("Changed", null, NOW)).isInstanceOf(NotFoundException.class);
        context.activate(NOW.plusSeconds(2));
        assertThat(context.isActive()).isTrue();
        assertThat(context.getName()).isEqualTo("Manager");
    }
}
