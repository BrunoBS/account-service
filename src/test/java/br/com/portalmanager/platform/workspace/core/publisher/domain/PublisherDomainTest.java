package br.com.portalmanager.platform.workspace.core.publisher.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class PublisherDomainTest {

    @Test
    void protectsSettingsFromExternalMutation() {
        ObjectNode settings = JsonNodeFactory.instance.objectNode().put("enabled", true);
        Publisher publisher = publisher(settings);
        settings.put("enabled", false);
        assertThat(publisher.getSettings().get("enabled").asBoolean()).isTrue();
        ((ObjectNode) publisher.getSettings()).put("enabled", false);
        assertThat(publisher.getSettings().get("enabled").asBoolean()).isTrue();
    }

    @Test
    void protectsLifecycleAndPreventsUpdatingInactivePublishers() {
        Publisher publisher = publisher(JsonNodeFactory.instance.objectNode());
        LocalDateTime now = LocalDateTime.now();
        assertThrows(ValidationException.class, () -> publisher.restore(now));
        assertThrows(ValidationException.class, () -> publisher.quarantine(now));
        publisher.inactivate(now);
        assertThrows(ValidationException.class, () -> publisher.inactivate(now));
        assertThrows(ValidationException.class, () ->
            publisher.update(details(JsonNodeFactory.instance.objectNode()), now)
        );
        publisher.restore(now);
        publisher.inactivate(now);
        publisher.quarantine(now);
        assertThrows(ValidationException.class, () -> publisher.restore(now));
    }

    private Publisher publisher(ObjectNode settings) {
        return new Publisher("HTTP", details(settings), LocalDateTime.now());
    }

    private PublisherDetails details(ObjectNode settings) {
        return new PublisherDetails("Publisher", "Publisher test", ResourceScopeTypeCode.workspace(), false, settings);
    }
}
