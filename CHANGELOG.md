# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- On Android TV, the screen shown while a list loads no longer gets cut off. It now uses two columns and keeps a margin from the screen edge.
- On Android TV, the welcome and add list screens also keep a margin from the top and bottom edges.
- The next episode card in the player now shows the season and episode code in the app language, so English shows S1 · E5 instead of T1 · E5.
- On Android TV, the not found screens and the player error no longer show a Back button. The remote's Back key already leaves them.
- On Android TV, the featured card on Home is a single focus stop instead of two overlapping ones.

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
