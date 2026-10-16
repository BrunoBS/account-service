package br.com.portalmanager.platform.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

class FeatureCapabilityArchitectureTest {
    private static final String PREFIX = "br.com.portalmanager.platform.workspace.feature.";
    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(PREFIX.substring(0, PREFIX.length() - 1));

    @Test
    void featureCapabilitiesHaveOnlyTheApprovedNames() {
        Set<String> capabilities =
                classes.stream()
                        .map(JavaClass::getPackageName)
                        .filter(name -> name.startsWith(PREFIX))
                        .map(name -> name.substring(PREFIX.length()).split("\\.")[0])
                        .collect(Collectors.toSet());
        assertThat(capabilities)
                .containsExactlyInAnyOrder(
                        "dynamicbundle",
                        "servicecontext",
                        "segmentation",
                        "resourcesharing",
                        "onboarding",
                        "dynamicparameter");
    }

    @Test
    void entitiesRepositoriesAndFacadesAreOrganizedWithinTheirLayerByResponsibility() {
        assertThat(classes.stream().filter(type -> type.isAnnotatedWith(Entity.class)).toList())
                .isNotEmpty()
                .allSatisfy(
                        type ->
                                assertThat(type.getPackageName())
                                        .matches(".*\\.domain\\.[a-z]+(?:\\..*)?"));
        assertThat(
                        classes.stream()
                                .map(JavaClass::getPackageName)
                                .filter(
                                        name ->
                                                name.endsWith(".repository")
                                                        || name.endsWith(".facade"))
                                .distinct()
                                .toList())
                .as(
                        "Feature repositories and facades must be grouped by their domain or"
                            + " responsibility")
                .isEmpty();
    }
}
