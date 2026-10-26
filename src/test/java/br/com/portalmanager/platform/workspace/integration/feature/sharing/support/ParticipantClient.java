package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ParticipantClient extends BaseClient {
    private static final String PATH="/api/v1/workspaces/{workspace}/applications/{application}";
    public ParticipantClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public ParticipantClientResponse request(String workspace,String application,String contract) { return response(owner().post(PATH+"/resource-sharing/contracts/{contract}/participants",workspace,application,contract).then(),ParticipantClientResponse::new); }
    public ParticipantClientResponse list(String workspace,String application,String status,String contract,int page,int size) { var req=owner().queryParam("page",page).queryParam("size",size); if(status!=null)req.queryParam("status",status); if(contract!=null)req.queryParam("contractIdentifier",contract); return response(req.get(PATH+"/resource-sharing/participants",workspace,application).then(),ParticipantClientResponse::new); }
    public ParticipantClientResponse find(String workspace,String application,String participant) { return response(owner().get(PATH+"/resource-sharing/participants/{participant}",workspace,application,participant).then(),ParticipantClientResponse::new); }
    public ParticipantClientResponse requestAgain(String workspace,String application,String participant) { return response(owner().post(PATH+"/resource-sharing/participants/{participant}/request",workspace,application,participant).then(),ParticipantClientResponse::new); }
    public ParticipantClientResponse delete(String workspace,String application,String participant) { return response(owner().delete(PATH+"/resource-sharing/participants/{participant}",workspace,application,participant).then(),ParticipantClientResponse::new); }
    public ParticipantClientResponse listContracts(String workspace,String application,String ownerWorkspace,String ownerApplication,int page,int size) { var req=owner().queryParam("page",page).queryParam("size",size); if(ownerWorkspace!=null)req.queryParam("ownerWorkspaceIdentifier",ownerWorkspace); if(ownerApplication!=null)req.queryParam("ownerApplicationIdentifier",ownerApplication); return response(req.get(PATH+"/resource-sharing/contracts/available",workspace,application).then(),ParticipantClientResponse::new); }
    public ParticipantClientResponse findContract(String workspace,String application,String contract) { return response(owner().get(PATH+"/resource-sharing/contracts/available/{contract}",workspace,application,contract).then(),ParticipantClientResponse::new); }
    private RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build()).header("correlation-id","workspace-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
}
