package br.com.portalmanager.platform.workspace.integration.feature.sharing.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response.ContractResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response.ParticipantPageResponse;
import br.com.portalmanager.platform.workspace.entrypoint.sharing.participant.response.ParticipantResponse;
import io.restassured.response.ValidatableResponse;

public final class ParticipantClientResponse extends BaseResponse<ParticipantClientResponse> {
    public ParticipantClientResponse(ValidatableResponse response) { super(response); }
    public ParticipantResponse extractBody() { return extract(ParticipantResponse.class); }
    public ParticipantPageResponse extractParticipants() { return extract(ParticipantPageResponse.class); }
    public ContractPageResponse extractContracts() { return extract(ContractPageResponse.class); }
    public ContractResponse extractContract() { return extract(ContractResponse.class); }
}
