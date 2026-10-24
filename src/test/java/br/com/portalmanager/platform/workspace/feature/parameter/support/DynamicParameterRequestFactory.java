package br.com.portalmanager.platform.workspace.feature.parameter.support;

/** Keeps JSON values verbatim for schema and publication tests. */
final class DynamicParameterRequestFactory {

    private DynamicParameterRequestFactory() {
    }

    static String create(String parent, String key, String type, String valueJson) {
        return "{\"microserviceIdentifier\":\"" + parent + "\",\"key\":\"" + key
                + "\",\"type\":\"" + type + "\",\"value\":" + valueJson + "}";
    }
}
