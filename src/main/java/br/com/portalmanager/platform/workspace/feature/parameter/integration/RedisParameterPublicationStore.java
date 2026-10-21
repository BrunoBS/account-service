package br.com.portalmanager.platform.workspace.feature.parameter.integration;

import br.com.portalmanager.platform.library.redis.annotation.RedisNamespace;
import br.com.portalmanager.platform.library.redis.configuration.PlatformRedisProperties.NamespaceMode;
import br.com.portalmanager.platform.library.redis.core.RedisStore;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RedisParameterPublicationStore implements ParameterPublicationStore {
    private final RedisStore store;

    public RedisParameterPublicationStore(
            @RedisNamespace(value = "dynamic-parameter", mode = NamespaceMode.SHARED)
                    RedisStore store) {
        this.store = store;
    }

    @Override
    public void publish(Map<String, PublishedParameterValue> values) {
        store.setAll(values);
    }

    @Override
    public void remove(String key) {
        store.delete(key);
    }
}
