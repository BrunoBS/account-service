package br.com.portalmanager.platform.workspace.feature.dynamicbundle.repository.message;

import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    boolean existsByMicroserviceId(Long microserviceId);

    Optional<Message> findByIdentifier(String identifier);

    @Query("select message.id from Message message where message.identifier = :identifier")
    Optional<Long> findIdByIdentifier(@Param("identifier") String identifier);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select message from Message message where message.identifier = :identifier")
    Optional<Message> findByIdentifierForUpdate(@Param("identifier") String identifier);

    @Query(
            """
            select message from Message message
             where (:microserviceId is null or message.microserviceId = :microserviceId)
               and (:lifecycle is null or message.lifecycle.value = :lifecycle)
               and (:code is null or message.code = :code)
               and (:messageKey is null or message.messageKey = :messageKey)
            """)
    Page<Message> findFiltered(
            @Param("microserviceId") Long microserviceId,
            @Param("lifecycle") String lifecycle,
            @Param("code") String code,
            @Param("messageKey") String messageKey,
            Pageable pageable);

    boolean existsByMicroserviceIdAndMessageKey(Long microserviceId, String messageKey);

    boolean existsByMicroserviceIdAndMessageKeyAndIdNot(
            Long microserviceId, String messageKey, Long excludedMessageId);

    boolean existsByMicroserviceIdAndCode(Long microserviceId, String code);

    boolean existsByMicroserviceIdAndCodeAndIdNot(
            Long microserviceId, String code, Long excludedMessageId);
}
