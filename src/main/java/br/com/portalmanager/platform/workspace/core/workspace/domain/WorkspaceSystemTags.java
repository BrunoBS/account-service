package br.com.portalmanager.platform.workspace.core.workspace.domain;

import java.util.Arrays;
import java.util.List;

public final class WorkspaceSystemTags {

    private WorkspaceSystemTags() {
    }

    public static List<String> resolve(Workspace workspace) {
        return Arrays.asList(
                workspace.getIdentifier(),
                workspace.getName(),
                workspace.getAuthorizerGroup(),
                workspace.getAcronym()
        );
    }
}
