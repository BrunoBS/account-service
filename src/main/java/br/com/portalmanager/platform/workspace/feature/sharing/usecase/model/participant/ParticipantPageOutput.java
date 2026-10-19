package br.com.portalmanager.platform.workspace.feature.sharing.usecase.model.participant;

import java.util.List;

public record ParticipantPageOutput(
        List<ParticipantOutput> content, int page, int size, long totalElements, int totalPages) {}
