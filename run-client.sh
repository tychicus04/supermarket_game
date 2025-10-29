#!/usr/bin/env zsh
# Run compiled SupermarketClient
# Usage: ./run-client.sh

BASEDIR="$(dirname -- "$(realpath "$0")")"
CLASSES="$BASEDIR/SupermarketClient/build/classes"

if [ ! -d "$CLASSES" ]; then
  echo "Error: compiled classes not found at $CLASSES"
  exit 1
fi

# If you need JavaFX, set JAVAFX_LIB to your JavaFX SDK lib directory and
# uncomment the JAVA_OPTS line below.
# Example: export JAVAFX_LIB="/Users/you/Downloads/javafx-sdk-17/lib"
# JAVA_OPTS="--module-path $JAVAFX_LIB --add-modules javafx.controls,javafx.fxml"
JAVA_OPTS="${JAVA_OPTS:-}"

echo "Running client.Main with classpath: $CLASSES"
java $JAVA_OPTS -cp "$CLASSES" client.Main

