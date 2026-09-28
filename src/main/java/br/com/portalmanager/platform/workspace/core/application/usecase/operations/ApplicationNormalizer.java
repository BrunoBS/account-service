package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ApplicationNormalizer {
    public CreateApplicationInput normalize(CreateApplicationInput value) {
        if (value == null) return null;
        return new CreateApplicationInput(trim(value.name()), trim(value.alias()), trim(value.acronym()),
                code(value.applicationScope()), group(value.authorizerGroup()), settings(value.settings()),
                value.tags());
    }

    public UpdateApplicationInput normalize(UpdateApplicationInput value) {
        if (value == null) return null;
        return new UpdateApplicationInput(value.version(), trim(value.name()), trim(value.alias()),
                trim(value.acronym()), code(value.applicationScope()), group(value.authorizerGroup()),
                settings(value.settings()), value.tags());
    }

    public String normalizeTag(String value) { return TagNormalizer.normalize(value); }
    private String trim(String value) { return value == null ? null : value.trim(); }
    private String code(String value) {
        String normalized = trim(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }
    private String group(String value) {
        String normalized = trim(value);
        if (normalized == null || normalized.isBlank()) return null;
        return normalized.startsWith("A-") ? normalized : "A-" + normalized;
    }
    private String settings(String value) { return value == null || value.isBlank() ? "{}" : value.trim(); }
}
