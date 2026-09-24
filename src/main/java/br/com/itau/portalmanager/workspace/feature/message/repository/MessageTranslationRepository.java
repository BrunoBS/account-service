package br.com.itau.portalmanager.workspace.feature.message.repository;

import br.com.itau.portalmanager.workspace.feature.message.domain.MessageTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTranslationRepository extends JpaRepository<MessageTranslation,Long>{
    @Query("""
            select mt from MessageTranslation mt
             where mt.message.id = :messageId
               and (:locale is null or mt.locale = :locale)
               and (:lifecycle is null or mt.lifecycle.value = :lifecycle)
             order by mt.locale asc
            """)
    List<MessageTranslation> findFiltered(@Param("messageId") Long messageId,@Param("locale") String locale,@Param("lifecycle") String lifecycle);
    Optional<MessageTranslation> findByIdentifierAndMessageId(String identifier,Long messageId);
    boolean existsByMessageIdAndLocale(Long messageId,String locale);
    boolean existsByMessageIdAndLocaleAndIdNot(Long messageId,String locale,Long id);
}
