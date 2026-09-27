package br.com.portalmanager.platform.workspace.entrypoint.web.application.request;

import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import java.util.List;

public record CreateApplicationRequest(String name, String alias, String acronym, String applicationScope,
                                       String authorizerGroup, String settings, List<String> tags) {
    public CreateApplicationInput toInput() {
        return new CreateApplicationInput(name, alias, acronym, applicationScope, authorizerGroup, settings, tags);
    }
}
