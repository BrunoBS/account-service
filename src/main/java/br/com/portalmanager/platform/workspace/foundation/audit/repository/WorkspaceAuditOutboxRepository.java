package br.com.portalmanager.platform.workspace.foundation.audit.repository;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxRepository;
import br.com.portalmanager.platform.workspace.foundation.audit.domain.WorkspaceAuditOutboxEntity;

public interface WorkspaceAuditOutboxRepository extends AuditOutboxRepository<WorkspaceAuditOutboxEntity> {}
