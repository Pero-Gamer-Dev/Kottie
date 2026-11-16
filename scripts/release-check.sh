#!/usr/bin/env bash
# Release gate for Kottie 2.0+
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "==> Layout"
bash scripts/verify-layout.sh

echo "==> Version"
grep -E 'version\s*=' build.gradle.kts | head -1

echo "==> Wrapper"
test -x gradlew || chmod +x gradlew

echo "==> Build + test"
./gradlew clean build :kottie-testkit:test --no-daemon

echo "==> Publish library modules to Maven Local"
./gradlew publishToMavenLocal --no-daemon

echo "==> Required docs"
for f in README.md \
  docs/compatibility.md \
  docs/DEFINITION_OF_DONE.md \
  docs/migration-guide.md \
  docs/publishing.md \
  docs/architecture.md
do
  test -f "$f" && echo "  ok $f"
done

echo "==> Sample coordinates"
echo "  io.kottie:kottie-api:$(grep -E 'version = "' build.gradle.kts | head -1 | sed 's/.*"\([^"]*\)".*/\1/')"

echo "==> Release check passed"
