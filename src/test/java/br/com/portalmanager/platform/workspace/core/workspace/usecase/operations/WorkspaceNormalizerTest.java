package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceNormalizerTest {

    private final WorkspaceNormalizer normalizer = new WorkspaceNormalizer();

    @Test
    void shouldNormalizeTypeFilter() {
        assertThat(normalizer.normalizeTypeFilter(" manager ")).isEqualTo("MANAGER");
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
