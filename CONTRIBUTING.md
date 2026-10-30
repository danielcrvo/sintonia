# Contributing to Sintonia

Thanks for taking the time to contribute. Sintonia is small on purpose: few screens, one user, no backend, no login and no analytics. Changes beyond that scope are unlikely to be accepted, so please open an issue and talk it through before you start on anything large.

By participating, you agree to follow the [code of conduct](CODE_OF_CONDUCT.md). By contributing, you agree that your work is released under the [GPL-3.0-or-later](LICENSE), like the rest of the project.

## Reporting bugs

Use the [bug report form](https://github.com/danielcrvo/sintonia/issues/new?template=bug.yml). Include your device, Android version, the Sintonia version (Settings shows it) and the steps that trigger the problem. Never paste playlist links, usernames or passwords. Replace them with placeholders.

## Suggesting features

Use the [idea form](https://github.com/danielcrvo/sintonia/issues/new?template=idea.yml). Check the open issues first to avoid duplicates.

## Setting up

You need JDK 17 or newer and the Android SDK. Clone the repository, open it in Android Studio or build from the command line:

```bash
./gradlew assembleDebug
```

The `scripts` folder has a mock Xtream server (`mock-xtream-server.mjs`, username and password `demo`) and a sample playlist generator (`generate-sample-playlist.mjs`), so you do not need a real provider.

## Project layout

The app is a single module (`:app`) with a package per feature.

```
core/        designsystem, data, database, datastore, network, player, parental, model, di, common
feature/<x>/ ui (Screen, Route, ViewModel) and domain, when there is real logic
navigation/  destinations, nav host and the main tabs
```

Each screen has three parts. The `Screen` is stateless and takes a `UiState` and lambdas. The `Route` gets the `ViewModel` and handles effects. The `ViewModel` exposes a `StateFlow<UiState>`, takes events as `onXxx()` functions and sends one-off effects through a `Channel`.

## Rules

* `feature` depends on `core`, never on another `feature`. A screen never imports a repository or a database class: only its route and view model do. It may use plain value types such as sort options and language enums.
* Outside `core/designsystem`, do not use `Color(0x…)`, `.dp` or `.sp` literals, `fontSize` or Material 3. Read everything from `SintoniaTheme`.
* Do not write literal text in composables. Use `stringResource` and keep `values` (pt-BR), `values-en` and `values-es` in sync. A test checks keys, placeholders and plurals.
* Phone and TV share one set of screens. Every screen with focusable items has to work with the D-pad alone.
* Do not add a dependency that is not in `gradle/libs.versions.toml` without opening an issue first.
* Sample data is always fictional. Never commit lists, provider URLs, usernames or passwords.
* Code, comments, commits and docs are in English. Comments are rare, one line, and explain a non-obvious why. Prefer a better name over a comment.
* The app must not crash on I/O, network or parser errors. Use typed errors, and keep state across rotation and process death.

## Before opening a pull request

Run the same checks CI runs. Everything has to pass:

```bash
./gradlew ktlintCheck detekt lintDebug testDebugUnitTest assembleDebug
```

CI also builds the release variant with `./gradlew assembleRelease`, so run that too when you touch Gradle, R8 rules or the manifest.

Add tests for behavior you change, and a screenshot test for any new screen. Add a line to the `Unreleased` section of [`CHANGELOG.md`](CHANGELOG.md) when the change is visible to users.

## Commits and pull requests

Write commits in English with [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `test:`, `docs:`, `refactor:`, `perf:`), one commit per logical unit. Keep pull requests focused, and fill in the pull request template.
