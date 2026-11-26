package de.vptr.nucleus.app.fixture;

/**
 * Deliberate violation of the threading-model fence, never scanned as production code. It reaches Mutiny with no
 * import: {@code Uni} by its fully qualified name, the other Mutiny types only through inference.
 */
public final class MutinyViaVarFixture {

    private MutinyViaVarFixture() {
    }

    /** Blocks on a Mutiny pipeline. */
    public static String shout() {
        final var uni = io.smallrye.mutiny.Uni.createFrom().item("x");
        return uni.await().indefinitely();
    }
}
