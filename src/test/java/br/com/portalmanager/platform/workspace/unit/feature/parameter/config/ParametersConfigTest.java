package br.com.portalmanager.platform.workspace.unit.feature.parameter.config;


import br.com.portalmanager.platform.workspace.feature.parameter.config.ParameterProperties;
import br.com.portalmanager.platform.workspace.feature.parameter.config.ParametersConfig;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.testing.lifecycle.PlatformContextRunners;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.time.Duration;

class ParametersConfigTest {
    private final ApplicationContextRunner runner =
            PlatformContextRunners.forUserConfiguration(ParametersConfig.class);

    @Test
    void runtimeAndHourlySchedulingAreEnabledByDefault() {
        runner.run(
                context -> {
                    assertThat(context)
                            .hasSingleBean(ParameterProperties.class)
                            .hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
                    ParameterProperties properties = context.getBean(ParameterProperties.class);
                    assertThat(properties.runtime().enabled()).isTrue();
                    assertThat(properties.runtime().batchSize()).isEqualTo(500);
                    assertThat(properties.reconciliation().enabled()).isTrue();
                    assertThat(properties.reconciliation().interval())
                            .isEqualTo(Duration.ofHours(1));
                });
    }

    @Test
    void schedulerCanBeDisabled() {
        runner.withPropertyValues("workspace.parameter.reconciliation.enabled=false")
                .run(
                        context -> {
                            assertThat(context)
                                    .doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
                            assertThat(
                                            context.getBean(ParameterProperties.class)
                                                    .runtime()
                                                    .enabled())
                                    .isTrue();
                        });
    }

    @Test
    void invalidSettingsFailStartupWithPlatformConfigurationException() {
        runner.withPropertyValues("workspace.parameter.runtime.batch-size=0")
                .run(
                        context ->
                                assertThat(context.getStartupFailure())
                                        .hasRootCauseInstanceOf(
                                                PlatformConfigurationException.class));
    }
}
