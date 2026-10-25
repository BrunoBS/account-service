package br.com.portalmanager.platform.workspace.integration.foundation.schema.binding;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaBindingClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema binding create")
class SchemaBindingCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("CREATE - cria binding para schema de plataforma")
    void criaBindingEConsultaPersistencia() {
        String schema = scenario.platformDefaultSchemaIdentifier();
        var request = factory.validBinding("TEST_RESOURCE", "resource-create", schema);
        var created = bindings.create(request).expectCreated().extractBinding();
        bindings.find(created.identifier()).expectOk()
                .expect("resourceType", "TEST_RESOURCE").expect("resourceCode", "resource-create")
                .expect("schemaIdentifier", schema).expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - rejeita campos obrigatórios, schema ausente e binding duplicado")
    void rejeitaBindingInvalidoOuDuplicado() {
        String schema = scenario.platformDefaultSchemaIdentifier();
        bindings.create(factory.bindingWithoutResourceType(schema)).expectBadRequest();
        bindings.create(factory.bindingWithoutResourceCode(schema)).expectBadRequest();
        bindings.create(factory.bindingWithoutSchema("TEST_RESOURCE", "missing-schema")).expectBadRequest();
        bindings.create(factory.bindingWithUnknownSchema("TEST_RESOURCE", "unknown-schema")).expectNotFound();
        var request = factory.validBinding("TEST_RESOURCE", "resource-duplicate", schema);
        bindings.create(request).expectCreated();
        bindings.create(request).expectConflict();
    }

    @Test
    @DisplayName("AUTH - exige autorização de owner para criar binding")
    void exigeAutorizacao() {
        String schema = scenario.platformDefaultSchemaIdentifier();
        authorization.forbidden();
        bindings.create(factory.validBinding("TEST_RESOURCE", "forbidden", schema)).expectForbidden();
    }
}
