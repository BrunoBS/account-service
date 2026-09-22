package br.com.itau.portalmanager.workspace.core.workspace.usecase.update;

import br.com.itau.portalmanager.workspace.core.workspace.domain.Workspace;
import br.com.itau.portalmanager.workspace.core.workspace.domain.validation.WorkspaceValidator;
import br.com.itau.portalmanager.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceFinder;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceNormalizer;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.support.WorkspaceTaggingSupport;
import br.com.itau.portalmanager.workspace.foundation.catalog.workspace.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.messaging.exception.ResourceVersionConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Service
public class UpdateWorkspaceUseCase {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final WorkspaceTaggingSupport taggingSupport;

    public UpdateWorkspaceUseCase(
            WorkspaceRepository repository,
            WorkspaceFinder finder,
            WorkspaceNormalizer normalizer,
            WorkspaceValidator validator,
            WorkspaceTaggingSupport taggingSupport
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.taggingSupport = taggingSupport;
    }

    @Transactional
    public WorkspaceOutput execute(Long id, UpdateWorkspaceInput rawInput) {
        Workspace workspace = finder.findActive(id);
        UpdateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null
                && input.name() != null
                && repository.existsByNameAndIdNot(input.name(), id);

        validator.validateForUpdate(normalizer.toValidationData(input), nameDuplicate);

        if (!Objects.equals(workspace.getVersion(), input.version())) {
            throw new ResourceVersionConflictException();
        }

        workspace.update(
                WorkspaceTypeEnum.valueOf(input.workspaceType()),
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.settings(),
                input.authorizerGroup(),
                input.emailGroup(),
                LocalDateTime.now()
        );
        workspace.clearApprovers();
        input.approvers().forEach(
                approver -> workspace.addApprover(approver.functional(), approver.email())
        );

        Workspace saved = repository.saveAndFlush(workspace);
        taggingSupport.reconcile(saved, input.tags());
        return WorkspaceOutput.from(saved, taggingSupport.findManual(saved));
    }
}
