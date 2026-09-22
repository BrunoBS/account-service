package br.com.itau.portalmanager.workspace.core.workspace.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemanticCodeTest {

    @Test
    void shouldValidateSharedSemanticCodeFormat() {
        assertThat(SemanticCode.isValid("ACTIVE")).isTrue();
        assertThat(SemanticCode.isValid("PENDING_DELETION")).isTrue();
        assertThat(SemanticCode.isValid("workspace")).isFalse();
        assertThat(SemanticCode.isValid(null)).isFalse();
    }

    @Test
    void shouldKeepDomainSpecificFactoriesAndTypes() {
        assertThat(LifecycleTypeCode.active().value()).isEqualTo("ACTIVE");
        assertThat(WorkspaceTypeCode.of("ADMIN").value()).isEqualTo("ADMIN");

        assertThatThrownBy(() -> LifecycleTypeCode.of("active"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> WorkspaceTypeCode.of("admin"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
