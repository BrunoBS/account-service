package br.com.portalmanager.platform.workspace.feature.parameter.config;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.config.WorkspaceConfiguration;
import br.com.portalmanager.platform.workspace.config.WorkspaceProperties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

import java.time.Duration;

class ParametersConfigTest {
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(WorkspaceConfiguration.class, ParametersConfig.class);

    @Test
    void runtimeAndHourlySchedulingAreEnabledByDefault() {
        runner.run(
                context -> {
                    assertThat(context)
                            .hasSingleBean(WorkspaceProperties.class)
                            .hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
                    var properties = context.getBean(WorkspaceProperties.class).parameter();
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
                                            context.getBean(WorkspaceProperties.class)
                                                    .parameter()
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
                                                br.com.portalmanager.platform.library.messaging
                                                        .exception.PlatformConfigurationException
                                                        .class));
    }
}
