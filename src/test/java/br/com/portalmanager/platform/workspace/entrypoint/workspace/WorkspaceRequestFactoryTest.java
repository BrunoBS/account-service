package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceRequestFactoryTest {

    @Test
    void preservesMalformedSettingsAndNeverFillsAnUpdateVersion() {
        Map<String, Object> invalid = WorkspaceRequestFactory.invalidField("Workspace", "ADMIN", "settings", "not-an-object");
        assertThat(invalid.get("settings")).isEqualTo("not-an-object");

        Map<String, Object> create = WorkspaceRequestFactory.validCreate("Workspace", "ADMIN");
        assertThat(create).doesNotContainKey("version");
        assertThat(WorkspaceRequestFactory.validUpdate(7, "Workspace", "ADMIN"))
                .containsEntry("version", 7);
    }
}
