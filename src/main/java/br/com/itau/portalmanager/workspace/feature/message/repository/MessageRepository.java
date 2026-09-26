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
             where (:serviceIdentifier is null or m.serviceIdentifier = :serviceIdentifier)
               and (:lifecycle is null or m.lifecycle.value = :lifecycle)
               and (:code is null or m.code = :code)
               and (:messageKey is null or m.messageKey = :messageKey)
             order by m.serviceIdentifier asc, m.messageKey asc
            """)
    List<Message> findFiltered(@Param("serviceIdentifier") String serviceIdentifier,
                               @Param("lifecycle") String lifecycle,
                               @Param("code") String code,
                               @Param("messageKey") String messageKey);

    boolean existsByServiceIdentifierAndMessageKey(String serviceIdentifier,String messageKey);
    boolean existsByServiceIdentifierAndMessageKeyAndIdNot(String serviceIdentifier,String messageKey,Long id);
    boolean existsByServiceIdentifierAndCode(String serviceIdentifier,String code);
    boolean existsByServiceIdentifierAndCodeAndIdNot(String serviceIdentifier,String code,Long id);
}
