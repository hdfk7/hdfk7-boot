#!/bin/bash

if [ -z "${BASH_VERSION:-}" ]; then
    exec bash "$0" "$@"
fi

PROJECT_NAME="${PROJECT_NAME:-}"
DEPLOYMENT_NAME="${DEPLOYMENT_NAME:-}"
NAMESPACE="${NAMESPACE:-}"
RUN_ENV="${RUN_ENV:-${env:-}}"
IMAGE_NAME="${IMAGE_NAME:-}"

declare -A KUBECONFIG_MAP
KUBECONFIG_MAP["dev"]="${KUBECONFIG_MAP_DEV:-/root/.kube/config_dev}"
KUBECONFIG_MAP["test"]="${KUBECONFIG_MAP_TEST:-/root/.kube/config_test}"
KUBECONFIG_MAP["prod"]="${KUBECONFIG_MAP_PROD:-/root/.kube/config_prod}"

declare -A KUBE_HOST_MAP
KUBE_HOST_MAP["dev"]="${KUBE_HOST_DEV:-vm5}"
KUBE_HOST_MAP["test"]="${KUBE_HOST_TEST:-vm5}"
KUBE_HOST_MAP["prod"]="${KUBE_HOST_PROD:-vm2}"

declare -A KUBE_PORT_MAP
KUBE_PORT_MAP["dev"]="${KUBE_PORT_DEV:-221}"
KUBE_PORT_MAP["test"]="${KUBE_PORT_TEST:-221}"
KUBE_PORT_MAP["prod"]="${KUBE_PORT_PROD:-220}"

KUBE_USER="${KUBE_USER:-root}"
KUBECTL_BIN="${KUBECTL_BIN:-kubectl}"

error_exit() {
    printf '[%s] [ERROR] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1" >&2
    exit 1
}

info_echo() {
    printf '[%s] [INFO] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1"
}

require_var() {
    local name="$1"
    local value="$2"
    [ -n "${value}" ] || error_exit "缺少必要参数: ${name}"
}

shell_quote() {
    printf '%q' "$1"
}

require_var "RUN_ENV" "${RUN_ENV}"
require_var "IMAGE_NAME" "${IMAGE_NAME}"
require_var "DEPLOYMENT_NAME" "${DEPLOYMENT_NAME}"
require_var "NAMESPACE" "${NAMESPACE}"

KUBECONFIG_FILE="${KUBECONFIG_FILE:-${KUBECONFIG_MAP[${RUN_ENV}]:-}}"
KUBE_HOST="${KUBE_HOST:-${KUBE_HOST_MAP[${RUN_ENV}]:-}}"
KUBE_PORT="${KUBE_PORT:-${KUBE_PORT_MAP[${RUN_ENV}]:-}}"

require_var "KUBECONFIG_FILE(${RUN_ENV})" "${KUBECONFIG_FILE}"
require_var "KUBE_HOST(${RUN_ENV})" "${KUBE_HOST}"
require_var "KUBE_PORT(${RUN_ENV})" "${KUBE_PORT}"

info_echo "集群发布环境：${RUN_ENV}"
info_echo "集群命名空间：${NAMESPACE}"
info_echo "目标工作负载：deployment/${DEPLOYMENT_NAME}"
info_echo "目标镜像：${IMAGE_NAME}"
info_echo "远端执行目标：${KUBE_USER}@${KUBE_HOST}:${KUBE_PORT}"
info_echo "Kubeconfig路径：${KUBECONFIG_FILE}"
info_echo "开始更新工作负载镜像"

REMOTE_COMMAND=$(printf '%s --kubeconfig=%s --insecure-skip-tls-verify=true set image deployment.apps/%s %s -n %s' \
    "$(shell_quote "${KUBECTL_BIN}")" \
    "$(shell_quote "${KUBECONFIG_FILE}")" \
    "$(shell_quote "${DEPLOYMENT_NAME}")" \
    "$(shell_quote "runtime=${IMAGE_NAME}")" \
    "$(shell_quote "${NAMESPACE}")")

ssh -p "${KUBE_PORT}" "${KUBE_USER}@${KUBE_HOST}" \
    "bash -lc $(shell_quote "${REMOTE_COMMAND}")" \
    || error_exit "工作负载镜像更新失败，请检查kubeconfig权限、命名空间或deployment名称"

info_echo "工作负载镜像更新完成：project=${PROJECT_NAME}, namespace=${NAMESPACE}, deployment=${DEPLOYMENT_NAME}"
exit 0
