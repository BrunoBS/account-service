package br.com.portalmanager.account.application;

import br.com.portalmanager.account.application.model.ApproverCommand;
import br.com.portalmanager.account.application.model.CreateAccountCommand;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccountNormalizerTest {

    private final AccountNormalizer normalizer = new AccountNormalizer();

    @Test
    void shouldNormalizeBusinessFieldsAndPreserveNullAuthorizerGroup() {
        var normalized = normalizer.normalize(new CreateAccountCommand(
                " manager ",
                "  Conta Normalizada  ",
                "  Descrição normalizada  ",
                "  requester  ",
                " ABC ",
                "   ",
                "{\"feature\":true}",
                " group@portalmanager.com ",
                List.of(new ApproverCommand(
                        " F1234 ",
                        " approver@portalmanager.com "
                )),
                List.of(" Manual Tag ")
        ));

        assertThat(normalized.accountType()).isEqualTo("MANAGER");
        assertThat(normalized.name()).isEqualTo("Conta Normalizada");
        assertThat(normalized.description()).isEqualTo("Descrição normalizada");
        assertThat(normalized.requester()).isEqualTo("requester");
        assertThat(normalized.acronym()).isEqualTo("ABC");
        assertThat(normalized.authorizerGroup()).isNull();
        assertThat(normalized.emailGroup()).isEqualTo("group@portalmanager.com");
        assertThat(normalized.approvers()).containsExactly(
                new ApproverCommand("F1234", "approver@portalmanager.com")
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
