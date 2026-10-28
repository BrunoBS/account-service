package br.com.portalmanager.platform.workspace.feature.parameter.usecase.port;

import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;

import java.util.Map;
import java.util.Collection;

/** Persistent publication of committed parameter snapshots, using logical keys. */
public interface ParameterPublicationStore {
    void publish(Map<String, PublishedParameterValue> values);

    void remove(String key);

    long removeAll(Collection<String> keys);

    /** Namespace-scoped traversal of the resolved microservice code; includes orphan keys. */
    ParameterCachePage scan(String microserviceCode, String cursor, int count);
}
