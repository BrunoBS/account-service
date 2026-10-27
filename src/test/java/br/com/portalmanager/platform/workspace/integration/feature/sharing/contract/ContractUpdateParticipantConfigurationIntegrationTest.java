package br.com.portalmanager.platform.workspace.integration.feature.sharing.contract;

import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ContractFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantClient;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ParticipantFactory;
import br.com.portalmanager.platform.workspace.integration.feature.sharing.support.ResourceSharingScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ContractUpdateParticipantConfigurationIntegrationTest extends BaseIntegrationTest {
    @Autowired private ResourceSharingScenario scenario;
    @Autowired private ContractClient contracts;
    @Autowired private ParticipantClient participants;
    @Autowired private AuthorizationMock authorization;
    private final ContractFactory factory = new ContractFactory();
    private ResourceSharingScenario.Data data;

    @BeforeEach
    void prepareScenario() {
        data = scenario.setup();
    }

    @Test
    @DisplayName("VALIDATION - impede configurar participante ainda pendente")
    void impedeConfigurarParticipantePendente() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        contracts.updateConfiguration(
                        data.ownerWorkspace(),
                        data.ownerApplication(),
                        contract.identifier(),
                        participant,
                        new ParticipantFactory().validConfiguration("source", "destination"))
                .expectConflict();
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para participante inexistente")
    void naoConfiguraParticipanteInexistente() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);

        contracts.updateConfiguration(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        "missing-participant", new ParticipantFactory().validConfiguration("source", "destination"))
                .expectNotFound();
    }

    @Test
    @DisplayName("VALIDATION - impede configurar participante de contrato inativo")
    void impedeConfigurarContratoInativo() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        contracts.inactivate(data.ownerWorkspace(), data.ownerApplication(), contract.identifier()).expectOk();

        contracts.updateConfiguration(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().validConfiguration("source", "destination"))
                .expectNotFound();
    }

    @Test
    @DisplayName("UPDATE - atualiza configuração de participante aprovado")
    void atualizaConfiguracaoDeParticipanteAprovado() {
        ResourceSharingScenario.EnvironmentMapping mapping = scenario.environmentMapping(data);
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        ParticipantFactory participantFactory = new ParticipantFactory();
        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, participantFactory.approve(mapping.source(), mapping.destination()))
                .expectOk();

        contracts.updateConfiguration(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, participantFactory.validConfiguration(mapping.source(), mapping.destination()))
                .expectOk()
                .expect("status", "APPROVED")
                .expectSize("mappings", 1);
    }

    @Test
    @DisplayName("VALIDATION - rejeita configuração com modo de publicação desconhecido")
    void rejeitaModoDePublicacaoDesconhecido() {
        ResourceSharingScenario.EnvironmentMapping mapping = scenario.environmentMapping(data);
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        ParticipantFactory participantFactory = new ParticipantFactory();
        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, participantFactory.approve(mapping.source(), mapping.destination()))
                .expectOk();

        contracts.updateConfiguration(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, participantFactory.withInvalidPublicationMode(mapping.source(), mapping.destination()))
                .expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita configuração ausente para participante aprovado")
    void rejeitaConfiguracaoAusente() {
        ResourceSharingScenario.EnvironmentMapping mapping = scenario.environmentMapping(data);
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        ParticipantFactory participantFactory = new ParticipantFactory();
        contracts.changeParticipantStatus(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, participantFactory.approve(mapping.source(), mapping.destination()))
                .expectOk();

        contracts.updateConfiguration(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, null)
                .expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige permissão administrativa para alterar configuração")
    void exigePermissaoAdministrativa() {
        br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse contract = scenario.createContract(data);
        String participant = scenario.requestParticipation(data, contract.identifier(), participants);
        authorization.forbidden();

        contracts.updateConfiguration(data.ownerWorkspace(), data.ownerApplication(), contract.identifier(),
                        participant, new ParticipantFactory().withoutMappings())
                .expectForbidden();
        authorization.verifyCalledWithPolicy("ADM");
    }
}
