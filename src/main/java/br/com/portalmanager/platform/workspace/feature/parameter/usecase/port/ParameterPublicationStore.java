package br.com.portalmanager.platform.workspace.feature.parameter.usecase.port;

import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;

import java.util.Map;

/** Persistent publication of committed parameter snapshots, using logical keys. */
public interface ParameterPublicationStore {
    void publish(Map<String, PublishedParameterValue> values);

    void remove(String key);
}
