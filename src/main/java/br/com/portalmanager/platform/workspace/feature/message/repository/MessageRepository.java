package br.com.portalmanager.platform.workspace.feature.message.repository;

import br.com.portalmanager.platform.workspace.feature.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message,Long>{
    Optional<Message> findByIdentifier(String identifier);

    @Query("""
            select m from Message m
             where (:microserviceId is null or m.microserviceId = :microserviceId)
               and (:lifecycle is null or m.lifecycle.value = :lifecycle)
               and (:code is null or m.code = :code)
               and (:messageKey is null or m.messageKey = :messageKey)
             order by m.microserviceId asc, m.messageKey asc
            """)
    List<Message> findFiltered(@Param("microserviceId") Long microserviceId,
                               @Param("lifecycle") String lifecycle,
                               @Param("code") String code,
                               @Param("messageKey") String messageKey);

    boolean existsByMicroserviceIdAndMessageKey(Long microserviceId,String messageKey);
    boolean existsByMicroserviceIdAndMessageKeyAndIdNot(Long microserviceId,String messageKey,Long id);
    boolean existsByMicroserviceIdAndCode(Long microserviceId,String code);
    boolean existsByMicroserviceIdAndCodeAndIdNot(Long microserviceId,String code,Long id);
}
