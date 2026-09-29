package br.com.portalmanager.platform.workspace.entrypoint.web.environment.response;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTreeOutput;
import java.util.List;

public record EnvironmentTreeResponse(EnvironmentResponse environment, List<EnvironmentTreeResponse> children) {
    public static EnvironmentTreeResponse from(EnvironmentTreeOutput output) {
        return new EnvironmentTreeResponse(EnvironmentResponse.from(output.environment()),
                output.children().stream().map(EnvironmentTreeResponse::from).toList());
    }
}
