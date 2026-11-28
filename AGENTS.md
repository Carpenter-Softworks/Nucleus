# nucleus

A self-hosted pull-through cache for Maven, PyPI, npm, OCI/Docker, NVD data
feeds and Eclipse p2, published by Carpenter Softworks under
AGPL-3.0-or-later. Java 25 on Quarkus. It is pre-alpha: the scaffold exists,
the product does not.

Read `docs/decisions.md` before proposing a design: it records what is
settled and which alternatives were rejected. Record a new load-bearing
decision there, in the same entry format. `make lint` checks the `## ADR-N`
heading, exactly one `**Status:**` of Accepted or Proposed, and that a
`**Premise:**` has a `**Guard:**` naming how it is watched; it does not check
the section layout.

## Modules

- `core` (`nucleus-core`): plain Java. No Quarkus, no Vert.x. The Maven module
  graph is the boundary: `core` does not depend on them, so it cannot use them.
- `app` (`nucleus-app`): the Quarkus application. It depends on `core`.
- Coordinates: groupId `de.vptr`; Java packages under `de.vptr.nucleus`.
- Parents: a Quarkus module (`app`) inherits devkit's `devkit-quarkus-parent`
  and restates the root's settings, since it inherits nothing from the root.
  The root inherits `devkit-parent`, and every other module inherits the root.
  The Quarkus version is the Quarkus parent's and moves with a devkit bump
  (ADR-1).

## Threading model

Blocking code on virtual threads (`@RunOnVirtualThread`).
`app/src/test/java/de/vptr/nucleus/app/ThreadingModelTest.java` enforces one
thing: production classes in `de.vptr.nucleus..` must not depend on
`io.smallrye.mutiny..` or `io.vertx.mutiny..`. It is itself proven against a
deliberate violation (`MutinyViaVarFixture`). Do not weaken the rule to make a
test pass; change the code.

The test does not check these rules yet (#42 tracks widening the fence): no
reactive return types and no `@NonBlocking`; Vert.x core only where Quarkus
REST needs it, called from a virtual thread and awaited (ADR-10).

## Gates

- Run `make hooks` once per clone and after each devkit bump.
- `make check`: verifies the toolchain (JDK 25 and the devkit pin) and reports
  outdated version pins.
- `make lint`: repository gates (`devkitw`, pins, decisions log, secrets,
  Markdown), then the Java gates (Error Prone and NullAway, Spotless,
  Checkstyle, SpotBugs, and the rest of devkit's set).
- `make test`: tests, with JaCoCo requiring 100% line and 100% branch coverage.
  It skips integration tests (`*IT`); only `make coverage` runs them, and CI
  runs `make test`.
- `make format` fixes Markdown and Java formatting.
- Run the Maven build through `make`, which uses devkit's pinned Maven.

100% coverage means every decision point in the project's own code is
exercised. It does not mean exception paths in general are tested, nor that
behaviour inside a library is: failure and concurrency behaviour needs
behavioural tests of its own. See the coverage entry in `docs/decisions.md`.

devkit fails the build on a module that has classes but no execution data;
`package-info` and `module-info` do not count, which is why `core` passes
today. Every module with production classes needs at least one unit test that
runs under `make test`: integration tests alone do not count.

## Rules

- devkit is pinned in `devkit.toml`. Never edit `devkitw`; a bump copies the
  new one in (`make lint` verifies it). The GitHub mirror (`mirror` in
  `devkit.toml`) must carry the new signed tag before CI can fetch it. A bump
  also checks that devkit's Quarkus stays on an LTS line (ADR-1) and that
  `junit.version` in the root pom matches the JUnit the Quarkus BOM manages.
- Work lands on `main` only through a branch and a pull request.
- A fix for a failing gate fixes the cause. Do not suppress a finding without
  a recorded reason next to the suppression.
- Never commit secrets, and never log credentials.
