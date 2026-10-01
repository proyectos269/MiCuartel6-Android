#!/bin/sh
#
# Gradle wrapper launcher for Mi Cuartel 6 Android
# Uses the official Gradle 8.9 wrapper JAR when it is not already present.
#
set -e

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
  mkdir -p "$APP_HOME/gradle/wrapper"
  curl -fL --retry 3 --connect-timeout 15 \
    "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar" \
    -o "$JAR"
fi

if [ -n "${JAVA_HOME:-}" ]; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="$(command -v java)"
fi

if [ -z "$JAVACMD" ] || [ ! -x "$JAVACMD" ]; then
  echo "ERROR: Java no está disponible en el entorno de compilación." >&2
  exit 1
fi

exec "$JAVACMD" \
  -Dorg.gradle.appname=gradlew \
  -classpath "$JAR" \
  org.gradle.wrapper.GradleWrapperMain "$@"
