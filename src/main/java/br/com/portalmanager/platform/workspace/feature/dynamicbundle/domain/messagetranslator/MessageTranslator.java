package br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.messagetranslator;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.MessageMessageKeys;
import br.com.portalmanager.platform.workspace.feature.dynamicbundle.domain.message.Message;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "message_translators",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_message_translators_message_locale",
                        columnNames = {"message_id", "locale"}))
public class MessageTranslator {

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
    @AttributeOverride(
            name = "value",
            column = @Column(name = "lifecycle_code", nullable = false, length = 50))
    private LifecycleTypeCode lifecycle;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MessageTranslator() {}

    public MessageTranslator(Message message, MessageTranslatorDetails details, LocalDateTime now) {
        message.requireActiveForTranslator();
        identifier = UUID.randomUUID().toString();
        this.message = message;
        applyDetails(details);
        lifecycle = LifecycleTypeCode.active();
        createdAt = now;
        updatedAt = now;
    }

    public void update(MessageTranslatorDetails details, LocalDateTime now) {
        requireActiveForUpdate();
        applyDetails(details);
        updatedAt = now;
    }

    private void applyDetails(MessageTranslatorDetails details) {
        locale = details.locale();
        title = details.title();
        detail = details.detail();
        suggestion = details.suggestion();
    }

    public void activate(LocalDateTime now) {
        message.requireActiveForTranslator();
        if (!isInactive())
            throw new ValidationException(MessageMessageKeys.TRANSLATOR_ACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.active();
        updatedAt = now;
    }

    public void inactivate(LocalDateTime now) {
        if (!isActive())
            throw new ValidationException(MessageMessageKeys.TRANSLATOR_INACTIVATE_INVALID);
        lifecycle = LifecycleTypeCode.inactive();
        updatedAt = now;
    }

    public void requireInactiveForDeletion() {
        if (!isInactive())
            throw new ValidationException(MessageMessageKeys.TRANSLATOR_DELETE_INVALID);
    }

    public boolean isInactive() {
        return LifecycleTypeCode.inactive().equals(lifecycle);
    }

    public void requireActiveForUpdate() {
        if (!isActive())
            throw new ValidationException(MessageMessageKeys.TRANSLATOR_UPDATE_INVALID);
    }

    private boolean isActive() {
        return LifecycleTypeCode.active().equals(lifecycle);
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
