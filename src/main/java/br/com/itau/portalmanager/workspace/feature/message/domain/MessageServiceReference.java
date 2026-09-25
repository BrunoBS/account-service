package br.com.itau.portalmanager.workspace.feature.message.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "platform_services")
public class MessageServiceReference {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    protected MessageServiceReference() {
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }
}
