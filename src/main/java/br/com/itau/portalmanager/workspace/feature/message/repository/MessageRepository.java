package br.com.itau.portalmanager.workspace.feature.message.repository;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
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
             where (:serviceId is null or m.serviceId = :serviceId)
               and (:lifecycle is null or m.lifecycle.value = :lifecycle)
               and (:code is null or m.code = :code)
               and (:messageKey is null or m.messageKey = :messageKey)
             order by m.serviceId asc, m.messageKey asc
            """)
    List<Message> findFiltered(@Param("serviceId") Long serviceId,
                               @Param("lifecycle") String lifecycle,
                               @Param("code") String code,
                               @Param("messageKey") String messageKey);

    boolean existsByServiceIdAndMessageKey(Long serviceId,String messageKey);
    boolean existsByServiceIdAndMessageKeyAndIdNot(Long serviceId,String messageKey,Long id);
    boolean existsByServiceIdAndCode(Long serviceId,String code);
    boolean existsByServiceIdAndCodeAndIdNot(Long serviceId,String code,Long id);
}
