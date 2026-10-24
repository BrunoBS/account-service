package br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environment;

import static org.junit.jupiter.api.Assertions.*;

import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.EnvironmentDetails;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeDetails;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentTreeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EnvironmentMapperTest {
    private final EnvironmentMapper mapper = new EnvironmentMapper();

    @Test
    void mapsDetailsReferencesAndTreeData() {
        var json = JsonMapper.builder().build().createObjectNode().put("region", "sa-east-1");
        var details = new EnvironmentDetails("Environment", "Description", AuthorizationTypeCode.development(), "E-OPS", json, 3);
        var type = new EnvironmentType("CUSTOM", new EnvironmentTypeDetails("Custom", "Workspace", true, true, true, 1), LocalDateTime.now());
        var environment = new Environment(10L, type, null, details, LocalDateTime.now());
        var output = mapper.toOutput(environment, "workspace");
        assertEquals(environment.getIdentifier(), output.identifier());
        assertEquals("workspace", output.workspaceIdentifier());
        assertNull(output.parentIdentifier());
        assertEquals("DEV", output.authorizationType());
        var explicitParent = mapper.toOutput(environment, "workspace", "parent-id");
        assertEquals("parent-id", explicitParent.parentIdentifier());
        var reference = mapper.toReference(1L, output);
        assertEquals(1L, reference.id());
        assertSame(output, reference.environment());
        var child = new EnvironmentTreeOutput(output, List.of());
        var tree = mapper.toTreeOutput(output, List.of(child));
        assertEquals(1, tree.children().size());
        var input = new CreateEnvironmentInput(null, "n", "d", "ADM", 2, "OPS", json, "CUSTOM", null);
        assertEquals(2, mapper.toDetails(input, 2).sortOrder());
        var update = new UpdateEnvironmentInput(1L, "n2", "d2", "TST", 4, null, null, "SHARD", null);
        var updatedDetails = mapper.toDetails(update, 4);
        assertEquals("TST", updatedDetails.authorizationType().value());
        assertEquals("n2", updatedDetails.name());
    }
}
