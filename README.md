# nucleus

nucleus is a self-hosted pull-through cache for Maven, PyPI, npm, OCI/Docker
images, NVD data feeds and Eclipse p2 repositories. It is built for operators
who want one trustworthy cache in front of their build network, without the
usage caps of a vendor's free tier.

Pull-through only: everything nucleus stores can be fetched again from its
upstream, and hosting your own packages is out of scope
([ADR-5](docs/decisions.md)).

## Status

Pre-alpha. This repository is the scaffold: the build, the quality gates and
the decision record exist, and nothing runs yet.

## Building

```sh
make help    # every target
make check   # verify the toolchain
make lint    # repository gates, then the Java quality gates
make test    # unit tests, with 100% line and branch coverage enforced
```

The gates come from [devkit](https://github.com/gregor-dietrich/devkit),
pinned in `devkit.toml`. You need JDK 25, git 2.36 or later, GNU make,
`ssh-keygen` (OpenSSH 8.1 or later), bash, python3 3.11 or later, and node
22.22.2 or later with npm (the Markdown gate). The first run also needs curl,
tar and `sha256sum` or `shasum`, to fetch and verify the pinned tools. See
devkit's README for the full list.

## Documentation

- [docs/decisions.md](docs/decisions.md) records the design decisions and the
  reasoning behind them.
- Work is tracked in the
  [issue tracker](https://github.com/Carpenter-Softworks/Nucleus/issues).
- Report vulnerabilities as [SECURITY.md](SECURITY.md) describes.

## License

Copyright Carpenter Softworks. Licensed under the GNU Affero General Public
License, version 3 or (at your option) any later version
(AGPL-3.0-or-later). The full text is in [LICENSE](LICENSE).
