package de.vptr.nucleus.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;

import de.vptr.nucleus.app.fixture.MutinyViaVarFixture;

/**
 * Blocking code on virtual threads is the settled threading model, so production code must not depend on Mutiny. This
 * fence checks Mutiny only; reactive return types and the dispatch annotations are not checked yet (#42). ArchUnit
 * reads bytecode dependencies, which an import ban would miss: fully qualified names and inferred types.
 */
class ThreadingModelTest {

    /** Defined once; the production scan and the fixture check both evaluate it. */
    private static final ArchRule NO_MUTINY = noClasses().that().resideInAPackage("de.vptr.nucleus..").should()
            .dependOnClassesThat().resideInAnyPackage("io.smallrye.mutiny..", "io.vertx.mutiny..")
            .because("blocking code runs on virtual threads; Mutiny must not mix in");

    @Test
    void productionCodeUsesNoMutiny() {
        final var production = new ClassFileImporter().withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("de.vptr.nucleus");
        assertDoesNotThrow(() -> NO_MUTINY.check(production));
    }

    @Test
    void ruleCatchesMutinyReachedWithoutImport() {
        final var fixture = new ClassFileImporter().importClasses(MutinyViaVarFixture.class);
        final var violation = assertThrows(AssertionError.class, () -> NO_MUTINY.check(fixture));
        final var message = String.valueOf(violation.getMessage());
        assertTrue(message.contains(MutinyViaVarFixture.class.getName() + ".shout()"), message);
    }
}
