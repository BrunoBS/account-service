package br.com.portalmanager.platform.workspace;

import br.com.portalmanager.platform.library.catalog.model.AbstractCatalogCode;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogCodeArchitectureTest {

    private static final String ROOT = "br.com.portalmanager.platform.workspace";
    private static final String CATALOG = ROOT + ".foundation.catalog.";

    private final com.tngtech.archunit.core.domain.JavaClasses classes =
            new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages(ROOT);

    @Test
    void everyCatalogEnumMustHaveCorrespondingCodeValueObject() {
        Set<String> classNames = classes.stream()
                .map(JavaClass::getName)
                .collect(Collectors.toSet());

        var missingCodes = catalogEnums().stream()
                .map(this::expectedCodeClassName)
                .filter(expected -> !classNames.contains(expected))
                .toList();

        assertThat(missingCodes)
                .as("Every catalog enum must have a corresponding *Code value object in the same domain package")
                .isEmpty();
    }

    @Test
    void catalogCodesMustExtendAbstractCatalogCodeAndExposeBothOfFactories() {
        var violations = catalogEnums().stream()
                .filter(enumClass -> classes.stream()
                        .anyMatch(candidate -> candidate.getName().equals(expectedCodeClassName(enumClass))))
                .map(enumClass -> validateCodeContract(enumClass, findCode(enumClass)))
                .filter(message -> message != null)
                .toList();

        assertThat(violations)
                .as("Catalog *Code VOs must extend AbstractCatalogCode and expose static of(String) and of(Enum)")
                .isEmpty();
    }

    private java.util.List<JavaClass> catalogEnums() {
        return classes.stream()
                .filter(javaClass -> javaClass.getPackageName().startsWith(CATALOG))
                .filter(javaClass -> javaClass.getPackageName().endsWith(".domain"))
                .filter(JavaClass::isEnum)
                .filter(javaClass -> javaClass.getSimpleName().endsWith("Enum"))
                .toList();
    }

    private JavaClass findCode(JavaClass enumClass) {
        String expected = expectedCodeClassName(enumClass);
        return classes.stream()
                .filter(candidate -> candidate.getName().equals(expected))
                .findFirst()
                .orElseThrow();
    }

    private String expectedCodeClassName(JavaClass enumClass) {
        String codeSimpleName = enumClass.getSimpleName().substring(
                0, enumClass.getSimpleName().length() - "Enum".length()) + "Code";
        return enumClass.getPackageName() + "." + codeSimpleName;
    }

    private String validateCodeContract(JavaClass enumClass, JavaClass codeClass) {
        Class<?> reflectedCode = codeClass.reflect();
        Class<?> reflectedEnum = enumClass.reflect();

        if (!AbstractCatalogCode.class.isAssignableFrom(reflectedCode)) {
            return codeClass.getName() + " must extend AbstractCatalogCode";
        }
        if (!hasStaticFactory(reflectedCode, String.class)) {
            return codeClass.getName() + " must expose public static of(String)";
        }
        if (!hasStaticFactory(reflectedCode, reflectedEnum)) {
            return codeClass.getName() + " must expose public static of(" + reflectedEnum.getSimpleName() + ")";
        }
        return null;
    }

    private boolean hasStaticFactory(Class<?> codeClass, Class<?> parameterType) {
        try {
            Method method = codeClass.getMethod("of", parameterType);
            return Modifier.isPublic(method.getModifiers())
                    && Modifier.isStatic(method.getModifiers())
                    && method.getReturnType().equals(codeClass);
        } catch (NoSuchMethodException exception) {
            return false;
        }
    }
}
