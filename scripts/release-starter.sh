#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
. "${SCRIPT_DIR}/release-functions.sh"

invoke_project_release "hdfk7-boot-starter-code-generator" "hdfk7-boot-starter-common" "hdfk7-boot-starter-discovery" "hdfk7-boot-starter-shardingsphere"
