package br.com.portalmanager.platform.workspace.entrypoint.web.resourcesharing.contract.response;

import br.com.portalmanager.platform.workspace.feature.resourcesharing.usecase.model.contract.ContractPageOutput;

import java.util.List;

public record ContractPageResponse(
        List<ContractResponse> content, int page, int size, long totalElements, int totalPages) {
    public static ContractPageResponse from(ContractPageOutput output) {
        return new ContractPageResponse(
                output.content().stream().map(ContractResponse::from).toList(),
                output.page(),
                output.size(),
                output.totalElements(),
                output.totalPages());
    }
}
