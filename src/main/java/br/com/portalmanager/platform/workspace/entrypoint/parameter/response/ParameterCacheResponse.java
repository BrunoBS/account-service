package br.com.portalmanager.platform.workspace.entrypoint.parameter.response;

import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;
import java.util.List;

public record ParameterCacheResponse(String cursor, List<String> keys, boolean complete) {
    public static ParameterCacheResponse from(ParameterCachePage page) {
        return new ParameterCacheResponse(page.cursor(), page.keys(), "0".equals(page.cursor()));
    }
}
