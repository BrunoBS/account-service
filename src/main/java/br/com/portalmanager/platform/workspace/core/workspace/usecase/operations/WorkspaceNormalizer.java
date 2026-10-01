package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagNormalizer;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class WorkspaceNormalizer {

    public String normalizeTypeFilter(String value) {
        return normalizeType(value);
    }

    public String normalizeTagFilter(String value) {
        return TagNormalizer.normalize(value);
    }

    private String normalizeType(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized.toUpperCase(Locale.ROOT);
    }

}
