package br.com.itau.portalmanager.workspace.feature.message.repository;

import br.com.itau.portalmanager.workspace.feature.message.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    Optional<Message> findByIdentifier(String identifier);

    List<Message> findAllByOrderByServiceCodeAscMessageKeyAsc();

    List<Message> findByServiceCodeOrderByMessageKeyAsc(String serviceCode);

    boolean existsByServiceCodeAndMessageKey(String serviceCode, String messageKey);

    boolean existsByServiceCodeAndMessageKeyAndIdNot(
            String serviceCode,
            String messageKey,
            Long id
    );

    boolean existsByServiceCodeAndCode(String serviceCode, String code);

    boolean existsByServiceCodeAndCodeAndIdNot(String serviceCode, String code, Long id);
}
