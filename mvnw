#!/bin/bash
# Maven wrapper — garante Java 21 independente do JAVA_HOME global
export JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)

if [ -z "$JAVA_HOME" ]; then
  echo "❌ Java 21 não encontrado. Instale com: brew install --cask microsoft-openjdk21"
  exit 1
fi

exec mvn "$@"
