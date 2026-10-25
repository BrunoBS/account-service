package br.com.portalmanager.platform.workspace.unit.core.application.domain;


import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationDetails;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ApplicationDomainTest {

    @Test
    void protectsSettingsFromExternalMutation() {
        ObjectNode settings = JsonNodeFactory.instance.objectNode().put("enabled", true);
        Application application = application(settings);
        settings.put("enabled", false);
        assertThat(application.getSettings().get("enabled").asBoolean()).isTrue();
        ((ObjectNode) application.getSettings()).put("enabled", false);
        assertThat(application.getSettings().get("enabled").asBoolean()).isTrue();
    }

    @Test
    void rejectsInvalidLifecycleTransitions() {
        Application application = application(JsonNodeFactory.instance.objectNode());
        LocalDateTime now = LocalDateTime.now();
        assertThrows(ValidationException.class, () -> application.restore(now));
        assertThrows(ValidationException.class, () -> application.quarantine(now));
        application.inactivate(now);
        assertThrows(ValidationException.class, () -> application.inactivate(now));
        application.restore(now);
        application.inactivate(now);
        application.quarantine(now);
        assertThat(application.getLifecycle()).isEqualTo(LifecycleTypeCode.quarantined());
        assertThrows(ValidationException.class, () -> application.restore(now));
    }

    @Test
    void treatsMissingAndExplicitNullSettingsAsAbsent() {
        Application missingSettings = application(null);
        Application explicitNullSettings = application(JsonNodeFactory.instance.nullNode());

        assertNull(missingSettings.getSettings());
        assertNull(explicitNullSettings.getSettings());
    }

    private Application application(JsonNode settings) {
        return new Application(
            1L,
            new ApplicationDetails(
                "Application",
                "alias",
                null,
                "APP",
                ApplicationScopeTypeCode.backend(),
                null,
                settings
            ),
            LocalDateTime.now()
        );
    }
}
