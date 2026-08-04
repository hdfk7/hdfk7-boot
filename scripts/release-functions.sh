#!/usr/bin/env bash

INITIAL_LOCATION="$(pwd)"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_PATH="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
BOOT_PATH="${REPOSITORY_PATH}/boot"
MAVEN_CONFIG_PATH="${REPOSITORY_PATH}/.mvn/maven.config"

get_config_line() {
    local file_source="$1"
    local prefix="$2"

    awk -v prefix="${prefix}" '
        index($0, prefix) == 1 {
            sub(/\r$/, "")
            print
            exit
        }
    ' "${file_source}"
}

REVISION_LINE="$(get_config_line "${MAVEN_CONFIG_PATH}" "-Drevision=")"
CHANGELIST_LINE="$(get_config_line "${MAVEN_CONFIG_PATH}" "-Dchangelist=")"
REVISION="${REVISION_LINE#-Drevision=}"
CHANGELIST="${CHANGELIST_LINE#-Dchangelist=}"

write_file_hash() {
    local file_source="$1"
    local algorithm="$2"
    local lower_algorithm

    lower_algorithm="$(printf '%s' "${algorithm}" | tr '[:upper:]' '[:lower:]')"

    case "${algorithm}" in
        MD5)
            if command -v md5sum >/dev/null 2>&1; then
                md5sum "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            else
                md5 -q "${file_source}" | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            fi
            ;;
        SHA1)
            if command -v sha1sum >/dev/null 2>&1; then
                sha1sum "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            else
                shasum -a 1 "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            fi
            ;;
        SHA256)
            if command -v sha256sum >/dev/null 2>&1; then
                sha256sum "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            else
                shasum -a 256 "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            fi
            ;;
        SHA512)
            if command -v sha512sum >/dev/null 2>&1; then
                sha512sum "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            else
                shasum -a 512 "${file_source}" | awk '{print $1}' | tr '[:lower:]' '[:upper:]' > "${file_source}.${lower_algorithm}"
            fi
            ;;
    esac
}

new_release_file_signature() {
    local file_source="$1"
    local algorithms=("MD5" "SHA1" "SHA256" "SHA512")
    local algorithm

    if [ -f "${file_source}" ]; then
        gpg --armor --output "${file_source}.asc" --detach-sig "${file_source}"
        for algorithm in "${algorithms[@]}"; do
            write_file_hash "${file_source}" "${algorithm}"
        done
    fi
}

copy_existing_file() {
    local file_source="$1"
    local file_destination="$2"

    if [ -f "${file_source}" ]; then
        cp "${file_source}" "${file_destination}"
    fi
}

invoke_project_release() {
    local projects=("$@")
    local project

    for project in "${projects[@]}"; do
        local project_name="${project##*/}"
        local project_root="${project%%/*}"
        local runtime_path="${BOOT_PATH}/${project}"
        local target_path="${runtime_path}/target"

        local project_revision="${REVISION}"
        local project_changelist="${CHANGELIST}"
        local project_maven_config_path="${BOOT_PATH}/${project_root}/.mvn/maven.config"
        if [ -f "${project_maven_config_path}" ]; then
            local project_revision_line
            local project_changelist_line
            project_revision_line="$(get_config_line "${project_maven_config_path}" "-Drevision=")"
            project_changelist_line="$(get_config_line "${project_maven_config_path}" "-Dchangelist=")"
            if [ -n "${project_revision_line}" ]; then
                project_revision="${project_revision_line#-Drevision=}"
            fi
            if [ -n "${project_changelist_line}" ]; then
                project_changelist="${project_changelist_line#-Dchangelist=}"
            fi
        fi
        local project_version_name="${project_revision}${project_changelist}"

        local staging_path="${target_path}/central-staging"
        local publishing_path="${target_path}/central-publishing"
        local storage_path="${staging_path}/cn/hdfk7/boot/${project_name}/${project_version_name}"
        local pom_file_name="${project_name}-${project_version_name}.pom"
        local jar_file_name="${project_name}-${project_version_name}.jar"
        local javadoc_file_name="${project_name}-${project_version_name}-javadoc.jar"
        local sources_file_name="${project_name}-${project_version_name}-sources.jar"

        cd -- "${runtime_path}"
        mvn clean deploy

        if ! command -v gpg >/dev/null 2>&1; then
            echo "no signature files were generated"
            cd -- "${INITIAL_LOCATION}"
            continue
        fi

        mkdir -p "${publishing_path}"
        mkdir -p "${storage_path}"
        local pom_path="${target_path}/.flattened-pom.xml"
        local jar_path="${target_path}/${jar_file_name}"
        local javadoc_path="${target_path}/${javadoc_file_name}"
        local sources_path="${target_path}/${sources_file_name}"

        copy_existing_file "${pom_path}" "${storage_path}/${pom_file_name}"
        copy_existing_file "${jar_path}" "${storage_path}/${jar_file_name}"
        copy_existing_file "${javadoc_path}" "${storage_path}/${javadoc_file_name}"
        copy_existing_file "${sources_path}" "${storage_path}/${sources_file_name}"

        cd -- "${storage_path}"
        new_release_file_signature "${pom_file_name}"
        new_release_file_signature "${jar_file_name}"
        new_release_file_signature "${javadoc_file_name}"
        new_release_file_signature "${sources_file_name}"

        cd -- "${staging_path}"
        zip -r "${publishing_path}/central-bundle.zip" ./*

        cd -- "${INITIAL_LOCATION}"
    done
}
