package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkspaceNormalizerTest {

    private final WorkspaceNormalizer normalizer = new WorkspaceNormalizer();

    @Test
    void shouldNormalizeBusinessFieldsAndPreserveNullAuthorizerGroup() {
        CreateWorkspaceInput normalized = normalizer.normalize(
            new CreateWorkspaceInput(
                " manager ",
                "  Workspace Normalizado  ",
                "  Descrição normalizada  ",
                "  requester  ",
                " ABC ",
                "   ",
                "{}",
                " group@portalmanager.com ",
                List.of(new ApproverInput(" F1234 ", " approver@portalmanager.com ")),
                List.of(" Manual Tag ")
            )
        );

        assertThat(normalized.workspaceType()).isEqualTo("MANAGER");
        assertThat(normalized.name()).isEqualTo("Workspace Normalizado");
        assertThat(normalized.description()).isEqualTo("Descrição normalizada");
        assertThat(normalized.requester()).isEqualTo("requester");
        assertThat(normalized.acronym()).isEqualTo("ABC");
        assertThat(normalized.authorizerGroup()).isNull();
        assertThat(normalized.emailGroup()).isEqualTo("group@portalmanager.com");
        assertThat(normalized.approvers()).containsExactly(new ApproverInput("F1234", "approver@portalmanager.com"));
        assertThat(normalized.tags()).containsExactly(" Manual Tag ");
    }

    @Test
    void shouldNormalizeAuthorizerGroupToUppercaseOnCreate() {
        CreateWorkspaceInput normalized = normalizer.normalize(
            new CreateWorkspaceInput(
                "MANAGER",
                "Workspace",
                "Descrição",
                "requester",
                "ABC",
                "  bbs-app  ",
                "{}",
                "group@portalmanager.com",
                List.of(),
                List.of()
            )
        );

        assertThat(normalized.authorizerGroup()).isEqualTo("BBS-APP");
    }

    @Test
    void shouldNormalizeAuthorizerGroupToUppercaseOnUpdate() {
        UpdateWorkspaceInput normalized = normalizer.normalize(
            new UpdateWorkspaceInput(
                1L,
                "MANAGER",
                "Workspace",
                "Descrição",
                "requester",
                "ABC",
                "  catalog-team  ",
                "{}",
                "group@portalmanager.com",
                List.of(),
                List.of()
            )
        );

        assertThat(normalized.authorizerGroup()).isEqualTo("CATALOG-TEAM");
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
