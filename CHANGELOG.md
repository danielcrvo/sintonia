# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- On Android TV, pressing Back in the player while the controls are showing hides them, and a second press leaves the player.

### Fixed

- On Android TV, the screen shown while a list loads fits the screen. It uses two columns and keeps a margin from the screen edge.
- On Android TV, the welcome and add list screens keep a margin from the top and bottom edges.
- On Android TV, the featured card on Home and the automatic update row in Settings each have a single focus stop.
- On Android TV, My lists starts on the active list, the remove dialog starts on Cancel, the category tabs start on the selected tab, and the Retry button on the movie and series details has focus when the details fail to load.
- On Android TV, the not found screens and the player error have no Back button, because the Back key on the remote already leaves them.
- The next episode card in the player shows the season and episode code in the app language, such as S1 · E5 in English.
- The language picker shows Portuguese as Português (Brasil) in every app language.
- The size of a picked list file follows the device formatting and language.
- The progress bar of the current programme in the player moves while you watch a live channel.
- The player controls hide once the stream starts playing, even when it was still loading after the usual four seconds.
- On Android TV, a focused primary button now shows a visible focus ring.

## [1.0.0] - 2026-10-28

### Added

- Complete rewrite of Sintonia in Jetpack Compose, for phones and Android TV.
- Lists from Xtream Codes, an M3U link or an M3U file, with a streamed import that reports progress and can be cancelled. A reimport replaces the old content only when it finishes.
- Live TV with categories, favorites, number or name sorting, channel zapping and a programme guide. On TV, the guide is a time grid.
- Movies and series with details, seasons, resume and an automatic next episode.
- Search across channels, movies and series.
- Rename and hide categories.
- Parental control with a 4-digit PIN, blocked categories and a growing lockout after repeated wrong PINs.
- Several lists, with automatic background refresh.
- Audio and subtitle language preferences, and a video fit option in the player.
- Light, dark and automatic themes. Portuguese (Brazil), English and Spanish, switched without a restart.
- Full D-pad navigation on Android TV.

[Unreleased]: https://github.com/danielcrvo/sintonia/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/danielcrvo/sintonia/releases/tag/v1.0.0
