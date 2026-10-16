package br.com.portalmanager.platform.workspace.feature.segmentation.repository.category;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsBySegmentIdAndCode(Long segmentId, String code);

    @Query(
            """
            select new br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput(
                resource.version,
                resource.identifier,
                resource.code,
                resource.name,
                resource.lifecycle.value,
                parent.identifier,
                case when resource.lifecycle.value = 'ACTIVE' and parent.lifecycle.value = 'ACTIVE' then true else false end,
                resource.createdAt,
                resource.updatedAt)
            from Category resource join resource.segment parent
            where resource.identifier = :identifier and resource.segment.identifier = :segmentIdentifier
            """)
    Optional<CategoryOutput> findOutputByIdentifier(
            @Param("segmentIdentifier") String segmentIdentifier,
            @Param("identifier") String identifier);

    @Query(
            """
            select new br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CategoryOutput(
                resource.version,
                resource.identifier,
                resource.code,
                resource.name,
                resource.lifecycle.value,
                parent.identifier,
                case when resource.lifecycle.value = 'ACTIVE' and parent.lifecycle.value = 'ACTIVE' then true else false end,
                resource.createdAt,
                resource.updatedAt)
            from Category resource join resource.segment parent
            where resource.lifecycle.value = :lifecycle
            and parent.identifier = :segmentIdentifier
            """)
    Page<CategoryOutput> findPage(
            @Param("segmentIdentifier") String segmentIdentifier,
            @Param("lifecycle") String lifecycle,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select resource.id from Category resource where resource.identifier = :identifier and"
                    + " resource.segment.identifier = :segmentIdentifier")
    Optional<Long> findInternalIdByIdentifierForUpdate(
            @Param("segmentIdentifier") String segmentIdentifier,
            @Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select resource from Category resource where resource.identifier = :identifier and"
                    + " resource.segment.identifier = :segmentIdentifier")
    Optional<Category> findByIdentifierForUpdate(
            @Param("segmentIdentifier") String segmentIdentifier,
            @Param("identifier") String identifier);
}
