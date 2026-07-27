#!/bin/bash
set -euo pipefail

APP_HOME="/app"

if ! command -v java >/dev/null 2>&1; then
  echo "[ERROR] java not found"
  exit 1
fi

echo "JAVA_TOOL_OPTIONS=${JAVA_TOOL_OPTIONS:-}"
echo "JAVA_OPTS=${JAVA_OPTS:-}"
echo "APP_OPTS=${APP_OPTS:-}"

jar=$(find "${APP_HOME}" -maxdepth 1 -type f -name "*.jar" ! -name "*sources.jar" ! -name "*tests.jar" | head -n 1)

if [[ -z "${jar}" ]]; then
  echo "[ERROR] cannot detect application jar"
  exit 1
fi

LOG_DIR="${LOG_HOME:-${APP_HOME}/logs}"
mkdir -p "${LOG_DIR}"
echo "log directory: ${LOG_DIR}"

echo "start application: ${jar}"

exec java \
  ${JAVA_OPTS:-} \
  ${APP_OPTS:-} \
  -jar "${jar}"
