#!/usr/bin/env bash
# Builds the mod on Mac/Linux. Needs JDK 25.
set -e
GRADLE_VERSION=9.2.0
command -v java >/dev/null || { echo "Install JDK 25 first (https://adoptium.net)"; exit 1; }
if [ ! -x ".gradle-dist/gradle-$GRADLE_VERSION/bin/gradle" ]; then
  mkdir -p .gradle-dist
  curl -L "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o .gradle-dist/gradle.zip
  unzip -q -o .gradle-dist/gradle.zip -d .gradle-dist
fi
".gradle-dist/gradle-$GRADLE_VERSION/bin/gradle" build
echo "Done. Jar is in build/libs (use the one without -sources)."
