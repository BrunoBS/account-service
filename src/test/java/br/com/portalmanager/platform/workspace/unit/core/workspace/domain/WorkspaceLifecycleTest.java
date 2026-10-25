package br.com.portalmanager.platform.workspace.unit.core.workspace.domain;


import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WorkspaceLifecycleTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 10, 11, 12, 0);

    @Test
    void shouldRejectInvalidTransitionsWithoutChangingStateOrDate() {
        Workspace workspace = workspace();
        assertThatThrownBy(() -> workspace.restore(now.plusMinutes(1))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> workspace.quarantine(now.plusMinutes(1))).isInstanceOf(ValidationException.class);
        assertThat(workspace.getLifecycle().value()).isEqualTo("ACTIVE");
        assertThat(workspace.getUpdatedAt()).isEqualTo(now);
        workspace.inactivate(now.plusMinutes(2));
        assertThatThrownBy(() -> workspace.inactivate(now.plusMinutes(3))).isInstanceOf(ValidationException.class);
        workspace.quarantine(now.plusMinutes(4));
        assertThatThrownBy(() -> workspace.restore(now.plusMinutes(5))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> workspace.inactivate(now.plusMinutes(5))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> workspace.quarantine(now.plusMinutes(5))).isInstanceOf(ValidationException.class);
        assertThat(workspace.getLifecycle().value()).isEqualTo("QUARANTINED");
        assertThat(workspace.getUpdatedAt()).isEqualTo(now.plusMinutes(4));
    }

    @Test
    void shouldRestoreInactiveWorkspace() {
        Workspace workspace = workspace();
        workspace.inactivate(now.plusMinutes(1));
        workspace.restore(now.plusMinutes(2));
        assertThat(workspace.getLifecycle().value()).isEqualTo("ACTIVE");
        assertThat(workspace.getUpdatedAt()).isEqualTo(now.plusMinutes(2));
    }

    private Workspace workspace() {
        return new Workspace(
            new WorkspaceDetails(
                WorkspaceTypeCode.of("MANAGER"),
                "Lifecycle Workspace",
                "Workspace lifecycle validation",
                "requester",
                "WSP",
                null,
                null,
                "workspace@example.com"
            ),
            now
        );
    }
}
