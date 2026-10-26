package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message;


import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support.MessageScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageClient messages;
    @Autowired private MessageScenario scenario;
    private final MessageFactory factory = new MessageFactory();
    private String microservice;

    @BeforeEach void prepare() { microservice = scenario.setup(); }

    @Test @DisplayName("CREATE - cria mensagem com os dados do serviço")
    void criaMensagemValida() {
        var created = messages.create(factory.valid(microservice, "workspace.created", "WORKSPACE-CREATED"))
                .expectCreated().extractBody();
        messages.findByIdentifier(created.identifier())
                .expectOk()
                .expectNotNull("identifier")
                .expect("messageKey", "workspace.created")
                .expect("code", "WORKSPACE-CREATED")
                .expect("lifecycle", "ACTIVE");
    }

    @Test @DisplayName("VALIDATION - rejeita mensagem sem código")
    void rejeitaMensagemSemCodigo() {
        messages.create(factory.withoutCode(microservice)).expectBadRequest();
    }
}
