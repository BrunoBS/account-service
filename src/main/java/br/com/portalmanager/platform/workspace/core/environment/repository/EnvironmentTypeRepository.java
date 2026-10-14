package br.com.portalmanager.platform.workspace.core.environment.repository;

import br.com.portalmanager.platform.workspace.core.environment.domain.environmenttype.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnvironmentTypeRepository extends JpaRepository<EnvironmentType, String> {
    List<EnvironmentType> findByLifecycleOrderByDisplayOrderAscCodeAsc(LifecycleTypeCode lifecycle);
    Optional<EnvironmentType> findByCodeAndLifecycle(String code, LifecycleTypeCode lifecycle);
    boolean existsByCode(String code);

    @Query("select type.code from EnvironmentType type order by type.code")
    List<String> findAllCodes();

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select type from EnvironmentType type where type.code = :code")
    Optional<EnvironmentType> findByCodeForRead(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select type from EnvironmentType type where type.code = :code")
    Optional<EnvironmentType> findByCodeForUpdate(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select type from EnvironmentType type order by type.code")
    List<EnvironmentType> findAllForUpdate();

    List<EnvironmentType> findAllByOrderByDisplayOrderAscCodeAsc();
}
