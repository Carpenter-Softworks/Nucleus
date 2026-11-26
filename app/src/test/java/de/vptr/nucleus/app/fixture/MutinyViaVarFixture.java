package de.vptr.nucleus.app.fixture;

/**
 * Deliberate violation of the threading-model fence, never scanned as production code. It reaches Mutiny with no
 * import: a fully qualified name, and a type only {@code var} infers.
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
