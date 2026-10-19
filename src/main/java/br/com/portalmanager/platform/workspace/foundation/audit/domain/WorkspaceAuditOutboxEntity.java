package br.com.portalmanager.platform.workspace.foundation.audit.domain;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_outbox")
public class WorkspaceAuditOutboxEntity extends AuditOutboxEntity {

    protected WorkspaceAuditOutboxEntity() {}
}
