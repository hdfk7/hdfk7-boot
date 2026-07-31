#!/bin/sh
set -eu

DEST_DIR="${DEST_DIR:-/opt/agent}"
JAR_NAME="${JAR_NAME:-aliyun-java-agent.jar}"
DEST_JAR="${DEST_DIR}/${JAR_NAME}"
DEFAULT_AGENT_DIR="${DEFAULT_AGENT_DIR:-/app/agent}"
DEFAULT_JAR="${DEFAULT_AGENT_DIR}/${JAR_NAME}"

echo "AGENT_DOWNLOAD_URL=${AGENT_DOWNLOAD_URL:-}"
echo "DEST_JAR=${DEST_JAR}"
echo "DEFAULT_JAR=${DEFAULT_JAR}"

mkdir -p "$(dirname "$DEST_JAR")"

if [ -n "${AGENT_DOWNLOAD_URL:-}" ]; then
  echo ">> Downloading agent from: $AGENT_DOWNLOAD_URL"
  wget --timeout=30 --tries=3 --no-check-certificate -O "$DEST_JAR" "$AGENT_DOWNLOAD_URL"
  echo ">> Download complete"
else
  echo ">> Using default agent jar: $DEFAULT_JAR"
  cp "$DEFAULT_JAR" "$DEST_JAR"
fi

chmod 644 "$DEST_JAR"
echo ">> Agent jar ready at $DEST_JAR"
