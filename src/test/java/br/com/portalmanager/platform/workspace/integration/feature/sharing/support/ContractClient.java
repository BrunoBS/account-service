package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.request.ContractRequest;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.ParticipantConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.request.ParticipantStatusRequest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ContractClient extends BaseClient {
    private static final String PATH = "/api/v1/workspaces/{workspace}/applications/{application}/resource-sharing/contracts";
    public ContractClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public ContractClientResponse create(String workspace, String application, ContractRequest body) { return response(owner().body(json(body)).post(PATH, workspace, application).then(), ContractClientResponse::new); }
    public ContractClientResponse list(String workspace, String application, String lifecycle, int page, int size) { RequestSpecification req=owner().queryParam("page",page).queryParam("size",size); if(lifecycle!=null) req.queryParam("lifecycle",lifecycle); return response(req.get(PATH,workspace,application).then(),ContractClientResponse::new); }
    public ContractClientResponse find(String workspace, String application, String contract) { return response(owner().get(PATH+"/{contract}",workspace,application,contract).then(),ContractClientResponse::new); }
    public ContractClientResponse update(String workspace, String application, String contract, ContractRequest body) { return response(owner().body(json(body)).put(PATH+"/{contract}",workspace,application,contract).then(),ContractClientResponse::new); }
    public ContractClientResponse activate(String workspace,String application,String contract) { return response(owner().patch(PATH+"/{contract}/activate",workspace,application,contract).then(),ContractClientResponse::new); }
    public ContractClientResponse inactivate(String workspace,String application,String contract) { return response(owner().patch(PATH+"/{contract}/inactivate",workspace,application,contract).then(),ContractClientResponse::new); }
    public ContractClientResponse delete(String workspace,String application,String contract) { return response(owner().delete(PATH+"/{contract}",workspace,application,contract).then(),ContractClientResponse::new); }
    public ContractClientResponse participants(String workspace,String application,String contract,String participantName,String participantApplication,String status,int page,int size) { RequestSpecification req=owner().queryParam("page",page).queryParam("size",size); if(participantName!=null) req.queryParam("participantName",participantName); if(participantApplication!=null) req.queryParam("participantApplicationIdentifier",participantApplication); if(status!=null) req.queryParam("status",status); return response(req.get(PATH+"/{contract}/participants",workspace,application,contract).then(),ContractClientResponse::new); }
    public ContractClientResponse participants(String workspace,String application,String contract,String status,int page,int size) { return participants(workspace,application,contract,null,null,status,page,size); }
    public ContractClientResponse changeParticipantStatus(String workspace,String application,String contract,String participant,ParticipantStatusRequest body) { return response(owner().body(json(body)).patch(PATH+"/{contract}/participants/{participant}/status",workspace,application,contract,participant).then(),ContractClientResponse::new); }
    public ContractClientResponse findParticipant(String workspace,String application,String contract,String participant) { return response(owner().get(PATH+"/{contract}/participants/{participant}",workspace,application,contract,participant).then(),ContractClientResponse::new); }
    public ContractClientResponse updateConfiguration(String workspace,String application,String contract,String participant,ParticipantConfigurationRequest body) { return response(owner().body(json(body)).put(PATH+"/{contract}/participants/{participant}/configuration",workspace,application,contract,participant).then(),ContractClientResponse::new); }
    private RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build()).header("correlation-id","workspace-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
}
