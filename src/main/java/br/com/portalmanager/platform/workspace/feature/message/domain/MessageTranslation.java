package br.com.portalmanager.platform.workspace.feature.message.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "message_translations", uniqueConstraints = @UniqueConstraint(name = "uk_message_translations_message_locale", columnNames = {"message_id", "locale"}))
public class MessageTranslation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    private Long version;
    @Column(nullable = false, unique = true, length = 36, updatable = false)
    private String identifier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;
    @Column(nullable = false, length = 35)
    private String locale;
    @Column(nullable = false, length = 150)
    private String title;
    @Column(nullable = false, length = 1000)
    private String detail;
    @Column(nullable = false, length = 1000)
    private String suggestion;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MessageTranslation() {
    }

    public MessageTranslation(Message message, String locale, String title, String detail, String suggestion, LocalDateTime now) {
        this.identifier = UUID.randomUUID().toString();
        this.message = message;
        this.locale = locale;
        this.title = title;
        this.detail = detail;
        this.suggestion = suggestion;
        this.lifecycle = LifecycleTypeCode.active();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String locale, String title, String detail, String suggestion, LocalDateTime now) {
        this.locale = locale;
        this.title = title;
        this.detail = detail;
        this.suggestion = suggestion;
        this.updatedAt = now;
    }

    public void activate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void quarantine(LocalDateTime now) {
        lifecycle = LifecycleTypeCode.quarantined();
        updatedAt = now;
    }

    public boolean isInactive() {
        return LifecycleTypeCode.inactive().equals(lifecycle);
    }

    public Long getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Message getMessage() {
        return message;
    }

    public String getLocale() {
        return locale;
    }

    public String getTitle() {
        return title;
    }

    public String getDetail() {
        return detail;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public LifecycleTypeCode getLifecycle() {
        return lifecycle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
