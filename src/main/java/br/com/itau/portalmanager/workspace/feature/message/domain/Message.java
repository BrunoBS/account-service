package br.com.itau.portalmanager.workspace.feature.message.domain;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name="messages", uniqueConstraints={
        @UniqueConstraint(name="uk_messages_service_message_key", columnNames={"service_id","message_key"}),
        @UniqueConstraint(name="uk_messages_service_message_code", columnNames={"service_id","code"})
})
public class Message {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version @Column(nullable=false) private Long version;
    @Column(nullable=false,unique=true,length=36,updatable=false) private String identifier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private MessageServiceReference service;

    @Column(name="message_key",nullable=false,length=255) private String messageKey;
    @Column(nullable=false,length=50) private String code;
    @Column(name="http_status",nullable=false) private Integer httpStatus;

    @Embedded
    @AttributeOverride(name="value", column=@Column(name="lifecycle_code",nullable=false,length=50))
    private LifecycleTypeCode lifecycle;

    @Column(length=500) private String observation;
    @Column(name="created_at",nullable=false) private LocalDateTime createdAt;
    @Column(name="updated_at",nullable=false) private LocalDateTime updatedAt;
    @OneToMany(mappedBy="message",cascade=CascadeType.ALL,orphanRemoval=true)
    private Set<MessageTranslation> translations=new LinkedHashSet<>();

    protected Message() {}

    public Message(MessageServiceReference service,String messageKey,String code,Integer httpStatus,String observation,LocalDateTime now){
        this.identifier=UUID.randomUUID().toString(); this.service=service; this.messageKey=messageKey;
        this.code=code; this.httpStatus=httpStatus; this.observation=observation; this.lifecycle=LifecycleTypeCode.active();
        this.createdAt=now; this.updatedAt=now;
    }

    public void update(MessageServiceReference service,String messageKey,String code,Integer httpStatus,String observation,LocalDateTime now){
        this.service=service; this.messageKey=messageKey; this.code=code; this.httpStatus=httpStatus;
        this.observation=observation; this.updatedAt=now;
    }
    public void activate(LocalDateTime now){ lifecycle=LifecycleTypeCode.active(); updatedAt=now; }
    public void inactivate(LocalDateTime now){ lifecycle=LifecycleTypeCode.inactive(); updatedAt=now; }
    public void quarantine(LocalDateTime now){ lifecycle=LifecycleTypeCode.quarantined(); updatedAt=now; }
    public boolean isInactive(){ return LifecycleTypeCode.inactive().equals(lifecycle); }

    public Long getId(){return id;} public Long getVersion(){return version;} public String getIdentifier(){return identifier;}
    public MessageServiceReference getService(){return service;} public String getMessageKey(){return messageKey;} public String getCode(){return code;}
    public Integer getHttpStatus(){return httpStatus;} public LifecycleTypeCode getLifecycle(){return lifecycle;}
    public String getObservation(){return observation;} public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getUpdatedAt(){return updatedAt;}
}
