package br.com.portalmanager.platform.workspace.unit.feature.servicecontext.domain.feature;


import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.FeatureDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class FeatureDomainTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 25, 13, 30);

    @Test
    void rejectsActivationOfActiveFeaturesAndEditionOfInactiveFeatures() {
        Microservice parent = new Microservice("parent-service", "Parent service", null, NOW);
        Feature feature =
                new Feature("review", new FeatureDetails("Review", null, "{}", true), parent, NOW);
        assertThatThrownBy(() -> feature.activate(NOW.plusMinutes(1)))
                .isInstanceOf(ValidationException.class);
        feature.inactivate(NOW.plusMinutes(1));
        assertThatThrownBy(
                        () ->
                                feature.update(
                                        new FeatureDetails("Changed", null, "{}", false),
                                        parent,
                                        NOW.plusMinutes(3)))
                .isInstanceOf(NotFoundException.class);
        assertThat(feature.getName()).isEqualTo("Review");
        assertThat(feature.isShareable()).isTrue();
        feature.activate(NOW.plusMinutes(4));
        assertThat(feature.isActive()).isTrue();
    }

    @Test
    void updatesFeatureDetailsAndParentInOneDomainOperation() {
        Microservice original = new Microservice("original-service", "Original service", null, NOW);
        Microservice replacement =
                new Microservice("replacement-service", "Replacement service", null, NOW);
        Feature feature =
                new Feature(
                        "review", new FeatureDetails("Review", null, "{}", false), original, NOW);
        feature.update(
                new FeatureDetails("Updated", "Details", "{\"enabled\":true}", true),
                replacement,
                NOW.plusMinutes(1));
        assertThat(feature.getMicroservice()).isSameAs(replacement);
        assertThat(feature.getName()).isEqualTo("Updated");
        assertThat(feature.getDescription()).isEqualTo("Details");
        assertThat(feature.getSettings()).isEqualTo("{\"enabled\":true}");
        assertThat(feature.isShareable()).isTrue();
    }
}
