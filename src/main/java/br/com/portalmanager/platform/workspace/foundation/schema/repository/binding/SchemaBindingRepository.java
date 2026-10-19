package br.com.portalmanager.platform.workspace.foundation.schema.repository.binding;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import java.util.List;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.binding.SchemaBinding;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchemaBindingRepository extends JpaRepository<SchemaBinding, Long> {
    List<SchemaBinding> findByLifecycleOrderByIdAsc(LifecycleTypeCode lifecycle);

    Optional<SchemaBinding> findByIdentifier(String identifier);
    Optional<SchemaBinding> findByResourceTypeAndResourceCode(String type, String code);
    boolean existsByResourceTypeAndResourceCode(String type, String code);
}
