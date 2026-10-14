package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentTypeCompatibility;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnvironmentTypeCompatibilityRepository extends JpaRepository<EnvironmentTypeCompatibility, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select compatibility from EnvironmentTypeCompatibility compatibility where compatibility.parentType.code = :parentTypeCode and compatibility.childType.code = :childTypeCode")
    Optional<EnvironmentTypeCompatibility> findByTypePairForUpdate(@Param("parentTypeCode") String parentTypeCode, @Param("childTypeCode") String childTypeCode);

    @Query(value = "select parent_type_code as parentTypeCode, child_type_code as childTypeCode from environment_type_compatibilities where lifecycle_code = 'ACTIVE' order by id for share", nativeQuery = true)
    List<TypeLink> findActiveTypeLinksForValidation();

    @Query(value = "select id from environment_type_compatibilities where parent_type_code = :parentTypeCode and child_type_code = :childTypeCode and lifecycle_code = 'ACTIVE' limit 1 for share", nativeQuery = true)
    Optional<Long> findActiveLinkForValidation(@Param("parentTypeCode") String parentTypeCode, @Param("childTypeCode") String childTypeCode);

    @Query(value = "select id from environment_type_compatibilities where parent_type_code = :parentTypeCode " +
        "and lifecycle_code = 'ACTIVE' order by id limit 1 for share", nativeQuery = true)
    Optional<Long> findActiveChildLinkForTypeValidation(@Param("parentTypeCode") String parentTypeCode);

    interface TypeLink {
        String getParentTypeCode();
        String getChildTypeCode();
    }

    @EntityGraph(attributePaths = { "parentType", "childType" })
    Optional<EnvironmentTypeCompatibility> findByIdentifier(String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select compatibility from EnvironmentTypeCompatibility compatibility where compatibility.identifier = :identifier")
    Optional<EnvironmentTypeCompatibility> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @EntityGraph(attributePaths = { "parentType", "childType" })
    List<EnvironmentTypeCompatibility> findByLifecycleOrderByIdAsc(LifecycleTypeCode lifecycle);

    @Query(value = "select id from environment_type_compatibilities where parent_type_code = :typeCode or child_type_code = :typeCode limit 1 for share", nativeQuery = true)
    Optional<Long> findUsageForTypeValidation(@Param("typeCode") String typeCode);
}
