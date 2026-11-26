package de.vptr.nucleus.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import de.vptr.nucleus.app.fixture.MutinyViaVarFixture;

/**
 * Blocking code on virtual threads is the settled threading model, so Mutiny and reactive code stay out and the two
 * models never mix. ArchUnit reads bytecode dependencies, which an import ban would miss: fully qualified names and
 * types inferred through {@code var}.
 */
class ThreadingModelTest {

    /** Defined once; the production scan and the fixture check both evaluate it. */
    private static final ArchRule NO_MUTINY = noClasses().that().resideInAPackage("de.vptr.nucleus..").should()
            .dependOnClassesThat().resideInAnyPackage("io.smallrye.mutiny..", "io.vertx.mutiny..")
            .because("blocking code runs on virtual threads; Mutiny must not mix in")
            // The first production class does not exist yet: an empty match set is no failure.
            .allowEmptyShould(true);

    @Test
    void productionCodeUsesNoMutiny() {
        final var production = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("de.vptr.nucleus");
        assertDoesNotThrow(() -> NO_MUTINY.check(production));
    }

    @Test
    void ruleCatchesMutinyReachedWithoutImport() {
        final var fixture = new ClassFileImporter().importClasses(MutinyViaVarFixture.class);
        assertThrows(AssertionError.class, () -> NO_MUTINY.check(fixture));
    }
}
