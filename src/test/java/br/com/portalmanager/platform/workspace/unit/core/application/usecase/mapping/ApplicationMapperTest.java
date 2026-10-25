package br.com.portalmanager.platform.workspace.unit.core.application.usecase.mapping;


import br.com.portalmanager.platform.workspace.core.application.usecase.mapping.ApplicationMapper;
import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationDetails;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.JsonNodeFactory;

class ApplicationMapperTest {

    @Test
    void returnsAnEmptyTagListWhenNoManualTagsExist() {
        Application application = new Application(
                11L,
                new ApplicationDetails(
                        "Application", "alias", null, "APP", ApplicationScopeTypeCode.backend(), null,
                        JsonNodeFactory.instance.objectNode()),
                LocalDateTime.now());

        var output = new ApplicationMapper().toOutput(application, "workspace", null);

        assertEquals(List.of(), output.tags());
    }
}
