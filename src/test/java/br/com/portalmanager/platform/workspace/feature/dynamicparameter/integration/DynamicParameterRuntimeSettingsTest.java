package br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterRuntimeProperties;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.runtime.DynamicParameterRuntimeSettings;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.integration.scheduler.DynamicParameterReconciliationScheduler;
import br.com.portalmanager.platform.workspace.feature.dynamicparameter.usecase.operations.runtime.DynamicParameterRuntimeService;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

class DynamicParameterRuntimeSettingsTest {
    private final DynamicParameterRuntimeService service =
            mock(DynamicParameterRuntimeService.class);
    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withUserConfiguration(DynamicParameterRuntimeSettings.class)
                    .withBean(DynamicParameterRuntimeService.class, () -> service);

    @Test
    void runtimeAndHourlySchedulingAreEnabledByDefault() {
        runner.run(
                context -> {
                    assertThat(context)
                            .hasSingleBean(DynamicParameterRuntimeProperties.class)
                            .hasSingleBean(DynamicParameterReconciliationScheduler.class);
                    var properties = context.getBean(DynamicParameterRuntimeProperties.class);
                    assertThat(properties.runtime().enabled()).isTrue();
                    assertThat(properties.runtime().batchSize()).isEqualTo(500);
                    assertThat(properties.reconciliation().enabled()).isTrue();
                    assertThat(properties.reconciliation().interval())
                            .isEqualTo(Duration.ofHours(1));
                    verifyNoInteractions(service);
                });
    }

    @Test
    void schedulerCanBeDisabled() {
        runner.withPropertyValues("platform.dynamic-parameter.reconciliation.enabled=false")
                .run(
                        context -> {
                            assertThat(context)
                                    .doesNotHaveBean(DynamicParameterReconciliationScheduler.class);
                            assertThat(
                                            context.getBean(DynamicParameterRuntimeProperties.class)
                                                    .runtime()
                                                    .enabled())
                                    .isTrue();
                            verifyNoInteractions(service);
                        });
    }

    @Test
    void invalidSettingsFailStartupWithPlatformConfigurationException() {
        runner.withPropertyValues("platform.dynamic-parameter.runtime.batch-size=0")
                .run(
                        context ->
                                assertThat(context.getStartupFailure())
                                        .hasRootCauseInstanceOf(
                                                br.com.portalmanager.platform.library.messaging
                                                        .exception.PlatformConfigurationException
                                                        .class));
    }
}
