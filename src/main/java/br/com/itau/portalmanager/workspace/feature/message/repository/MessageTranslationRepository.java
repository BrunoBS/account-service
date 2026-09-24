package br.com.itau.portalmanager.workspace.feature.message.repository;

import br.com.itau.portalmanager.workspace.feature.message.domain.MessageTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageTranslationRepository extends JpaRepository<MessageTranslation, Long> {

    List<MessageTranslation> findByMessageIdOrderByLocaleAsc(Long messageId);

    Optional<MessageTranslation> findByIdentifierAndMessageId(String identifier, Long messageId);

    boolean existsByMessageIdAndLocale(Long messageId, String locale);

    boolean existsByMessageIdAndLocaleAndIdNot(Long messageId, String locale, Long id);
}
