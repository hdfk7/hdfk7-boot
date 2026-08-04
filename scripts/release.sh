#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

bash "${SCRIPT_DIR}/release-parent.sh"

bash "${SCRIPT_DIR}/release-proto.sh"

bash "${SCRIPT_DIR}/release-starter.sh"
