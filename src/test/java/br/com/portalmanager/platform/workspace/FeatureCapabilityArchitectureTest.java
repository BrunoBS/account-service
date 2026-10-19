package br.com.portalmanager.platform.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

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
                        "sharing",
                        "onboarding",
                        "parameter");
    }

    @Test
    void singleDomainLayersAreNotWrappedInRedundantSubpackages() {
        Set<String> packageNames =
                classes.stream().map(JavaClass::getPackageName).collect(Collectors.toSet());
        assertThat(packageNames)
                .noneMatch(
                        name ->
                                name.matches(
                                        ".*\\.entrypoint\\.web\\.parameter\\.parameterkey(?:\\..*)?"))
                .noneMatch(
                        name ->
                                name.matches(
                                        ".*\\.feature\\.parameter\\.(domain|facade|repository|usecase\\.model|usecase\\.validation)\\.parameterkey(?:\\..*)?"))
                .noneMatch(
                        name ->
                                name.matches(
                                        ".*\\.feature\\.onboarding\\.(domain|facade|integration|repository|usecase\\.validation)\\.(completion|progress)(?:\\..*)?"))
                .noneMatch(
                        name ->
                                name.matches(
                                        ".*\\.feature\\.sharing\\.usecase\\.validation\\.participant(?:\\..*)?"));
    }
}
