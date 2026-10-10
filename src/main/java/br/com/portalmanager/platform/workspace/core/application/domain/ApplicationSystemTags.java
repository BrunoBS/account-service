package br.com.portalmanager.platform.workspace.core.application.domain;

import java.util.Arrays;
import java.util.List;

public final class ApplicationSystemTags {

    private ApplicationSystemTags() {}

    public static List<String> resolve(Application application, String workspaceIdentifier) {
        return Arrays.asList(
            application.getIdentifier(),
            application.getName(),
            application.getAlias(),
            application.getAcronym(),
            application.getApplicationScope().value(),
            application.getAuthorizerGroup(),
            workspaceIdentifier
        );
    }
}
