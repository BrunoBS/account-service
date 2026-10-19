package br.com.portalmanager.platform.workspace.feature.segmentation.usecase.mapping.category;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;
import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CreateCategoryInput;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toDomain(Segment segment, CreateCategoryInput input, LocalDateTime now) {
        return new Category(segment, input.code(), input.name(), now);
    }

    public CategoryOutput toOutput(Category category) {
        return new CategoryOutput(
                category.getVersion(), category.getIdentifier(), category.getCode(),
                category.getName(), category.getLifecycle().value(),
                category.getSegment().getIdentifier(), category.isAvailable(),
                category.getCreatedAt(), category.getUpdatedAt());
    }
}
