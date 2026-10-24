package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ApplicationNormalizerTest {

    private final ApplicationNormalizer normalizer = new ApplicationNormalizer();

    @Test
    void leavesNonObjectPayloadsUntouched() {
        JsonNode payload = JsonNodeFactory.instance.arrayNode().add("value");

        assertSame(payload, normalizer.normalizePayload(payload));
    }

    @Test
    void trimsTextAndNormalizesCodesAndAuthorizerGroups() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode()
                .put("name", " Application ")
                .put("alias", " alias ")
                .put("description", " description ")
                .put("acronym", " app ")
                .put("applicationScope", " backend ")
                .put("authorizerGroup", " team-a ");

        JsonNode normalized = normalizer.normalizePayload(payload);

        assertEquals("Application", normalized.get("name").asString());
        assertEquals("alias", normalized.get("alias").asString());
        assertEquals("description", normalized.get("description").asString());
        assertEquals("app", normalized.get("acronym").asString());
        assertEquals("BACKEND", normalized.get("applicationScope").asString());
        assertEquals("A-TEAM-A", normalized.get("authorizerGroup").asString());
        assertEquals(" Application ", payload.get("name").asString());
    }

    @Test
    void nullsBlankAuthorizerGroupAndPreservesNonStringValues() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode()
                .put("name", 42)
                .put("applicationScope", 10)
                .put("authorizerGroup", "   ");

        JsonNode normalized = normalizer.normalizePayload(payload);

        assertEquals(42, normalized.get("name").asInt());
        assertEquals(10, normalized.get("applicationScope").asInt());
        assertTrue(normalized.get("authorizerGroup").isNull());

        ObjectNode nonStringGroup = JsonNodeFactory.instance.objectNode().put("authorizerGroup", 42);
        assertEquals(
                42,
                normalizer.normalizePayload(nonStringGroup).get("authorizerGroup").asInt());
    }

    @Test
    void keepsAlreadyPrefixedAuthorizerGroupsAndNormalizesOptionalTags() {
        ObjectNode payload = JsonNodeFactory.instance.objectNode().put("authorizerGroup", " a-team-a ");

        assertEquals("A-TEAM-A", normalizer.normalizePayload(payload).get("authorizerGroup").asString());
        assertNull(normalizer.normalizeTag(null));
        assertNull(normalizer.normalizeTag("   "));
        assertEquals("my-tag", normalizer.normalizeTag(" My Tag "));
    }

    @Test
    void preservesAnAbsentAuthorizerGroup() {
        JsonNode normalized = normalizer.normalizePayload(JsonNodeFactory.instance.objectNode());

        assertFalse(normalized.has("authorizerGroup"));
    }
}
