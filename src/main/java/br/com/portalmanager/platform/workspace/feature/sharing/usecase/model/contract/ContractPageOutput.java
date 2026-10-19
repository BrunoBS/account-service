package br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.contract;

import java.util.List;

public record ContractPageOutput(
        List<ContractOutput> content, int page, int size, long totalElements, int totalPages) {}
