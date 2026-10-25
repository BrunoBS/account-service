package br.com.portalmanager.platform.workspace.architecture;

import static org.assertj.core.api.Assertions.*;

import br.com.portalmanager.platform.workspace.feature.servicecontext.repository.featuregroup.FeatureGroupRelationRepository;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.Test;

import java.util.Set;

class GoldenArchitectureRulesTest {
    static class ForbiddenCommand {
        FeatureGroupRelationRepository relations;
    }

    static class CompliantCommand {
        String identifier;
    }

    @Test
    void associationRuleRejectsAControlledForbiddenDependency() {
        JavaClasses classes =
                new ClassFileImporter()
                        .importClasses(
                                ForbiddenCommand.class,
                                CompliantCommand.class,
                                FeatureGroupRelationRepository.class);
        DescribedPredicate<JavaClass> forbidden =
                DescribedPredicate.describe(
                        "controlled command",
                        type -> type.getName().equals(ForbiddenCommand.class.getName()));
        assertThat(
                        GoldenArchitectureRules.associationRule(forbidden)
                                .evaluate(classes)
                                .hasViolation())
                .isTrue();
        DescribedPredicate<JavaClass> compliant =
                DescribedPredicate.describe(
                        "compliant command",
                        type -> type.getName().equals(CompliantCommand.class.getName()));
        assertThat(
                        GoldenArchitectureRules.associationRule(compliant)
                                .evaluate(classes)
                                .hasViolation())
                .isFalse();
    }

    @Test
    void wrapperRuleRejectsTheEntrypointThatWasPreviouslyOutsideTheImportScope() {
        assertThatThrownBy(
                        () ->
                                GoldenArchitectureRules.requireNoRedundantWrappers(
                                        Set.of(
                                                GoldenArchitectureRules.ROOT
                                                        + ".entrypoint.web.parameter.parameterkey")))
                .isInstanceOf(AssertionError.class);
        assertThatCode(
                        () ->
                                GoldenArchitectureRules.requireNoRedundantWrappers(
                                        Set.of(
                                                GoldenArchitectureRules.ROOT
                                                        + ".entrypoint.parameter")))
                .doesNotThrowAnyException();
    }
}
