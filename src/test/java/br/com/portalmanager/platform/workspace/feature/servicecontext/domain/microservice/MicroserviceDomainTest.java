package br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class MicroserviceDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 45);

    @Test
    void shouldTransitionLifecycleAndUpdateTimestamp() {
        Microservice microservice =
                new Microservice("audit-service", "Audit Service", null, "{}", NOW);
        assertThat(microservice.isActive()).isTrue();
        assertThat(microservice.getLifecycle()).isEqualTo(LifecycleTypeCode.active());
        microservice.inactivate(NOW.plusMinutes(1));
        assertThat(microservice.isActive()).isFalse();
        microservice.activate(NOW.plusMinutes(2));
        assertThat(microservice.isActive()).isTrue();
        microservice.inactivate(NOW.plusMinutes(3));
        assertThat(microservice.isActive()).isFalse();
        assertThat(microservice.getLifecycle()).isEqualTo(LifecycleTypeCode.inactive());
    }

    @Test
    void activeMicroservicesCannotBeActivatedAgainAndInactiveOnesCannotBeUpdated() {
        Microservice microservice =
                new Microservice("audit-service", "Audit Service", null, "{}", NOW);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> microservice.activate(NOW))
                .isInstanceOf(
                        br.com.portalmanager.platform.library.messaging.exception
                                .ValidationException.class);
        microservice.inactivate(NOW.plusMinutes(1));
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () ->
                                microservice.update(
                                        new MicroserviceDetails("Changed", null, "{}"),
                                        NOW.plusMinutes(2)))
                .isInstanceOf(
                        br.com.portalmanager.platform.library.messaging.exception.NotFoundException
                                .class);
        microservice.activate(NOW.plusMinutes(3));
        assertThat(microservice.isActive()).isTrue();
        assertThat(microservice.getName()).isEqualTo("Audit Service");
    }
}
