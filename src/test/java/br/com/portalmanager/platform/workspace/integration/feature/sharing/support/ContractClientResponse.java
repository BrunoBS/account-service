package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response.ParticipantPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response.ParticipantResponse;
import io.restassured.response.ValidatableResponse;

public final class ContractClientResponse extends BaseResponse<ContractClientResponse> {
    public ContractClientResponse(ValidatableResponse response) { super(response); }
    public ContractResponse extractBody() { return extract(ContractResponse.class); }
    public ContractPageResponse extractContracts() { return extract(ContractPageResponse.class); }
    public ParticipantResponse extractParticipant() { return extract(ParticipantResponse.class); }
    public ParticipantPageResponse extractParticipants() { return extract(ParticipantPageResponse.class); }
    public String identifier() { return response.extract().path("identifier"); }
}
