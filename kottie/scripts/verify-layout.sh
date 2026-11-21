#!/usr/bin/env bash
# Ensures every project in settings.gradle.kts has a directory + build file.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> verify-layout"

test -f settings.gradle.kts
test -f build.gradle.kts
test -f gradlew
test -f gradle/wrapper/gradle-wrapper.properties
test -f gradle/libs.versions.toml

missing=0
tmp=$(mktemp)
grep '"' settings.gradle.kts | grep -E 'kottie-|examples:' > "$tmp" || true

while IFS= read -r line; do
  mod=$(echo "$line" | sed -n 's/.*"\([^"]*\)".*/\1/p')
  [ -z "$mod" ] && continue
  dir=$(echo "$mod" | tr ':' '/')
  if [ ! -d "$dir" ]; then
    echo "MISSING DIR: $dir (from include $mod)"
    missing=1
    continue
  fi
  if [ ! -f "$dir/build.gradle.kts" ] && [ ! -f "$dir/build.gradle" ]; then
    echo "MISSING BUILD FILE: $dir/build.gradle.kts"
    missing=1
  else
    echo "  ok $mod -> $dir"
  fi
done < "$tmp"
rm -f "$tmp"

for path in \
  kottie-api/src/main/kotlin \
  kottie-core/src/main/kotlin \
  kottie-testkit/src/test/kotlin \
  kottie-adapter-paper/src/main/kotlin
do
  if [ ! -d "$path" ]; then
    echo "MISSING SOURCE ROOT: $path"
    missing=1
  fi
done

if [ "$missing" -ne 0 ]; then
  echo "Layout verification FAILED"
  exit 1
fi
echo "Layout verification passed"
