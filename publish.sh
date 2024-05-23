#!/bin/bash
echo "Cleaning project..."
./gradlew clean >> publish_log.txt
if [[ $? -eq 0 ]]; then
  echo "Building project..."
  ./gradlew assembleRelease >> publish_log.txt
  if [[ $? -eq 0 ]]; then
    echo "Publishing project..."
    ./gradlew publish >> publish_log.txt
    if [[ $? -eq 0 ]]; then
      echo "Done."
    fi
  fi
fi
