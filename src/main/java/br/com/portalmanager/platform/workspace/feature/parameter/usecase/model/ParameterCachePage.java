package br.com.portalmanager.platform.workspace.feature.parameter.usecase.model;

import java.util.List;

/** Logical key names only; no parameter values or physical Redis namespace. */
public record ParameterCachePage(String cursor, List<String> keys) {
    public ParameterCachePage {
        keys = List.copyOf(keys);
    }
}
