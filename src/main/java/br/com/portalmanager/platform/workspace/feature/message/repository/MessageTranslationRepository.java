package br.com.portalmanager.platform.workspace.feature.message.repository;

import br.com.portalmanager.platform.workspace.feature.message.domain.MessageTranslation;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageTranslationRepository extends JpaRepository<MessageTranslation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select translation.identifier as identifier, translation.version as version,
               translation.locale as locale, translation.title as title,
               translation.detail as detail, translation.suggestion as suggestion,
               translation.lifecycle.value as lifecycle
          from MessageTranslation translation
         where translation.message.id = :messageId
         order by translation.id
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
    @Query("delete from MessageTranslation translation where translation.message.id = :messageId")
    void deleteAllByMessageId(@Param("messageId") Long messageId);

    @Query(
        """
        select translation from MessageTranslation translation
         where translation.message.id = :messageId
           and (:locale is null or translation.locale = :locale)
           and (:lifecycle is null or translation.lifecycle.value = :lifecycle)
        """
    )
    Page<MessageTranslation> findFiltered(
        @Param("messageId") Long messageId,
        @Param("locale") String locale,
        @Param("lifecycle") String lifecycle,
        Pageable pageable
    );

    Optional<MessageTranslation> findByIdentifierAndMessageId(String identifier, Long messageId);

    @Query("""
        select translation.id from MessageTranslation translation
         where translation.identifier = :identifier and translation.message.id = :messageId
        """)
    Optional<Long> findIdByIdentifierAndMessageId(
        @Param("identifier") String translationIdentifier,
        @Param("messageId") Long messageId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select translation from MessageTranslation translation
         where translation.identifier = :identifier and translation.message.id = :messageId
        """)
    Optional<MessageTranslation> findByIdentifierForUpdate(
        @Param("identifier") String translationIdentifier,
        @Param("messageId") Long messageId
    );

    boolean existsByMessageIdAndLocale(Long messageId, String locale);

    boolean existsByMessageIdAndLocaleAndIdNot(Long messageId, String locale, Long excludedTranslationId);
}
