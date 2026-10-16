package br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.category;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.category.request.CreateCategoryRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.category.request.UpdateCategoryRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.category.response.CategoryResponse;
import br.com.portalmanager.platform.workspace.feature.segmentation.facade.category.CategoryFacade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/segmentation/segments/{segmentIdentifier}/categories")
public class CategoryController {
    private final CategoryFacade facade;

    public CategoryController(CategoryFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(
            AuthorizationContext authorizationContext,
            @PathVariable UUID segmentIdentifier,
            @RequestBody CreateCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        CategoryResponse.from(
                                facade.create(
                                        authorizationContext,
                                        request.toInput(segmentIdentifier.toString()))));
    }

    @GetMapping("/{categoryIdentifier}")
    public CategoryResponse findByIdentifier(
            AuthorizationContext authorizationContext,
            @PathVariable UUID segmentIdentifier,
            @PathVariable String categoryIdentifier) {
        return CategoryResponse.from(
                facade.findByIdentifier(
                        authorizationContext, segmentIdentifier.toString(), categoryIdentifier));
    }

    @GetMapping
    public Page<CategoryResponse> findAll(
            AuthorizationContext authorizationContext,
            @PathVariable UUID segmentIdentifier,
            @RequestParam(required = false) String lifecycle,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return facade.findAll(
                        authorizationContext, segmentIdentifier.toString(), lifecycle, pageable)
                .map(CategoryResponse::from);
    }

    @PutMapping("/{categoryIdentifier}")
    public CategoryResponse update(
            AuthorizationContext authorizationContext,
            @PathVariable UUID segmentIdentifier,
            @PathVariable String categoryIdentifier,
            @RequestBody UpdateCategoryRequest request) {
        return CategoryResponse.from(
                facade.update(
                        authorizationContext,
                        segmentIdentifier.toString(),
                        categoryIdentifier,
                        request.toInput()));
    }

    @PatchMapping("/{categoryIdentifier}/activate")
    public CategoryResponse activate(
            AuthorizationContext authorizationContext,
            @PathVariable UUID segmentIdentifier,
            @PathVariable String categoryIdentifier) {
        return CategoryResponse.from(
                facade.activate(
                        authorizationContext, segmentIdentifier.toString(), categoryIdentifier));
    }

    @PatchMapping("/{categoryIdentifier}/inactivate")
    public CategoryResponse inactivate(
            AuthorizationContext authorizationContext,
            @PathVariable UUID segmentIdentifier,
            @PathVariable String categoryIdentifier) {
        return CategoryResponse.from(
                facade.inactivate(
                        authorizationContext, segmentIdentifier.toString(), categoryIdentifier));
    }
}
