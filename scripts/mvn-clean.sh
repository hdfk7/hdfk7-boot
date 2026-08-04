#!/usr/bin/env bash

INITIAL_LOCATION="$(pwd)"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_PATH="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
BOOT_PATH="${REPOSITORY_PATH}/boot"

PROJECTS=(
    "hdfk7-boot-dependencies"
    "hdfk7-boot-parent"
    "hdfk7-boot-proto/hdfk7-boot-base-proto"
    "hdfk7-boot-starter-code-generator"
    "hdfk7-boot-starter-common"
    "hdfk7-boot-starter-discovery"
    "hdfk7-boot-starter-shardingsphere"
)

for PROJECT in "${PROJECTS[@]}"; do
    RUNTIME_PATH="${BOOT_PATH}/${PROJECT}"

    cd -- "${RUNTIME_PATH}"
    mvn clean

    cd -- "${INITIAL_LOCATION}"
done
