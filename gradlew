#!/bin/sh
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Gradle wrapper jar is missing. Download it from https://gradle.org or run gradle wrapper with a local Gradle 8.7 install."
  exit 1
fi
JAVA_CMD=${JAVA_HOME:+$JAVA_HOME/bin/java}
JAVA_CMD=${JAVA_CMD:-java}
exec "$JAVA_CMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
