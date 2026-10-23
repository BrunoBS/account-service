package br.com.portalmanager.platform.workspace;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Set;

final class GoldenArchitectureRules {
    static final String ROOT = "br.com.portalmanager.platform.workspace";
    static final String ASSOCIATIONS =
            ROOT.replace(".", "\\.")
                    + "\\.feature\\.servicecontext\\.(usecase\\.operations|repository)\\.featuregroup\\.(FeatureGroupAssociation|FeatureGroupRelation).*";

    static ArchRule associationRule(DescribedPredicate<JavaClass> commands) {
        return noClasses()
                .that(commands)
                .should()
                .dependOnClassesThat()
                .haveNameMatching(ASSOCIATIONS);
    }

    static void requireNoRedundantWrappers(Set<String> packages) {
        assertThat(packages)
                .isNotEmpty()
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

    static String capability(String packageName) {
        if (!packageName.startsWith(ROOT + ".")) return "external";
        String[] parts = packageName.substring(ROOT.length() + 1).split("\\.");
        return parts.length < 2 ? parts[0] : parts[0] + "." + parts[1];
    }

    private GoldenArchitectureRules() {}
}
