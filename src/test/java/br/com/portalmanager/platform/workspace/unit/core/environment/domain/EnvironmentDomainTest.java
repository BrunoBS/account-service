package br.com.portalmanager.platform.workspace.unit.core.environment.domain;

import static org.junit.jupiter.api.Assertions.*;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.environment.domain.compatibility.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.EnvironmentDetails;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.json.JsonMapper;

class EnvironmentDomainTest {
    private final LocalDateTime now = LocalDateTime.now();

    private EnvironmentType type(String code, boolean root, boolean children, boolean workspace) {
        return new EnvironmentType(code, new EnvironmentTypeDetails(code, "Environment type", root, children, workspace, 1), now);
    }

    private EnvironmentDetails details(String name) {
        return new EnvironmentDetails(name, "Environment description", AuthorizationTypeCode.development(), "E-OPS", JsonMapper.builder().build().createObjectNode().put("x", 1), 1);
    }

    @Test
    void environmentLifecycleAndUpdateAreRestrictedToValidStates() {
        var environment = new Environment(20L, type("CUSTOM", true, true, true), null, details("Created"), now);
        assertNotNull(environment.getIdentifier());
        assertEquals(20L, environment.getWorkspaceId());
        assertEquals("Created", environment.getName());
        assertEquals("ACTIVE", environment.getLifecycle().value());
        environment.update(details("Updated"), now.plusSeconds(1));
        assertEquals("Updated", environment.getName());
        assertEquals(1, environment.getSettings().get("x").asInt());
        environment.inactivate(now.plusSeconds(2));
        environment.requireRestorable();
        environment.restore(now.plusSeconds(3));
        environment.inactivate(now.plusSeconds(4));
        environment.requireDeletable();
        environment.quarantine(now.plusSeconds(5));
        assertEquals("QUARANTINED", environment.getLifecycle().value());
        assertThrows(ValidationException.class, () -> environment.update(details("No"), now));
        assertThrows(ValidationException.class, () -> environment.restore(now));
        assertThrows(ValidationException.class, () -> environment.inactivate(now));
        assertThrows(ValidationException.class, () -> environment.quarantine(now));
        assertThrows(ValidationException.class, () -> environment.changeType(type("SHARD", false, true, true), now));
    }

    @Test
    void settingsNullIsStoredAsNullAndReturnedAsDefensiveCopy() {
        var withoutSettings = new Environment(20L, type("CUSTOM", true, true, true), null,
                new EnvironmentDetails("No settings", "Description", AuthorizationTypeCode.development(), null, null, 1), now);
        assertNull(withoutSettings.getSettings());
        var explicitNull = new Environment(20L, type("CUSTOM", true, true, true), null,
                new EnvironmentDetails("Explicit null", "Description", AuthorizationTypeCode.development(), null, JsonNodeFactory.instance.nullNode(), 1), now);
        assertNull(explicitNull.getSettings());
        var settings = JsonNodeFactory.instance.objectNode().put("key", "value");
        var environment = new Environment(20L, type("CUSTOM", true, true, true), null,
                new EnvironmentDetails("With settings", "Description", AuthorizationTypeCode.development(), null, settings, 1), now);
        assertNotSame(settings, environment.getSettings());
    }

    @Test
    void environmentTypeAndCompatibilityEnforceLifecycle() {
        var parent = type("CUSTOM", true, true, true);
        var child = type("SHARD", false, true, true);
        parent.update(new EnvironmentTypeDetails("Workspace custom", "Updated", true, true, true, 2), now.plusSeconds(1));
        assertEquals(2, parent.getDisplayOrder());
        parent.inactivate(now.plusSeconds(2));
        parent.restore(now.plusSeconds(3));
        var compatibility = new EnvironmentTypeCompatibility(parent, child, now);
        assertEquals("ACTIVE", compatibility.getLifecycle().value());
        compatibility.inactivate(now.plusSeconds(1));
        compatibility.restore(now.plusSeconds(2));
        compatibility.inactivate(now.plusSeconds(3));
        assertEquals("INACTIVE", compatibility.getLifecycle().value());
        assertThrows(ValidationException.class, () -> compatibility.inactivate(now));
        compatibility.restore(now.plusSeconds(4));
        parent.inactivate(now.plusSeconds(5));
        assertThrows(ValidationException.class, () -> parent.update(new EnvironmentTypeDetails("No", "No", true, true, true, 1), now));
        assertThrows(ValidationException.class, () -> parent.inactivate(now));
    }
}
