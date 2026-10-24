package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EnvironmentConfigurationRequestFactory {

    private EnvironmentConfigurationRequestFactory() {
    }

    static Map<String, Object> publisher(String code, int order) {
        return Map.of("publisherCode", code, "sortOrder", order,
                "parameters", Map.of("channel", "orders", "free", List.of(1, true, "text")));
    }

    static Map<String, Object> validCreate(String environment, String http, String kafka) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("environmentIdentifier", environment);
        input.put("settings", Map.of());
        input.put("publishers", List.of(publisher(http, 2), publisher(kafka, 1)));
        return input;
    }

    static Map<String, Object> validUpdate(int version, String http, String kafka) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("version", version);
        input.put("settings", Map.of("own", "application", "enabled", true));
        input.put("publishers", List.of(publisher(http, 1), publisher(kafka, 2)));
        return input;
    }

    static Map<String, Object> invalidField(Map<String, Object> valid, String field, Object value) {
        Map<String, Object> input = new LinkedHashMap<>(valid);
        input.put(field, value);
        return input;
    }
}
