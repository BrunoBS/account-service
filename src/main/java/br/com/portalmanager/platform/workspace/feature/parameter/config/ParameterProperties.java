package br.com.portalmanager.platform.workspace.feature.parameter.config;

import br.com.portalmanager.platform.library.messaging.exception.PlatformConfigurationException;
import br.com.portalmanager.platform.library.messaging.model.PlatformErrorDefinition;

import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

public record ParameterProperties(
        @DefaultValue Runtime runtime,
        @DefaultValue Reconciliation reconciliation) {

    public record Runtime(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("500") int batchSize) {

        public Runtime {
            if (batchSize < 1)
                throw new PlatformConfigurationException(
                        new PlatformErrorDefinition(
                                "PARAMETER-KEY-0502",
                                "Invalid parameter runtime batch size.",
                                "Use a positive batch size.",
                                500));
        }
    }

    public record Reconciliation(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("PT1H") Duration interval) {

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
