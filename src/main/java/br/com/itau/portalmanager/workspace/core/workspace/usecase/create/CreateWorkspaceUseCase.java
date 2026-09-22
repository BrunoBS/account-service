package br.com.itau.portalmanager.workspace.core.workspace.usecase.create;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.domain.validation.WorkspaceValidator;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceNormalizer;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceTaggingSupport;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceTypeEnum;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CreateWorkspaceUseCase {

    private final WorkspaceRepository repository;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final WorkspaceTaggingSupport taggingSupport;

    public CreateWorkspaceUseCase(
            WorkspaceRepository repository,
            WorkspaceNormalizer normalizer,
            WorkspaceValidator validator,
            WorkspaceTaggingSupport taggingSupport
    ) {
        this.repository = repository;
        this.normalizer = normalizer;
        this.validator = validator;
        this.taggingSupport = taggingSupport;
    }

    @Transactional
    public WorkspaceOutput execute(CreateWorkspaceInput rawInput) {
        CreateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null
                && input.name() != null
                && repository.existsByName(input.name());

        validator.validateForCreate(normalizer.toValidationData(input), nameDuplicate);

        LocalDateTime now = LocalDateTime.now();
        Workspace workspace = new Workspace(
                WorkspaceTypeEnum.valueOf(input.workspaceType()),
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.settings(),
                input.authorizerGroup(),
                input.emailGroup(),
                now
        );

        input.approvers().forEach(
                approver -> workspace.addApprover(approver.functional(), approver.email())
        );

        Workspace saved = repository.saveAndFlush(workspace);
        taggingSupport.reconcile(saved, input.tags());
        return WorkspaceOutput.from(saved, taggingSupport.findManual(saved));
    }
}
