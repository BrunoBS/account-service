package br.com.portalmanager.platform.workspace.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

class FeatureCapabilityArchitectureTest {
    private static final String PREFIX = "br.com.portalmanager.platform.workspace.feature.";
    private final JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("br.com.portalmanager.platform.workspace");

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
                        "sharing",
                        "onboarding",
                        "parameter");
    }

    @Test
    void singleDomainLayersAreNotWrappedInRedundantSubpackages() {
        Set<String> packageNames =
                classes.stream().map(JavaClass::getPackageName).collect(Collectors.toSet());
        GoldenArchitectureRules.requireNoRedundantWrappers(packageNames);
    }
}
