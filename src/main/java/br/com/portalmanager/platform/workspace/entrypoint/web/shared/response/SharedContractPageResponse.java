package br.com.portalmanager.platform.workspace.entrypoint.web.shared.response;

import br.com.portalmanager.platform.workspace.feature.shared.usecase.model.contract.SharedContractPageOutput;
import java.util.List;

public record SharedContractPageResponse(
    List<SharedContractResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static SharedContractPageResponse from(SharedContractPageOutput output) {
        return new SharedContractPageResponse(
            output.content().stream().map(SharedContractResponse::from).toList(),
            output.page(),
            output.size(),
            output.totalElements(),
            output.totalPages()
        );
    }
}
