package br.com.portalmanager.platform.workspace.foundation.tagging.domain;

import br.com.portalmanager.platform.library.tagging.model.TagOriginType;
import br.com.portalmanager.platform.library.tagging.model.TagRecord;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public abstract class AbstractTagEntity implements TagRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_type", nullable = false, length = 20)
    private TagOriginType originType;

    protected AbstractTagEntity() {
    }

    protected AbstractTagEntity(String name, TagOriginType originType) {
        this.name = name;
        this.originType = originType;
    }

    @Override
    public void changeOrigin(TagOriginType originType) {
        this.originType = originType;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TagOriginType getOriginType() {
        return originType;
    }
}
