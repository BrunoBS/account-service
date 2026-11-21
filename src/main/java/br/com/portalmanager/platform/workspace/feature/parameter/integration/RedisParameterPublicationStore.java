package br.com.portalmanager.platform.workspace.feature.parameter.integration;

import br.com.portalmanager.platform.library.redis.api.store.RedisStore;
import br.com.portalmanager.platform.library.redis.api.store.RedisKeyPage;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.PublishedParameterValue;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.model.ParameterCachePage;
import br.com.portalmanager.platform.workspace.feature.parameter.usecase.port.ParameterPublicationStore;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Collection;

@Component
public class RedisParameterPublicationStore implements ParameterPublicationStore {
    private final RedisStore store;

    public RedisParameterPublicationStore(
            RedisStore store) {
        this.store = store.withNamespace("dynamic-parameter");
    }

    @Override
    public void publish(Map<String, PublishedParameterValue> values) {
        store.setAll(values);
    }

    @Override
    public void remove(String key) {
        store.delete(key);
    }
    @Override
    public long removeAll(Collection<String> keys) {
        return store.deleteAll(keys);
    }

    @Override
    public ParameterCachePage scan(String microserviceCode, String cursor, int count) {
        RedisKeyPage page = store.scan(microserviceCode + ".", cursor, count);
        return new ParameterCachePage(page.cursor(), page.keys());
    }
}
