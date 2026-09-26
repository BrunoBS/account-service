package br.com.portalmanager.platform.workspace.foundation.schema.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "schema_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_schema_versions_schema_version", columnNames = {"schema_id", "schema_version"})
)
public class SchemaVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_id", nullable = false)
    private Schema schema;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "version_name", nullable = false, length = 100)
    private String versionName;

    @Column(name = "definition", nullable = false, columnDefinition = "json")
    private String definition;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "status", nullable = false, length = 20))
    private SchemaVersionStatusTypeCode status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SchemaVersion() {
    }

    public SchemaVersion(
            Schema schema,
            Integer schemaVersion,
            String versionName,
            String definition,
            SchemaVersionStatusTypeCode status,
            LocalDateTime now
    ) {
        this.identifier = UUID.randomUUID().toString();
        this.schema = schema;
        this.schemaVersion = schemaVersion;
        this.versionName = versionName == null || versionName.isBlank() ? "v" + schemaVersion : versionName.trim();
        this.definition = definition;
        this.status = status;
        this.createdAt = now;
    }

    public void updateDraft(String versionName, String definition) {
        this.versionName = versionName == null || versionName.isBlank() ? "v" + schemaVersion : versionName.trim();
        this.definition = definition;
    }

    public void publish() {
        if (isDraft()) {
            this.status = SchemaVersionStatusTypeCode.published();
        }
    }

    public boolean isDraft() {
        return SchemaVersionStatusTypeCode.draft().equals(status);
    }

    public boolean isPublished() {
        return SchemaVersionStatusTypeCode.published().equals(status);
    }

    public Long getId() {
        return id;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Schema getSchema() {
        return schema;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public String getVersionName() {
        return versionName;
    }

    public String getDefinition() {
        return definition;
    }

    public SchemaVersionStatusTypeCode getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
