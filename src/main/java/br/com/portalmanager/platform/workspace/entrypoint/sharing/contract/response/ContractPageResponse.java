package br.com.portalmanager.platform.workspace.entrypoint.sharing.contract.response;

import br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract.ContractPageOutput;

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
