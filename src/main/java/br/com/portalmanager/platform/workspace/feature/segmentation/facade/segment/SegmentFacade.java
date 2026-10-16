package br.com.portalmanager.platform.workspace.feature.segmentation.facade.segment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.CreateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.SegmentOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.UpdateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentCommandService;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.segment.SegmentQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class SegmentFacade {
    private final SegmentCommandService commands;
    private final SegmentQueryService queries;

    public SegmentFacade(SegmentCommandService commands, SegmentQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public SegmentOutput create(
            AuthorizationContext authorizationContext, CreateSegmentInput input) {
        return commands.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public SegmentOutput findByIdentifier(
            AuthorizationContext authorizationContext, String identifier) {
        return queries.findByIdentifier(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public Page<SegmentOutput> findAll(
            AuthorizationContext authorizationContext,
            String code,
            String name,
            String lifecycle,
            Pageable pageable) {
        return queries.findAll(code, name, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public SegmentOutput update(
            AuthorizationContext authorizationContext,
            String identifier,
            UpdateSegmentInput input) {
        return commands.update(identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public SegmentOutput activate(AuthorizationContext authorizationContext, String identifier) {
        return commands.activate(identifier);
    }

    @AuthorizationRequired(
            level = AuthorizationLevel.OWNER,
            action = AuthorizationAction.DEACTIVATE)
    public SegmentOutput inactivate(AuthorizationContext authorizationContext, String identifier) {
        return commands.inactivate(identifier);
    }
}
