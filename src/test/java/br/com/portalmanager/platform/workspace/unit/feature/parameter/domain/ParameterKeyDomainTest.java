package br.com.portalmanager.platform.workspace.unit.feature.parameter.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.parameter.domain.ParameterKey;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyTypeCode;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;

class ParameterKeyDomainTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 18, 0);
    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void startsActiveAndDraftWithImmutableIdentityAndNoPublication() {
        ParameterKey resource = create();
        String identifier = resource.getIdentifier();
        assertThat(java.util.UUID.fromString(identifier)).isNotNull();
        assertThat(resource.getStatus().value()).isEqualTo("DRAFT");
        assertThat(resource.isActive()).isTrue();
        assertThat(resource.getPublishedValue()).isNull();
        assertThat(resource.getPublishedType()).isNull();
        resource.update(
                "Changed",
                ParameterKeyTypeCode.configuration(),
                mapper.readTree("[1,2]"),
                NOW.plusSeconds(1));
        resource.inactivate(NOW.plusSeconds(2));
        resource.activate(NOW.plusSeconds(3));
        assertThat(resource.getIdentifier()).isEqualTo(identifier);
        assertThat(resource.getKey()).isEqualTo("retry.attempts");
        assertThat(resource.getMicroserviceId()).isEqualTo(10L);
        assertThat(resource.getCreatedAt()).isEqualTo(NOW);
        assertThat(resource.getPublishedValue()).isNull();
    }

    @Test
    void changingTypeOrValuePreservesThePublishedTypeAndValueUntilNewPublication() {
        ParameterKey resource = create();
        resource.publish(NOW.plusSeconds(1));
        assertThat(resource.getStatus().value()).isEqualTo("PUBLISHED");
        resource.update(
                "Description only",
                ParameterKeyTypeCode.configuration(),
                mapper.readTree("{\"attempts\":3}"),
                NOW.plusSeconds(2));
        assertThat(resource.getStatus().value()).isEqualTo("PUBLISHED");
        resource.update(
                "New rule",
                ParameterKeyTypeCode.expression(),
                mapper.readTree("{\"operator\":\"EQUALS\",\"valueType\":\"LONG\",\"value\":10}"),
                NOW.plusSeconds(3));
        assertThat(resource.getStatus().value()).isEqualTo("DRAFT");
        assertThat(resource.getPublishedType()).isEqualTo(ParameterKeyTypeCode.configuration());
        assertThat(resource.getPublishedValue()).isEqualTo(mapper.readTree("{\"attempts\":3}"));
        resource.publish(NOW.plusSeconds(4));
        assertThat(resource.getPublishedType()).isEqualTo(ParameterKeyTypeCode.expression());
        assertThat(resource.getPublishedValue()).isEqualTo(resource.getValue());
        assertThatThrownBy(() -> resource.publish(NOW.plusSeconds(5)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void snapshotsDoNotShareMutableJsonWithInputsOrGetters() {
        ObjectNode input = mapper.createObjectNode().put("attempts", 3);
        ParameterKey resource =
                new ParameterKey(
                        10L, "retry", null, ParameterKeyTypeCode.configuration(), input, NOW);
        input.put("attempts", 99);
        resource.publish(NOW);
        ((ObjectNode) resource.getValue()).put("attempts", 100);
        ((ObjectNode) resource.getPublishedValue()).put("attempts", 101);
        assertThat(resource.getValue().get("attempts").asInt()).isEqualTo(3);
        assertThat(resource.getPublishedValue().get("attempts").asInt()).isEqualTo(3);
    }

    @Test
    void transitionsAreIdempotentAndInactivationClearsTheEntirePublication() {
        ParameterKey resource = create();
        assertThatThrownBy(resource::requireDeletable).isInstanceOf(ValidationException.class);
        resource.activate(NOW.plusSeconds(1));
        assertThat(resource.getUpdatedAt()).isEqualTo(NOW);
        resource.publish(NOW.plusSeconds(2));
        resource.inactivate(NOW.plusSeconds(3));
        resource.inactivate(NOW.plusSeconds(4));
        assertThat(resource.getUpdatedAt()).isEqualTo(NOW.plusSeconds(3));
        assertThat(resource.getPublishedValue()).isNull();
        assertThat(resource.getPublishedType()).isNull();
        assertThat(resource.getStatus().value()).isEqualTo("DRAFT");
        resource.requireDeletable();
        assertThatThrownBy(() -> resource.publish(NOW.plusSeconds(5)))
                .isInstanceOf(ValidationException.class);
        resource.activate(NOW.plusSeconds(6));
        assertThat(resource.getPublishedValue()).isNull();
        assertThat(resource.getPublishedType()).isNull();
    }

    @Test
    void jsonNullIsAValidPresentValueAndDiffersFromMissingValue() {
        ParameterKey resource =
                new ParameterKey(
                        10L,
                        "nullable",
                        null,
                        ParameterKeyTypeCode.configuration(),
                        NullNode.getInstance(),
                        NOW);
        resource.publish(NOW);
        assertThat(resource.getPublishedValue().isNull()).isTrue();
        assertThatThrownBy(
                        () ->
                                new ParameterKey(
                                        10L,
                                        "missing",
                                        null,
                                        ParameterKeyTypeCode.configuration(),
                                        null,
                                        NOW))
                .isInstanceOf(ValidationException.class);
    }

    private ParameterKey create() {
        return new ParameterKey(
                10L,
                "retry.attempts",
                null,
                ParameterKeyTypeCode.configuration(),
                mapper.readTree("{\"attempts\":3}"),
                NOW);
    }
}
