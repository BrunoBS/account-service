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
             where (:service is null or m.service.code = :service)
               and (:lifecycle is null or m.lifecycle.value = :lifecycle)
               and (:code is null or m.code = :code)
               and (:messageKey is null or m.messageKey = :messageKey)
             order by m.service.code asc, m.messageKey asc
            """)
    List<Message> findFiltered(@Param("service") String service,@Param("lifecycle") String lifecycle,
                               @Param("code") String code,@Param("messageKey") String messageKey);

    boolean existsByService_CodeAndMessageKey(String serviceCode,String messageKey);
    boolean existsByService_CodeAndMessageKeyAndIdNot(String serviceCode,String messageKey,Long id);
    boolean existsByService_CodeAndCode(String serviceCode,String code);
    boolean existsByService_CodeAndCodeAndIdNot(String serviceCode,String code,Long id);
}
