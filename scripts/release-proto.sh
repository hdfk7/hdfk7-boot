#!/usr/bin/env bash

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
. "${SCRIPT_DIR}/release-functions.sh"

invoke_project_release "hdfk7-boot-proto/hdfk7-boot-base-proto"
