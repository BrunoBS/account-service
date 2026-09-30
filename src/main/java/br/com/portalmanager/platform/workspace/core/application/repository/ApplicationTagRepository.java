package br.com.portalmanager.platform.workspace.core.application.repository;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, Long> {

    @Query("""
            select t
              from ApplicationTag t
             where t.application.id = :applicationId
             order by t.name
            """)
    List<ApplicationTag> findByApplicationId(@Param("applicationId") Long applicationId);

    @Query("""
            select t
              from ApplicationTag t
             where t.application.identifier in :identifiers
               and t.originType = :originType
             order by t.application.identifier, t.name
            """)
    List<ApplicationTag> findByApplicationIdentifiersAndOrigin(
            @Param("identifiers") Collection<String> identifiers,
            @Param("originType") TagOriginType originType
    );

    @Query("""
            select distinct t.application.identifier
              from ApplicationTag t
             where t.name = :name
             order by t.application.identifier
            """)
    List<String> findApplicationIdentifiersByTag(@Param("name") String name);

    @Modifying(flushAutomatically = true)
    @Query("delete from ApplicationTag t where t.application.id = :applicationId")
    int deleteByApplicationId(@Param("applicationId") Long applicationId);
}
