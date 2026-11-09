# Release preparation

The frozen first-release candidate is `dev.jbaby:shadwick:0.1.0`, targeting Wicket 11.0.0, Java 21+ and Servlet 6.1+.
The candidate version is `0.1.0`; nothing is published to Maven Central. The packaging/signing workflow
uses Java 21, and the planned release tag is `v0.1.0`.

## Migrated platform baseline

[Wicket 11](https://wicket.apache.org/versions/wicket-11.x/) requires Java 21 and Jakarta Servlet 6.1. The repository
aligns Wicket core/extensions/tester to 11.0.0, compiles for Java 21, tests on Java 21/25 and uses Java 21 for
Docker, website and release builds. The examples use Spring Boot 4.1.2 / Spring 7 / Tomcat 11. Read
[compatibility](compatibility.md#moving-from-the-pre-release-wicket-10-build) for input validation, markup IDs and
fresh-session/page-store deployment requirements. Native/Ajax forms, serialization, resource loading and strict
CSP remain part of migration verification.

Parent/child versions are frozen at `0.1.0`, the declared SCM tag is `v0.1.0`, and the output timestamp is
fixed at `2026-11-09T14:47:43Z`. The release notes and installation examples match the candidate; the Git tag
has not been created. During this freeze, changes are limited to reviewed release-blocking fixes. The metadata
gate deliberately rejects snapshots and requires Wicket 11+ / Java 21+ / Servlet 6.1+ before staging the first release.

## Frozen candidate review — 9 November 2026

The review covered the accumulated chart additions, Wicket 11 field behavior, readable lifecycle composition,
model detachment/serialization, candidate API and publication payload. The existing API baseline passed without
regeneration. Feature scope and public/protected declarations are frozen; further chart variants belong to a
later release.

Release checks now reject a binary whose embedded Maven coordinates differ from its candidate filename. Export
also removes stale managed snapshot artifacts and refuses unrelated files before deleting anything. Regression
tests cover both failures. The final Javadoc export was rebuilt from an empty generated documentation directory
to exclude pages left by earlier builds.

| Verification | Result |
| --- | --- |
| Java 21 and Java 25 release builds | 1,396 JVM tests pass on each JDK: 1,218 library and 178 examples. |
| Release tooling | All 14 Python checks pass; metadata, binary, sources and Javadoc checks pass. |
| Documentation | All 43 Node tests pass; the generated 68-page site passes its checks. |
| Packaged consumer | An external Java 21 consumer compiles against the exported JAR, renders heatmap/radial charts, round-trips its page and serves packaged CSS/JavaScript over Wicket resource URLs. |
| Signing rehearsal | The parent/library bundle passes payload/checksum checks and verification of all five detached signatures, using a disposable test key with uploads disabled. The test key was removed. |

The preceding migration run passed 249 focused Chromium/Firefox cases and a real Java 21 Docker smoke test.
Those results cover the production implementation retained by this freeze; the complete browser CI job must
still pass for the final committed candidate. Disposable-key output is not a publishable signed release.

The remaining release steps are to commit/push the candidate, obtain successful full CI for that commit,
configure real signing/Central credentials, create the tag, stage and inspect the real signed deployment, then
publish. This review did not create a Git tag, upload artifacts or publish the release.

The first full candidate CI run subsequently passed 4,743 of 4,744 browser cases. A narrow German/RTL tooltip
assertion in Firefox remains a release blocker. The complete suite now runs across nine showcase skin jobs
and four component-class shards, with the failing case checked first and diagnostic reports retained.

## Published scope

Central receives `dev.jbaby:shadwick-parent` as a POM and `dev.jbaby:shadwick` as a POM, library JAR, sources JAR
and Javadoc JAR. The `shadwick-examples` executable remains a separate demo application. Its classic Maven deploy
and Central publication are both disabled; release commands select only the parent and library reactor.

The prepared [changelog](https://github.com/danielbartl/shadwick/blob/main/CHANGELOG.md) and
[versioned release notes](https://github.com/danielbartl/shadwick/blob/main/docs/releases/0.1.0.md) describe the scope,
requirements and compatibility boundaries. Keep the candidate status until publication has been confirmed.

## Verify unsigned artifacts

During development, from a clean checkout with the current supported JDK:

```sh
./mvnw -B -Prelease -pl shadwick-core -am clean verify
python3 scripts/check-release.py collect
```

The `release` profile attaches sources and Javadoc only for the library. Javadoc keeps `all,-missing` doclint
checks; build errors and malformed links fail rather than being hidden. CI runs the full JVM suite with this
profile on Java 21 and 25 and checks the resulting archives.

`target/release/` contains the five unsigned artifacts and `SHA256SUMS`. The checker verifies the configured Java bytecode level,
the embedded Maven coordinates, automatic module name, packaged licenses/runtime/styles, source/Javadoc content
and exclusion of example/test classes. Export removes older managed candidate files and refuses unrelated files,
so the directory contains exactly five payload artifacts plus their checksum manifest. Fixed
`project.build.outputTimestamp` values make archive timestamps repeatable. Build release artifacts
with the release's minimum JDK consistently, and compare their checksums when rebuilding the same source.

## Configure signing and Central access

The manual staging workflow uses these GitHub Actions repository secrets:

| Secret | Value |
| --- | --- |
| `CENTRAL_USERNAME` | The Central Portal user-token username. |
| `CENTRAL_PASSWORD` | The corresponding token password. |
| `MAVEN_GPG_KEY` | The ASCII-armored exported OpenPGP secret signing key. |
| `MAVEN_GPG_PASSPHRASE` | The signing key's passphrase. |

Use the existing verified `dev.jbaby` namespace in Central Portal. Generate a Portal publishing token, then
configure the secrets through the repository's secure settings or `gh secret set` using stdin/file input.
Do not put key material, passphrases or token values in Git, command-line arguments, build logs or chat.
Publish the matching public key to a Central-supported public keyserver before staging; keep the private key private.

Maven GPG uses the Bouncy Castle signer, `bestPractices=true` and environment-provided key/passphrase values.
`actions/setup-java` writes the `central` server entry using environment-variable references. Local staging needs
an equivalent Maven settings entry with ID `central`; use the Portal token, not the account login password.

See [Central's requirements](https://central.sonatype.org/publish/requirements/),
[Portal Maven configuration](https://central.sonatype.org/publish/publish-portal-maven/) and
[Maven GPG signing](https://maven.apache.org/plugins/maven-gpg-plugin/sign-mojo.html).

## Exercise signing without uploading

With signing environment variables configured, create and check the bundle locally:

```sh
./mvnw -B -Prelease,central -pl shadwick-core -am deploy -DskipUpload=true
python3 scripts/check-release.py bundle --bundle target/central-publishing/central-bundle.zip
```

`skipUpload=true` builds the signed bundle without sending it to Central. `skipPublishing=true` instead excludes
a module from the bundle; it is the examples module's setting and is not the dry-run switch.

A rehearsal may use a disposable test key in a temporary directory. Such a bundle is test output and must not
be staged or announced as the real signed release. Validate detached signatures with the intended public key.

## Stage the tagged release

Commit the reviewed candidate changes to `main` and wait for the complete Build workflow, including its browser
job, to succeed for that exact commit. Review the release notes and artifact payload, then create/push the
annotated `v0.1.0` tag on that commit. These are release actions, rather than a side effect of preparing files.
Before tagging, run `python3 scripts/check-release.py metadata --tag v0.1.0` to check the final version and upgrade gate.

Run **Stage release** manually with `tag=v0.1.0` from the repository's default branch. The workflow checks that the
tag matches the POM/SCM metadata, belongs to `main`, and has a completed successful Build run. It then builds,
signs and uploads the parent/library reactor with the Central plugin, waits for validation, and retains the
signed bundle and reference files as a workflow artifact. It also prepares a draft GitHub release using the
versioned release notes and those artifacts.

The Central configuration uses `autoPublish=false`. The workflow stages a validated deployment; public publication
is a separate action in Central Portal. Inspect that deployment's coordinates, signatures and contents before
publishing. Do not rerun a successful staging job simply to obtain a second deployment; use its retained result.

The **Publish validated release** workflow can perform the publication with the existing Central repository
secrets, so credentials stay inside GitHub Actions. Run it from `main` with the staged tag and deployment UUID;
leave `publish=false` first to inspect the status. It requires successful CI for the tagged source and the staged
GitHub release bundle, checks the deployment ID and both exact Maven coordinates, and refuses an unvalidated
deployment. After inspection, run it with `publish=true`. It publishes once and waits for `PUBLISHED`; retrying a
deployment already publishing or published does not submit a second publication request.

After Central reports publication, resolve both coordinates from a fresh local Maven repository to verify
availability. Finalize the draft GitHub release from the existing tag using the versioned release notes and the retained
artifacts. Update the candidate/not-on-Central language in README, installation/compatibility/docs index, landing
page and machine-readable status together, and publish the website. Then move development to the next snapshot
version and reset the development SCM tag to `HEAD`.

## Compatibility gate

Review the candidate [compatibility contract](compatibility.md). A matching API baseline is not a guarantee that
stored sessions migrate across builds. Confirm native/Ajax validation, partial replacement and callback behavior
using the relevant tests. Do not replace the API baseline or skip checks to make a release pass.
