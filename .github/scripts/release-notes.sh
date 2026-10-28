#!/usr/bin/env bash
# Prints the release notes for a version: its CHANGELOG.md section plus a short guide to the APK files.
# Usage: release-notes.sh <version> [changelog]   (version without the leading v)
set -euo pipefail

version="${1:?usage: release-notes.sh <version> [changelog]}"
changelog="${2:-CHANGELOG.md}"

section=$(awk -v v="$version" '
  /^## \[/ { if (found) exit; found = (index($0, "## [" v "]") == 1); next }
  found && /^\[[^]]+\]: / { next }
  found { print }
' "$changelog")

if [ -z "$(printf '%s' "$section" | tr -d '[:space:]')" ]; then
  echo "No entries for ${version} in ${changelog}. Rename Unreleased to [${version}] before tagging." >&2
  exit 1
fi

printf '%s\n' "$section" | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}'

cat <<EOF

## Which APK to download

| File | For |
|---|---|
| \`Sintonia-${version}-arm64-v8a.apk\` | Recent phones and Android TVs (64-bit). The right choice for most devices. |
| \`Sintonia-${version}-armeabi-v7a.apk\` | Older phones, TV boxes and sticks (32-bit). |
| \`Sintonia-${version}-x86_64.apk\` | Emulators and some Chromebooks. |
| \`Sintonia-${version}-universal.apk\` | Works on any of the above, but is larger. Use it if you are unsure. |
EOF
