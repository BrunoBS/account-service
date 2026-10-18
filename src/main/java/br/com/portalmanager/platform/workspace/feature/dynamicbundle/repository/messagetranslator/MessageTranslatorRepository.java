package br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.messagetranslator;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator.MessageTranslator;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTranslatorRepository extends JpaRepository<MessageTranslator, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select translator.identifier as identifier, translator.version as version,
                   translator.locale as locale, translator.title as title,
                   translator.detail as detail, translator.suggestion as suggestion,
                   translator.lifecycle.value as lifecycle
              from MessageTranslator translator
             where translator.message.id = :messageId
             order by translator.id
            """)
    List<DeletionSnapshot> findDeletionSnapshotsByMessageId(@Param("messageId") Long messageId);

    interface DeletionSnapshot {
        String getIdentifier();

        Long getVersion();

        String getLocale();

        String getTitle();

        String getDetail();

        String getSuggestion();

        String getLifecycle();
    }

    @Modifying
    @Query("delete from MessageTranslator translator where translator.message.id = :messageId")
    void deleteAllByMessageId(@Param("messageId") Long messageId);

    @Query(
            """
            select translator from MessageTranslator translator
             where translator.message.id = :messageId
               and (:locale is null or translator.locale = :locale)
               and (:lifecycle is null or translator.lifecycle.value = :lifecycle)
            """)
    Page<MessageTranslator> findFiltered(
            @Param("messageId") Long messageId,
            @Param("locale") String locale,
            @Param("lifecycle") String lifecycle,
            Pageable pageable);

    Optional<MessageTranslator> findByIdentifierAndMessageId(String identifier, Long messageId);

    @Query(
            """
            select translator.id from MessageTranslator translator
             where translator.identifier = :identifier and translator.message.id = :messageId
            """)
    Optional<Long> findIdByIdentifierAndMessageId(
            @Param("identifier") String translatorIdentifier, @Param("messageId") Long messageId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            """
            select translator from MessageTranslator translator
             where translator.identifier = :identifier and translator.message.id = :messageId
            """)
    Optional<MessageTranslator> findByIdentifierForUpdate(
            @Param("identifier") String translatorIdentifier, @Param("messageId") Long messageId);

    boolean existsByMessageIdAndLocale(Long messageId, String locale);

    boolean existsByMessageIdAndLocaleAndIdNot(
            Long messageId, String locale, Long excludedTranslatorId);
}
