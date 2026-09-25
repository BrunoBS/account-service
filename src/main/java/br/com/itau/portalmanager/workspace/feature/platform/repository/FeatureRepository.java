package br.com.itau.portalmanager.workspace.feature.platform.repository;

import br.com.itau.portalmanager.workspace.feature.platform.domain.Feature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, String> {
    Optional<Feature> findByCode(String code);
}
