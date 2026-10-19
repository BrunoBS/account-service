package br.com.portalmanager.platform.workspace.feature.segmentation.repository.category;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.category.Category;

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
            value = """
            select resource
            from Category resource join fetch resource.segment parent
            where resource.identifier = :identifier and resource.segment.identifier = :segmentIdentifier
            """)
    Optional<Category> findBySegmentIdentifierAndIdentifier(
            @Param("segmentIdentifier") String segmentIdentifier,
            @Param("identifier") String identifier);

    @Query(
            value = """
            select resource
            from Category resource join fetch resource.segment parent
            where resource.lifecycle.value = :lifecycle
            and parent.identifier = :segmentIdentifier
            """,
            countQuery = """
            select count(resource)
            from Category resource join resource.segment parent
            where resource.lifecycle.value = :lifecycle
            and parent.identifier = :segmentIdentifier
            """)
    Page<Category> findPage(
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
