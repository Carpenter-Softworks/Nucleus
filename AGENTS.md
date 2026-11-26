# nucleus

A self-hosted pull-through cache for Maven, PyPI, npm, OCI/Docker, NVD data
feeds and Eclipse p2, published by Carpenter Softworks under
AGPL-3.0-or-later. Java 25 on Quarkus. It is pre-alpha: the scaffold exists,
the product does not.

Read `docs/decisions.md` before proposing a design: it records what is
settled and which alternatives were rejected. Record a new load-bearing
decision there, in the same entry format (`make lint` checks it).

## Modules

- `core` (`nucleus-core`): plain Java. No Quarkus, no Vert.x. The Maven module
  graph is the boundary: `core` does not depend on them, so it cannot use them.
- `app` (`nucleus-app`): the Quarkus application. It depends on `core`.
- Coordinates: groupId `de.vptr`; Java packages under `de.vptr.nucleus`.

## Threading model

Blocking code on virtual threads (`@RunOnVirtualThread`). No Mutiny, no
reactive types, and no mixing of the two models.
`app/src/test/java/de/vptr/nucleus/app/ThreadingModelTest.java` enforces it on
bytecode dependencies and is itself proven against a deliberate violation
(`MutinyViaVarFixture`). Do not weaken the rule to make code compile; change
the code.

## Gates

- Run `make hooks` once per clone.
- `make check`: verifies the toolchain (JDK 25 and the devkit pin).
- `make lint`: repository gates (`devkitw`, pins, decisions log, secrets,
  Markdown), then the Java gates (Error Prone and NullAway, Spotless,
  Checkstyle, SpotBugs, and the rest of devkit's set).
- `make test`: tests, with JaCoCo requiring 100% line and 100% branch coverage.
- `make format` fixes Markdown and Java formatting.
- Run the Maven build through `make`, which uses devkit's pinned Maven.

100% coverage means every decision point in the project's own code is
exercised. It does not mean exception paths in general are tested, nor that
behaviour inside a library is: failure and concurrency behaviour needs
behavioural tests of its own. See the coverage entry in `docs/decisions.md`.

## Rules

- devkit is pinned in `devkit.toml`. Never edit `devkitw`; a bump copies the
  new one in (`make lint` verifies it).
- Work lands on `main` only through a branch and a pull request.
- A fix for a failing gate fixes the cause. Do not suppress a finding without
  a recorded reason next to the suppression.
- Never commit secrets, and never log credentials.
