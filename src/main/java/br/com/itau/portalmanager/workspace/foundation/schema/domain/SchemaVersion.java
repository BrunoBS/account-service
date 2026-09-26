package br.com.itau.portalmanager.workspace.foundation.schema.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "schema_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_schema_versions_schema_version", columnNames = {"schema_id", "schema_version"})
)
public class SchemaVersion {

    public static final String DRAFT = "DRAFT";
    public static final String PUBLISHED = "PUBLISHED";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 36, updatable = false) private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schema_id", nullable = false)
    private Schema schema;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Column(name = "version_name", nullable = false, length = 100)
    private String versionName;

    @Column(name = "definition", nullable = false, columnDefinition = "json")
    private String definition;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SchemaVersion() {
    }

    public SchemaVersion(
            Schema schema,
            Integer schemaVersion,
            String versionName,
            String definition,
            String status,
            LocalDateTime now
    ) {
        if (schema == null) throw new IllegalArgumentException("Schema is required");
        if (schemaVersion == null || schemaVersion < 1) throw new IllegalArgumentException("Schema version must be positive");
        if (definition == null || definition.isBlank()) throw new IllegalArgumentException("Schema definition is required");
        if (!DRAFT.equals(status) && !PUBLISHED.equals(status)) {
            throw new IllegalArgumentException("Schema version status is invalid");
        }

        this.identifier = UUID.randomUUID().toString();
        this.schema = schema;
        this.schemaVersion = schemaVersion;
        this.versionName = versionName == null || versionName.isBlank() ? "v" + schemaVersion : versionName.trim();
        this.definition = definition;
        this.status = status;
        this.createdAt = now;
    }

    public void updateDraft(String versionName, String definition) {
        if (!isDraft()) {
            throw new IllegalStateException("Published schema version is immutable");
        }
        if (definition == null || definition.isBlank()) {
            throw new IllegalArgumentException("Schema definition is required");
        }
        this.versionName = versionName == null || versionName.isBlank() ? "v" + schemaVersion : versionName.trim();
        this.definition = definition;
    }

    public void publish() {
        if (isDraft()) {
            this.status = PUBLISHED;
        }
    }

    public boolean isDraft() { return DRAFT.equals(status); }
    public boolean isPublished() { return PUBLISHED.equals(status); }

    public Long getId() { return id; }
    public String getIdentifier() { return identifier; }
    public Schema getSchema() { return schema; }
    public Integer getSchemaVersion() { return schemaVersion; }
    public String getVersionName() { return versionName; }
    public String getDefinition() { return definition; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
