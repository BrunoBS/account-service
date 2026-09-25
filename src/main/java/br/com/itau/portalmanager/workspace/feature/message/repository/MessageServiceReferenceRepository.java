package br.com.itau.portalmanager.workspace.feature.message.repository;

import br.com.itau.portalmanager.workspace.feature.message.domain.MessageServiceReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MessageServiceReferenceRepository
        extends JpaRepository<MessageServiceReference, Long> {

    Optional<MessageServiceReference> findByCode(String code);
}
