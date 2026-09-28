package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class EnvironmentNormalizer {
    public CreateEnvironmentInput normalize(CreateEnvironmentInput input) {
        if (input == null) return null;
        return new CreateEnvironmentInput(trim(input.name()), trim(input.description()), authorization(input.authorizationType()),
                input.sortOrder(), group(input.authorizerGroup()), settings(input.settings()),
                trim(input.environmentTypeCode()), trim(input.parentIdentifier()));
    }

    public UpdateEnvironmentInput normalize(UpdateEnvironmentInput input) {
        if (input == null) return null;
        return new UpdateEnvironmentInput(input.version(), trim(input.name()), trim(input.description()),
                authorization(input.authorizationType()), input.sortOrder(), group(input.authorizerGroup()),
                settings(input.settings()), trim(input.environmentTypeCode()));
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private String authorization(String value) {
        String code = trim(value);
        return code == null ? null : code.toUpperCase(Locale.ROOT);
    }
    private String group(String value) {
        String group = trim(value);
        if (group == null || group.isBlank()) return null;
        return group.startsWith("E-") ? group : "E-" + group;
    }
    private String settings(String value) { return value == null || value.isBlank() ? "{}" : value.trim(); }
}
