package br.com.portalmanager.platform.workspace.feature.segmentation.repository.segment;

import br.com.portalmanager.platform.workspace.feature.segmentation.domain.segment.Segment;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SegmentRepository extends JpaRepository<Segment, Long> {
    boolean existsByCode(String code);

    Optional<Segment> findByIdentifier(String identifier);

    @Query(
            """
            select resource
            from Segment resource
            where resource.lifecycle.value = :lifecycle
            and (:code is null or resource.code = :code)
            and (:name is null or lower(resource.name) like lower(concat('%', :name, '%')) escape '!')
            """)
    Page<Segment> findPage(
            @Param("code") String code,
            @Param("name") String name,
            @Param("lifecycle") String lifecycle,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource.id from Segment resource where resource.identifier = :identifier")
    Optional<Long> findInternalIdByIdentifierForUpdate(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select resource from Segment resource where resource.identifier = :identifier")
    Optional<Segment> findByIdentifierForUpdate(@Param("identifier") String identifier);
}
