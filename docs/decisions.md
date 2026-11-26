# Decisions

Each entry records one decision: the context, the decision, the alternatives
rejected and why, and the consequences. `make lint` checks the entry format.
Measurements cited as "the spike" are the results of a throwaway prototype,
posted as a
[comment on issue #1](https://github.com/Carpenter-Softworks/Nucleus/issues/1#issuecomment-6390447321).

## ADR-1: Java 25 on the JVM with Quarkus

**Status:** Accepted
**Date:** 2026-10-24

Context: the product is a long-running single-node streaming proxy. It moves
multi-GB blobs, answers Range and HEAD requests, and parses untrusted XML, JSON
and archives from the internet while fetching from outbound URLs that an
attacker may try to steer. Startup time and memory matter little; correctness
and supply-chain safety matter most. The project is developed with heavy LLM
assistance behind strict gates, up to 100% line and branch coverage, so a
language is only as good as the gates that exist for it.

Decision: Java 25 on the JVM, with Quarkus for HTTP, security and database
access. devkit's Java gates (Error Prone with NullAway, SpotBugs, PMD,
Checkstyle, JaCoCo branch coverage, a license allowlist, plugin checksums)
already exist and apply as they are. Virtual threads let straightforward
blocking code scale ([ADR-10](#adr-10-the-threading-model-blocking-code-on-virtual-threads)).

Rejected:

- **C++.** Modern C++ removes most leaks and double frees, but not dangling
  views and iterators, use-after-move, data races or signed-overflow undefined
  behaviour. A parser of attacker-controlled input is where those bugs live,
  and a test shows the presence of a bug, never its absence. Memory safety
  would depend on sanitizer builds and continuous fuzzing; the JVM gives it by
  default, short of JNI, `Unsafe` or the foreign-function API, which a gate can
  ban.
- **Rust.** Memory safety and data-race freedom are by construction, but
  branch coverage was an unstable, nightly-only feature in its coverage tooling
  when this was decided, devkit has no Rust support, and cancelling async work
  halfway through a cache write is easy to get wrong.
- **Go.** The best fit for the domain (the container ecosystem and its HTTP
  library), but `go test -cover` measures statements rather than branches, so a
  100% branch target needs third-party tooling, and devkit has no Go support.
- **Python.** Workable, and devkit gates it fully. It loses on Java's tooling
  and runtime guarantees rather than on a defect: a streaming proxy needs the
  async-versus-blocking discipline that Python makes the author's problem, and
  type checking is weakest at exactly the protocol boundaries.
- **A native image** is not a design driver: JaCoCo measures the JVM run, and a
  native build needs its own test pass. It can be revisited later.

Consequences: the Quarkus platform follows its latest LTS line, not the newest
stable release, so `make check` reporting a newer release as outdated is
expected. A deployment needs a JVM. A single static binary for tiny hardware is
not a goal; that need would reopen this decision toward Go.

## ADR-2: Build rather than adopt or stitch together existing tools

**Status:** Accepted
**Date:** 2026-10-24

Context: the motivation is Sonatype Nexus Community Edition's usage caps
(components and requests per day) and its closed-source, vendor-controlled free
tier. Alternatives were surveyed on 2026-10-24: Nexus CE, the open-source Nexus
core, Pulp, Artipie, Repsy, ProGet Free, Gitea and Forgejo (no proxy registries),
and a combination of single-format tools (Reposilite for Maven, devpi or proxpi
for PyPI, Verdaccio for npm, zot for OCI, a feed downloader for NVD, nginx for
p2).

Decision: build nucleus. Existing tools cover five of the six formats between
them, but:

- **No open-source tool proxies p2.** Nexus's closed binary is the only proxy
  anywhere, and it proxies only.
- **None offers the security layer** that is the point of the project: a
  record of which bytes an artifact had that survives eviction, no complete
  file reaching a client before it is verified ([ADR-11](#adr-11-the-fill-mechanism-hold-back-and-abort-with-a-length-on-every-response)),
  per-upstream SSRF and token-origin rules, operator deny rules, one audit
  trail. Adding that in front of the combined tools means writing a policy
  proxy that understands all six protocols, which is most of this project.
- **The gates cover code the project owns.** A combined setup is several
  foreign codebases with separate runtimes, configuration and auth models, and
  separate vulnerability streams.
- **Free tiers a vendor controls** (Nexus CE, ProGet Free) are the risk being
  escaped.

Rejected:

- **Nexus CE and ProGet Free:** the vendor sets the tier's terms.
- **Nexus core:** Maven, raw and apt only; its npm, PyPI and Docker support and
  its PostgreSQL support are CE-only.
- **Pulp:** several services plus PostgreSQL, configured over a REST API, no p2
  or NVD, and usage analytics on by default.
- **Artipie:** dormant (last release 2024-04), which rules it out for a
  security-first tool.
- **Repsy:** hosted repositories only, not a pull-through cache.
- **Embedding the CNCF Distribution registry or zot for OCI:** separate file
  store and garbage collection, separate auth, and a Go binary outside the
  gates. The risk it would address, an LLM reimplementing the protocol, is
  better handled with real clients and the conformance suite; the pull-only
  part of the protocol is small.

Consequences: the project is ambitious for a single maintainer. Existing tools
stay useful as a stopgap, as precedents to borrow (Reposilite's group
allow/block lists, zot's prefix mapping per upstream) and as reference
behaviour for differential tests.

**Premise:** the survey of 2026-10-24 found no open-source p2 proxy and no tool
offering the security layer above, and found Nexus CE's p2 support to be
proxy-only.
**Guard:** watcher: before relying on this entry to reject an adoption
proposal, re-check whether an open-source p2 proxy exists and whether any tool
now covers the security layer; if either does, reopen this decision.

## ADR-3: A repository of its own, published under Carpenter Softworks

**Status:** Accepted
**Date:** 2026-10-24

Context: nucleus is an open-source product, published on GitHub by Carpenter
Softworks. An existing multi-package Python repository, with its own release
and gate setup, could in principle have hosted it.

Decision: nucleus lives in its own repository, with its own license decision
([ADR-4](#adr-4-agpl-30-or-later)) and its own conventions, restated here as
the project's own rather than inherited.

Rejected:

- **Joining that Python repository.** devkit gives a project `common.mk` and
  exactly one language file, and that repository already uses the Python one,
  so a Java module cannot join it under devkit. Even for a Python
  implementation, that repository releases all its packages under one shared
  version, so every registry fix would re-release unrelated tools and the other
  way round, and the code it could share is small next to the registry's main
  job, serving artifacts.

Consequences: branding comes from the organisation and the issue and mirror
setup, not from a shared repository. The name `nucleus` and the coordinates in
[ADR-8](#adr-8-maven-coordinates-and-java-packages) were chosen for the
repository. The name collides with an unrelated Java persistence library
(groupId `org.datanucleus`) and a security vendor, which the `de.vptr`
coordinates and the organisation prefix mitigate for artifacts but not for a
bare image name; that is a known cost.

## ADR-4: AGPL-3.0-or-later

**Status:** Accepted
**Date:** 2026-10-24 (AGPL); 2026-11-26 ("or later")

Context: nucleus is a network service that the maintainer wants to publish
openly, with a single copyright holder.

Decision: AGPL-3.0-or-later. The `LICENSE` file holds the unmodified GNU AGPL
v3 text; "or later" is stated in the README and the POM license metadata.

Alternatives: no other license was weighed; AGPL is the license that fits a
network server. "Or later" over "only" is the maintainer's choice, with no
further rationale recorded.

Consequences: a sole copyright holder can relicense future versions; only past
releases stay AGPL, so the license alone does not remove the rug-pull worry
operators have with single-maintainer projects. What is structurally credible
is the product category: a pull-through cache has almost no lock-in, because
everything in it can be fetched again from upstream and clients reach it
through their ordinary mirror settings.

## ADR-5: Pull-through only

**Status:** Accepted
**Date:** 2026-10-24

Context: Nexus-class products also host a team's own packages. A homelab or
small team can host those in a general package registry such as Gitea.

Decision: nucleus is a pull-through cache only. Hosted repositories (publish,
upload, deploy) are out of scope, tracked as issue #38 with what would bring
them back in scope. Full offline mirroring is a different product and is
likewise not built.

Rejected: hosted repositories in the first version, which would add OCI
chunked push, npm publish, twine upload, Maven deploy and most of the write-side
authentication surface.

Consequences: clients only send GET and HEAD, and the only writes are cache
fills the server starts itself. Everything stored can be re-fetched, which is
what allows configuration to be a read-only file
([ADR-6](#adr-6-configuration-as-a-read-only-file-postgresql-for-the-cache-index)).
Eviction and disk quotas are therefore part of the first version: without a
component cap, disk is the only limit and Docker layers fill it fastest. Serving
stale content during an upstream outage is a core property. Where one URL must
serve both a hosted registry and public packages, that needs a group repository
with namespace routing, a security feature rather than an option.

## ADR-6: Configuration as a read-only file; PostgreSQL for the cache index

**Status:** Accepted
**Date:** 2026-10-24

Context: because everything cached can be re-fetched ([ADR-5](#adr-5-pull-through-only)),
no durable state needs to be authored through the application.

Decision: configuration lives in a file mounted read-only. The cache index
(what is stored, access times for eviction, the record of which bytes an
artifact had) lives in PostgreSQL. Blob
files are stored on disk under their digest and renamed into place atomically
after verification.

Rejected:

- **Configuration in the database,** edited over HTTP: it would make an HTTP
  endpoint able to change configuration. With a file, none can, and the web UI
  shrinks to read-only status and browsing.
- **An embedded index (H2, SQLite, or a homegrown file-based index):** each
  brings a cost, such as SQLite's JDBC driver shipping native code. Embedded
  wins only if a single-container deployment matters for adoption; that is
  recorded as out of scope, to be measured before it is reopened.

Consequences: PostgreSQL is a deployment requirement. Eviction queries are
plain SQL. The trusted reverse proxy
([ADR-12](#adr-12-serving-cache-hits-project-range-code-no-pathpart-over-http2-tls-at-a-reverse-proxy))
and the public base URL are configuration, never taken from a request.

## ADR-7: Qute and vanilla JavaScript instead of Vaadin

**Status:** Accepted
**Date:** 2026-10-24

Context: the web UI is read-only status and browsing, off the critical path of
a security product.

Decision: server-side Qute templates with vanilla JavaScript, under a strict
Content-Security-Policy with no inline scripts.

Rejected: Vaadin. It brings an npm and Vite toolchain, with its own dependency
pins to manage, into a product whose UI does not justify that supply-chain
surface.

Consequences: no frontend package manager in the project. Cached HTML or SVG
must never run in the UI's origin.

## ADR-8: Maven coordinates and Java packages

**Status:** Accepted
**Date:** 2026-10-24

Decision: groupId `de.vptr`; artifactIds `nucleus-parent`, `nucleus-core` and
`nucleus-app`; Java packages under `de.vptr.nucleus`.

Context and consequences: `de.vptr` is the maintainer's existing groupId, the
parent POM inherits devkit's `de.vptr.devkit` parent, and devkit's NullAway
default annotates the groupId's packages, so `de.vptr.nucleus` is covered
without further configuration. The project name `nucleus` is settled; see
[ADR-3](#adr-3-a-repository-of-its-own-published-under-carpenter-softworks) for
the name collisions.

`de.vptr` was chosen for consistency with the maintainer's other projects; no
alternative coordinate was weighed.

## ADR-9: Two modules, core without a framework

**Status:** Accepted
**Date:** 2026-10-24

Decision: `core` is plain Java with no Quarkus or Vert.x dependency; `app` is
the Quarkus application and depends on `core`.

Reason: protocol logic, parsing and verification are where the correctness
risk lies, and they benefit most from fast tests without a framework. A Maven
module cannot use what it does not depend on, so the module graph enforces the
boundary with no extra tool, whereas inside one module only a bytecode test
could police it. Mutation testing also runs on `core` without Quarkus.

Consequences: framework wiring, REST endpoints and database access live in
`app`; `core` exposes plain interfaces to them. The threading fence in
[ADR-10](#adr-10-the-threading-model-blocking-code-on-virtual-threads) is an
`app` test because that is where reactive types could arrive.

## ADR-10: The threading model, blocking code on virtual threads

**Status:** Accepted
**Date:** 2026-10-24

Context: the core mechanism is one upstream download that fills the cache
while several clients read the growing file, each at its own pace, and the
download continues if the client that started it disconnects.

Decision: blocking code on `@RunOnVirtualThread`. No Mutiny, and no reactive
types in the project's own code; Quarkus still runs on Vert.x underneath. The
fence is `ThreadingModelTest`, an ArchUnit rule on bytecode dependencies that
fails when project code depends on `io.smallrye.mutiny` or `io.vertx.mutiny`.
It is proven by a fixture that reaches Mutiny through `var` without an import.
An import ban would miss fully qualified names and types inferred through
`var`. Vert.x's core API is not banned: where Quarkus REST needs it, it is
called from a virtual thread and awaited.

Reason, from the measurements of the spike (Quarkus 3.40.1, JDK 25, 24 cores
shared with the load client, so the server's ceilings were not reached):

- With 300 concurrent 1 GiB readers, virtual threads gave a time to first byte
  at p99 of 203 ms with 68 platform threads. On Quarkus worker threads the pool
  caps near 200, and the other 100 clients waited about 17 s.
- Virtual threads cost about 6% more CPU (118.0 against 111.8 CPU-seconds for
  200 readers). Neither a tuned worker pool nor a reactive variant was
  measured, so the comparison is against the default pool.
- `jdk.VirtualThreadPinned` at a 0 ms threshold had no events in 21 of 22
  steady-state windows (one JFR-internal event in the other); startup bursts of
  169 to 277 events come from class loading and first-use code. 25 carriers
  were in use under load.
- Hand-off from the event loop to the handler took 30 to 52 µs at p50 and 0.2
  to 0.7 ms at p99.
- Straight-line blocking code keeps the control flow that the project's
  static-analysis gates and branch coverage see. Retry, timeout and fallback
  inside reactive library operators are invisible to them. A blocking write
  waits for the socket, so TCP flow control gives backpressure without
  machinery.
- A mix of two models is where code blocks an event loop by mistake.

Rejected:

- **Mutiny and Vert.x for the data path.** It wins with tens of thousands of
  mostly idle connections, which is not this scale. Sharing one upstream
  stream between readers at different speeds would have to run at the slowest
  reader's pace or buffer, so the same disk-backed reader would be built anyway.
- **Mutiny where beneficial.** The boundary between two models is where
  mistakes happen.
- **Quarkus worker threads,** for the pool cap above.

Consequences: a test failing on `jdk.VirtualThreadPinned` after a warm-up
window is possible, though it does not detect a virtual thread hogging a
carrier with CPU work. Reopen if the scale target becomes thousands of
concurrent connections, or if a needed Quarkus extension exists only in a
reactive form.

## ADR-11: The fill mechanism, hold-back and abort, with a length on every response

**Status:** Accepted
**Date:** 2026-10-24 (the Content-Length rule: 2026-10-25)

Context: a client that reads a growing file receives bytes before the digest is
known, but no client may end up with a complete unverified file. Verifying
first gives the first client of a multi-GB layer nothing until the whole
download is done, and may trip client idle timeouts. The spike measured the
first byte of a 1 GiB fill at 6 to 68 ms with hold-back and 10.3 to 11.8 s
verifying first.

Decision:

- A streaming fill holds back the final bytes (64 KiB in the spike) until the
  digest verifies. On a mismatch it aborts the connection, so nothing complete
  is delivered and nothing is published.
- **A response always carries `Content-Length`.** For an artifact it comes from
  the upstream's `Content-Length`; for rewritten metadata, from the project's
  own spooled copy. Hold-back with abort is used only on such responses.
- **An upstream response without a length is verified first:** downloaded
  completely, verified, then served with a length.

Reason, from the spike and
[the decision on issue #4](https://github.com/Carpenter-Softworks/Nucleus/issues/4#issuecomment-6390488106):
with a length, the abort works for every client tried. Maven 3.9.9 exits 1 and
stores nothing (its checksum policy set to `ignore`, so the failure comes from
the abort), curl exits 18 on HTTP/1.1 and 92 on HTTP/2, the JDK client throws,
and the upstream sees no retry. Without a length (chunked), an abort can
silently become a success: Vert.x's `HttpServerResponse.close()` only schedules
the close and Quarkus REST then calls `end()`, which writes the terminating
chunk. 19 of 20 raw captures ended with it, and Maven reported BUILD SUCCESS
with a truncated jar in 3 of 3 runs. Quarkus REST's `StreamingOutput` behaves
the same way (10 of 20; Maven accepted a truncated jar in 1 of 3).

Rejected:

- **Verify first for everything.** First-byte latency of seconds to minutes on
  large artifacts.
- **Waiting on `ConnectionBase.closeFuture()`.** It makes chunked aborts work
  (0 of 20 ended cleanly; Maven failed 3 of 3), but through an internal Vert.x
  class that an upgrade could break without notice, and the failure would be a
  security one: truncated artifacts accepted.

Consequences: a regression test pins that an aborted response never ends
cleanly, with neither a complete body nor a terminating chunk. Every supported
client must be shown to treat a cut-off response as a failed download; a format
whose client does not falls back to verify-first. Also measured: 200
concurrent misses of one 1 GiB artifact caused one upstream GET with all 200
bodies matching; a disconnecting trigger client does not stop the fill; slow
readers do not slow the others; corrupt and truncated upstreams aborted every
connection, published nothing and left no temp files. Not covered by the
spike: Maven's resume with Range, crash safety, and descriptor or thread leaks
after disconnect storms.

## ADR-12: Serving cache hits, project Range code, no PathPart over HTTP/2, TLS at a reverse proxy

**Status:** Accepted
**Date:** 2026-10-24

Context: Quarkus REST's plain `Path` response handles a single range, but it
never returns 416 (an unsatisfiable range gets 200 with the full body), sends
no `Accept-Ranges`, and answers HEAD with 200 and no headers at all, not even
`Content-Length`
([issue #5](https://github.com/Carpenter-Softworks/Nucleus/issues/5#issuecomment-6390488275)).
Throughput of a cached 1 GiB file on a single stream, from the spike:

| Path | Throughput |
| --- | --- |
| Cleartext HTTP/1.1 (`sendfile`) | 9.9 GiB/s |
| TLS HTTP/1.1 | 1.2 GiB/s |
| TLS HTTP/2 via `PathPart` | 23 MiB/s (1.1 GiB/s with 8 streams) |
| TLS HTTP/2 via a plain read/write pump | 369 MiB/s |

Decision:

- Range, 416 and HEAD are project code. A `PathPart` entity with that code was
  correct in all 28 rows of the spike's matrix, on HTTP/1.1 and HTTP/2.
- `PathPart` is not used over HTTP/2: it is about 54 times slower than TLS on
  HTTP/1.1 on a single stream.
- Zero-copy sending happens only on cleartext HTTP/1.1. The baseline deployment
  therefore terminates TLS at a reverse proxy in front of nucleus, which talks
  cleartext HTTP/1.1 to it
  ([issue #22](https://github.com/Carpenter-Softworks/Nucleus/issues/22#issuecomment-6390488446)).
  That proxy is configured as trusted ([ADR-6](#adr-6-configuration-as-a-read-only-file-postgresql-for-the-cache-index)).
  If nucleus terminates TLS itself, cache hits over HTTP/2 use a plain pump, not
  `PathPart`.

Rejected: relying on the plain `Path` entity, for the gaps above.

Consequences: the zero-copy finding was read from the Vert.x source, not
observed (strace was unavailable), so it is an inference. Not yet covered and
defined elsewhere: Range on a miss (the spike answered 200 with the full body),
and Maven's resume with Range. Every response carries `Content-Length`
([ADR-11](#adr-11-the-fill-mechanism-hold-back-and-abort-with-a-length-on-every-response)).
Reusing one direct buffer per response cut server CPU by 21% against heap
buffers.

## ADR-13: 100% line and branch coverage, and what it measures

**Status:** Accepted
**Date:** 2026-10-24

Context: with LLM-assisted development a test can reach every branch without
checking anything useful, and a coverage number can read as more assurance than
it gives.

Decision: JaCoCo minimums are 100% line and 100% branch, set in the root POM,
above devkit's defaults of 90% and 80%.

What the number measures: that the project's own decision points, the bytecode
branches and lines of its own code, were executed by a test. It does not
measure:

- exception paths in general: JaCoCo does not count them as branches, so a
  `catch` or an implicit exception can be untested at 100%;
- behaviour inside libraries: a retry, timeout or fallback implemented by a
  library is invisible, and so is, for example, whether Vert.x closes a
  connection before or after it writes a terminating chunk;
- concurrency: interleavings are not branches.

Consequences: failure and concurrency behaviour needs behavioural tests that
assert outcomes, not coverage. At least:

- an aborted response never ends cleanly, with neither a complete body nor a
  terminating chunk ([ADR-11](#adr-11-the-fill-mechanism-hold-back-and-abort-with-a-length-on-every-response));
- concurrent misses of one artifact cause one upstream fetch, and a
  disconnecting trigger client does not stop the fill;
- corrupt and truncated upstreams publish nothing and leave no temp files;
- crash recovery and descriptor or thread leaks after disconnect storms, which
  the spike did not cover;
- the threading fence itself, proven by a deliberate violation
  ([ADR-10](#adr-10-the-threading-model-blocking-code-on-virtual-threads)).

Mutation testing checks that the tests assert something. PIT 1.30.1 worked on
Java 25 in the spike, including with `@QuarkusTest`; its scores there were
`core` 77% mutations killed (85% test strength) and one `app` test class 39%
(62%). The scores are not a gate yet.
