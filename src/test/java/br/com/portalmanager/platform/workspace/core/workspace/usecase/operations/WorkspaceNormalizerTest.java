package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class WorkspaceNormalizerTest {

    private final WorkspaceNormalizer normalizer = new WorkspaceNormalizer();
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldNormalizeTextBeforeSchemaValidationWithoutChangingTheOriginalPayload() {
        JsonNode payload = objectMapper.readTree(
            """
            {"workspaceType":" manager ","name":" Workspace ","description":" Descrição válida ",
             "requester":" requester ","acronym":" ABC ","authorizerGroup":" team-a ",
             "emailGroup":" group@example.com ","settings":{"value":" preserve spaces "},
             "approvers":[{"functional":" F1234 ","email":" approver@example.com "}],
             "tags":[" Manual Tag "]}
            """
        );
        JsonNode normalized = normalizer.normalizePayload(payload);
        assertThat(normalized.get("workspaceType").asString()).isEqualTo("MANAGER");
        assertThat(normalized.get("name").asString()).isEqualTo("Workspace");
        assertThat(normalized.get("description").asString()).isEqualTo("Descrição válida");
        assertThat(normalized.get("requester").asString()).isEqualTo("requester");
        assertThat(normalized.get("acronym").asString()).isEqualTo("ABC");
        assertThat(normalized.get("authorizerGroup").asString()).isEqualTo("TEAM-A");
        assertThat(normalized.get("emailGroup").asString()).isEqualTo("group@example.com");
        assertThat(normalized.at("/approvers/0/functional").asString()).isEqualTo("F1234");
        assertThat(normalized.at("/approvers/0/email").asString()).isEqualTo("approver@example.com");
        assertThat(normalized.at("/settings/value").asString()).isEqualTo(" preserve spaces ");
        assertThat(normalized.at("/tags/0").asString()).isEqualTo(" Manual Tag ");
        assertThat(payload.get("name").asString()).isEqualTo(" Workspace ");
    }

    @Test
    void shouldPreserveInvalidTypesAndUnknownPropertiesForSchemaValidation() {
        JsonNode payload = objectMapper.readTree(
            """
            {"name":123,"workspaceType":false,"authorizerGroup":[],"approvers":[null,42,{"functional":false}],"unexpected":true}
            """
        );
        assertThat(normalizer.normalizePayload(payload)).isEqualTo(payload);
        JsonNode arrayPayload = objectMapper.readTree("[]");
        assertThat(normalizer.normalizePayload(arrayPayload)).isSameAs(arrayPayload);
        assertThat(normalizer.normalizePayload(null)).isNull();
    }

    @Test
    void shouldNormalizeBlankAuthorizerGroupToNullAndKeepMissingFieldsAbsent() {
        JsonNode normalized = normalizer.normalizePayload(objectMapper.readTree("{\"authorizerGroup\":\"   \"}"));
        assertThat(normalized.get("authorizerGroup").isNull()).isTrue();
        assertThat(normalized.has("name")).isFalse();
    }

    @Test
    void shouldIgnoreBlankTypeFilter() {
        assertThat(normalizer.normalizeTypeFilter("   ")).isNull();
    }

    @Test
    void shouldDelegateTagFilterNormalizationToFoundation() {
        assertThat(normalizer.normalizeTagFilter("  Minha   Tag  ")).isEqualTo("minha-tag");
    }
}
