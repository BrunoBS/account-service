package br.com.itau.portalmanager.workspace.core.workspace.usecase.support;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.create.CreateWorkspaceInput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.ApproverInput;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceNormalizerTest {

    private final WorkspaceNormalizer normalizer = new WorkspaceNormalizer();

    @Test
    void shouldNormalizeBusinessFieldsAndPreserveNullAuthorizerGroup() {
        var normalized = normalizer.normalize(new CreateWorkspaceInput(
                " manager ",
                "  Workspace Normalizado  ",
                "  Descrição normalizada  ",
                "  requester  ",
                " ABC ",
                "   ",
                "{\"feature\":true}",
                " group@portalmanager.com ",
                List.of(new ApproverInput(
                        " F1234 ",
                        " approver@portalmanager.com "
                )),
                List.of(" Manual Tag ")
        ));

        assertThat(normalized.workspaceType()).isEqualTo("MANAGER");
        assertThat(normalized.name()).isEqualTo("Workspace Normalizado");
        assertThat(normalized.description()).isEqualTo("Descrição normalizada");
        assertThat(normalized.requester()).isEqualTo("requester");
        assertThat(normalized.acronym()).isEqualTo("ABC");
        assertThat(normalized.authorizerGroup()).isNull();
        assertThat(normalized.emailGroup()).isEqualTo("group@portalmanager.com");
        assertThat(normalized.approvers()).containsExactly(
                new ApproverInput("F1234", "approver@portalmanager.com")
        );
        assertThat(normalized.tags()).containsExactly(" Manual Tag ");
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
