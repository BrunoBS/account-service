package br.com.portalmanager.platform.workspace.feature.authorizationcache.integration;

import br.com.portalmanager.platform.library.redis.api.store.RedisStore;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RedisAuthorizationCache {
    private static final Duration TTL = Duration.ofMinutes(30);
    private final RedisStore store;

    public RedisAuthorizationCache(RedisStore store) {
        this.store = store.withNamespace("authorization-cache");
    }

    public void reconcile(String prefix, Map<String, String> groups) {
        Set<String> obsolete = new HashSet<>();
        String cursor = "0";
        do {
            var page = store.scan(prefix, cursor, 100);
            page.keys().stream().filter(key -> !groups.containsKey(key)).forEach(obsolete::add);
            cursor = page.cursor();
        } while (!"0".equals(cursor));
        obsolete.forEach(store::delete);
        groups.forEach((key, group) -> store.set(key, group, TTL));
    }
}
