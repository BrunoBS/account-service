package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator;

import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorClient;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorFactory;
import br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support.MessageTranslatorScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MessageTranslatorFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private MessageTranslatorClient translators;
    @Autowired private MessageTranslatorScenario scenario;
    private final MessageTranslatorFactory factory = new MessageTranslatorFactory();
    private String message;
    @BeforeEach void prepare() { message = scenario.setup(); }

    @Test @DisplayName("QUERY - busca tradução pelo identificador")
    void encontraTraducao() {
        String identifier = translators.create(message, factory.valid("pt-BR")).expectCreated().extractBody().identifier();
        translators.findByIdentifier(message, identifier).expectOk().expect("locale", "pt-BR");
    }

    @Test @DisplayName("QUERY - retorna não encontrado para tradução ausente")
    void retornaNaoEncontrado() { translators.findByIdentifier(message, "missing-translator").expectNotFound(); }
}
