package br.com.itau.portalmanager.workspace.feature.platform.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "platform_features")
public class Feature {

    @Id
    @Column(name = "identifier", nullable = false, length = 36)
    private String identifier;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "service_identifier", nullable = false, length = 36)
    private String serviceIdentifier;

    @Column(name = "lifecycle_code", nullable = false, length = 50)
    private String lifecycleCode;

    @Column(name = "settings", nullable = false, columnDefinition = "json")
    private String settings;

    protected Feature() {
    }
}
