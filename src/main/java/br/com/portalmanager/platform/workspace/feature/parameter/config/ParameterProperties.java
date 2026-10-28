package br.com.portalmanager.platform.workspace.feature.parameter.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties("workspace.parameter")
public record ParameterProperties(
        @DefaultValue Runtime runtime, @DefaultValue Reconciliation reconciliation) {

    public record Runtime(
            @DefaultValue("true") boolean enabled, @DefaultValue("500") int batchSize,
            @DefaultValue("PT0.1S") Duration retryDelay) {

        public Runtime {
            if (batchSize < 1 || retryDelay == null || retryDelay.isNegative()
                    || retryDelay.compareTo(Duration.ofSeconds(10)) > 0)
                throw new PlatformConfigurationException(
                        new PlatformErrorDefinition(
                                "PARAMETER-KEY-0502",
                                "Invalid parameter runtime settings.",
                                "Use a positive batch size and a retry delay between 0 and 10 seconds.",
                                500));
        }
    }

    public record Reconciliation(
            @DefaultValue("true") boolean enabled, @DefaultValue("PT1H") Duration interval) {

        public Reconciliation {
            if (interval == null
                    || interval.isNegative()
                    || interval.compareTo(Duration.ofMillis(1)) < 0)
                throw new PlatformConfigurationException(
                        new PlatformErrorDefinition(
                                "PARAMETER-KEY-0502",
                                "Invalid parameter reconciliation settings.",
                                "Use a positive interval and batch size.",
                                500));
        }
    }
}
