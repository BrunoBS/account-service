package br.com.portalmanager.platform.workspace.feature.segmentation.facade.category;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CreateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.UpdateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category.CategoryCommandService;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.category.CategoryQueryService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryFacade {
    private final CategoryCommandService commands;
    private final CategoryQueryService queries;

    public CategoryFacade(CategoryCommandService commands, CategoryQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public CategoryOutput create(
            AuthorizationContext authorizationContext, CreateCategoryInput input) {
        return commands.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public CategoryOutput findByIdentifier(
            AuthorizationContext authorizationContext,
            String segmentIdentifier,
            String identifier) {
        return queries.findByIdentifier(segmentIdentifier, identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public Page<CategoryOutput> findAll(
            AuthorizationContext authorizationContext,
            String segmentIdentifier,
            String lifecycle,
            Pageable pageable) {
        return queries.findAll(segmentIdentifier, lifecycle, pageable);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public CategoryOutput update(
            AuthorizationContext authorizationContext,
            String segmentIdentifier,
            String identifier,
            UpdateCategoryInput input) {
        return commands.update(segmentIdentifier, identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.ACTIVATE)
    public CategoryOutput activate(
            AuthorizationContext authorizationContext,
            String segmentIdentifier,
            String identifier) {
        return commands.activate(segmentIdentifier, identifier);
    }

    @AuthorizationRequired(
            level = AuthorizationLevel.OWNER,
            action = AuthorizationAction.DEACTIVATE)
    public CategoryOutput inactivate(
            AuthorizationContext authorizationContext,
            String segmentIdentifier,
            String identifier) {
        return commands.inactivate(segmentIdentifier, identifier);
    }
}
