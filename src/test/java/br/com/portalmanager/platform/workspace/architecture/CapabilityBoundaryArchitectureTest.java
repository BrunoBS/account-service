package br.com.portalmanager.platform.workspace.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

import jakarta.persistence.Entity;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.List;
import java.util.Set;

class CapabilityBoundaryArchitectureTest {
    @Test
    void crossCapabilityImplementationCallsAreConfinedToBoundaryAdapters() {
        JavaClasses classes =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(GoldenArchitectureRules.ROOT);
        List<String> violations = new ArrayList<>();
        int inspected = 0;
        for (JavaClass source : classes) {
            String sourceCapability = GoldenArchitectureRules.capability(source.getPackageName());
            if (!(sourceCapability.startsWith("core.")
                    || sourceCapability.startsWith("feature.")
                    || sourceCapability.startsWith("foundation."))) continue;
            for (Dependency dependency : source.getDirectDependenciesFromSelf()) {
                JavaClass target = dependency.getTargetClass();
                String targetCapability =
                        GoldenArchitectureRules.capability(target.getPackageName());
                if (targetCapability.equals("external")
                        || sourceCapability.equals(targetCapability)) continue;
                inspected++;
                boolean adapter = source.getPackageName().contains(".integration");
                boolean publicPort = target.getPackageName().contains(".usecase.port");
                boolean catalogValue =
                        target.getPackageName()
                                        .startsWith(
                                                GoldenArchitectureRules.ROOT
                                                        + ".foundation.catalog.")
                                && target.getPackageName().contains(".domain")
                                && !target.isAnnotatedWith(Entity.class);
                if (!catalogValue
                        && !(adapter
                                && (publicPort
                                        || target.getPackageName().contains(".usecase")
                                        || target.getPackageName().contains(".integration"))))
                    violations.add(dependency.getDescription());
                if (adapter
                        && (target.getPackageName().contains(".repository")
                                || target.isAnnotatedWith(Entity.class)))
                    violations.add(dependency.getDescription());
            }
        }
        assertThat(inspected).isPositive();
        assertThat(violations).isEmpty();
    }

    @Test
    void serviceNeverOwnsTheTechnicalJsonSchemaEngine() {
        ArchRuleDefinition.noClasses()
                .that()
                .resideInAPackage(GoldenArchitectureRules.ROOT + "..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.networknt..")
                .check(
                        new ClassFileImporter()
                                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                                .importPackages(GoldenArchitectureRules.ROOT));
    }

    @Test
    void consumerPortsHaveConcreteSpringAdapters() {
        JavaClasses classes =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(GoldenArchitectureRules.ROOT);
        List<JavaClass> ports =
                classes.stream()
                        .filter(JavaClass::isInterface)
                        .filter(type -> type.getPackageName().contains(".usecase.port"))
                        .toList();
        assertThat(ports).isNotEmpty();
        for (JavaClass port : ports) {
            assertThat(
                            classes.stream()
                                    .filter(
                                            type ->
                                                    !type.isInterface()
                                                            && !type.getModifiers()
                                                                    .contains(
                                                                            JavaModifier.ABSTRACT))
                                    .filter(type -> port.reflect().isAssignableFrom(type.reflect()))
                                    .filter(
                                            type ->
                                                    AnnotatedElementUtils.hasAnnotation(
                                                            type.reflect(), Component.class)))
                    .as(port.getName())
                    .isNotEmpty();
        }
    }

    @Test
    void privilegedApplicationQueriesAreOnlyCalledByTheGuardedSharingAdapter() {
        JavaClasses classes =
                new ClassFileImporter()
                        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                        .importPackages(GoldenArchitectureRules.ROOT);
        List<JavaMethodCall> calls =
                classes.stream()
                        .flatMap(type -> type.getMethodCallsFromSelf().stream())
                        .filter(
                                call ->
                                        call.getTargetOwner()
                                                .getName()
                                                .equals(
                                                        GoldenArchitectureRules.ROOT
                                                                + ".core.application.usecase.operations.ApplicationQueryService"))
                        .filter(
                                call ->
                                        Set.of(
                                                        "findActiveScopeForShared",
                                                        "findReferencesForResourceSharing",
                                                        "findActiveReferencesForResourceSharing")
                                                .contains(call.getTarget().getName()))
                        .toList();
        assertThat(calls)
                .isNotEmpty()
                .allSatisfy(
                        call ->
                                assertThat(call.getOriginOwner().getName())
                                        .isEqualTo(
                                                GoldenArchitectureRules.ROOT
                                                        + ".feature.sharing.integration.ResourceSharingReferenceAdapter"));
    }
}
