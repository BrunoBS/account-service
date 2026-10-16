package br.com.portalmanager.platform.workspace.feature.segmentation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

class SegmentDomainTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);

    @Test
    void newIdentitiesAreUniqueAndRemainUnchangedByUpdatesAndLifecycleTransitions() {
        Segment first = new Segment("mobile-pf", "Mobile", NOW);
        Segment second = new Segment("internet-pf", "Internet", NOW);
        Category category = new Category(first, "premium", "Premium", NOW);
        String identifier = first.getIdentifier();
        String categoryIdentifier = category.getIdentifier();
        assertThat(UUID.fromString(identifier))
                .isNotEqualTo(UUID.fromString(second.getIdentifier()));
        assertThat(UUID.fromString(categoryIdentifier)).isNotNull();
        first.update("Mobile PF", NOW.plusSeconds(1));
        first.inactivate(NOW.plusSeconds(2));
        category.update("Premium PF", NOW.plusSeconds(3));
        category.inactivate(NOW.plusSeconds(4));
        category.activate(NOW.plusSeconds(5));
        assertThat(first.getIdentifier()).isEqualTo(identifier);
        assertThat(first.getCode()).isEqualTo("mobile-pf");
        assertThat(category.getIdentifier()).isEqualTo(categoryIdentifier);
        assertThat(category.getCode()).isEqualTo("premium");
        assertThat(category.getSegment()).isSameAs(first);
        assertThat(first.getCreatedAt()).isEqualTo(NOW);
        assertThat(category.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void parentTransitionsDoNotCascadeAndEffectiveAvailabilityRequiresBothToBeActive() {
        Segment segment = new Segment("mobile-pf", "Mobile", NOW);
        Category category = new Category(segment, "premium", "Premium", NOW);
        assertThat(category.isAvailable()).isTrue();
        segment.inactivate(NOW);
        assertThat(category.isActive()).isTrue();
        assertThat(category.isAvailable()).isFalse();
        category.inactivate(NOW);
        category.activate(NOW);
        assertThat(category.isActive()).isTrue();
        assertThat(category.isAvailable()).isFalse();
        segment.activate(NOW);
        assertThat(category.isAvailable()).isTrue();
        category.inactivate(NOW);
        assertThat(category.isAvailable()).isFalse();
        assertThat(segment.isActive()).isTrue();
    }

    @Test
    void categoryCreationRequiresAnActiveParentAndRepeatedTransitionsAreRejected() {
        Segment segment = new Segment("mobile-pf", "Mobile", NOW);
        Category category = new Category(segment, "premium", "Premium", NOW);
        assertThatThrownBy(() -> segment.activate(NOW)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> category.activate(NOW)).isInstanceOf(ValidationException.class);
        segment.inactivate(NOW);
        assertThatThrownBy(() -> segment.inactivate(NOW)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> new Category(segment, "basic", "Basic", NOW))
                .isInstanceOf(ValidationException.class);
        category.inactivate(NOW);
        assertThatThrownBy(() -> category.inactivate(NOW)).isInstanceOf(ValidationException.class);
        category.activate(NOW);
    }
}
