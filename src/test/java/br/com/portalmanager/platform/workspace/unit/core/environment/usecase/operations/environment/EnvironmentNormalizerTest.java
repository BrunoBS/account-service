package br.com.portalmanager.platform.workspace.unit.core.environment.usecase.operations.environment;


import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentNormalizer;
import static org.junit.jupiter.api.Assertions.*;

import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class EnvironmentNormalizerTest {
    private final EnvironmentNormalizer normalizer=new EnvironmentNormalizer();
    private final JsonMapper json=JsonMapper.builder().build();

    @Test void normalizesCreateAndUpdateInputsWithoutMutatingSettings(){
        var settings=json.createObjectNode().put("key","value");
        var create=normalizer.normalize(new CreateEnvironmentInput(null," Name "," Description "," dev ",null," ops ",settings," custom "," parent "));
        assertEquals("Name",create.name()); assertEquals("Description",create.description());
        assertEquals("DEV",create.authorizationType()); assertEquals("E-OPS",create.authorizerGroup());
        assertEquals("custom",create.environmentTypeCode()); assertEquals("parent",create.parentIdentifier());
        assertNotSame(settings,create.settings());
        var update=normalizer.normalize(new UpdateEnvironmentInput(2L," Name "," Description "," tst ",4,"E-OPS",null,"SHARD",null));
        assertEquals("TST",update.authorizationType()); assertEquals("E-OPS",update.authorizerGroup());
        assertTrue(update.settings().isObject());
        assertNull(normalizer.normalize((CreateEnvironmentInput)null));
        assertNull(normalizer.normalize((UpdateEnvironmentInput)null));
    }

    @Test void normalizesKnownProtocolFieldsAndLeavesOtherValuesIntact(){
        var payload=json.createObjectNode().put("name"," Name ").put("description"," Desc ")
                .put("parentIdentifier"," id ").put("parentTypeCode"," default ").put("childTypeCode"," shard ")
                .put("authorizationType"," dev ").put("environmentTypeCode"," custom ")
                .put("authorizerGroup"," ops ").put("code"," cell ").put("extra","  value  ");
        var normalized=normalizer.normalizePayload(payload);
        assertEquals("Name",normalized.get("name").asString());
        assertEquals("default",normalized.get("parentTypeCode").asString());
        assertEquals("DEV",normalized.get("authorizationType").asString());
        assertEquals("OPS",normalized.get("authorizerGroup").asString());
        assertEquals("CELL",normalized.get("code").asString());
        assertEquals("  value  ",normalized.get("extra").asString());
        var array=json.createArrayNode();
        assertSame(array,normalizer.normalizePayload(array));
        var sparse=json.createObjectNode().put("code","  ").put("authorizerGroup"," ").put("authorizationType","");
        sparse.set("environmentTypeCode",json.nullNode());
        sparse.put("name",4);
        var normalizedSparse=normalizer.normalizePayload(sparse);
        assertTrue(normalizedSparse.get("code").isNull());
        assertTrue(normalizedSparse.get("authorizerGroup").isNull());
        assertTrue(normalizedSparse.get("authorizationType").isNull());
        assertTrue(normalizedSparse.get("environmentTypeCode").isNull());
        assertEquals(4,normalizedSparse.get("name").asInt());
        assertSame(sparse.get("name"),normalizedSparse.get("name"));
    }
}
