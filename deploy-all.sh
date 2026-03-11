#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/docker-compose.yml}"
K8S_FILE="${K8S_FILE:-$ROOT_DIR/kubernetes.yml}"
CLUSTER_NAME="${CLUSTER_NAME:-chat-app-kind}"
MAVEN_IMAGE="${MAVEN_IMAGE:-maven:3.9-eclipse-temurin-17}"
KIND_VERSION="${KIND_VERSION:-v0.23.0}"
AUTO_YES=false

SERVICES=(
  "eureka-server"
  "api-gateway"
  "oauth"
  "chat-service"
  "profile-service"
)

K8S_IMAGES=(
  "chat-app-eureka-server:latest"
  "chat-app-api-gateway:latest"
  "chat-app-auth-service:latest"
  "chat-app-chat-service:latest"
  "chat-app-profile-service:latest"
  "chat-app-frontend:latest"
)

K8S_DEPLOYMENTS=(
  "eureka-server"
  "api-gateway"
  "frontend"
  "auth-service"
  "chat-service"
  "profile-service"
  "redis"
  "mongodb-profiles"
  "mongodb-users"
  "mongodb-messages"
)

usage() {
  cat <<'EOF'
Usage: ./deploy-all.sh [-y]

This script does all of the following:
1) Reset Docker state (stop/remove all containers, images, volumes)
2) Deploy Docker Compose MSA stack
3) Install kubectl/kind (if missing) into ~/.local/bin
4) Build service JARs with Maven Docker image
5) Build/load Kubernetes images into kind
6) Apply kubernetes.yml and wait for rollout

Options:
  -y, --yes    Run non-interactively (skip confirmation)
  -h, --help   Show this help
EOF
}

log() {
  printf '[%s] %s\n' "$(date +'%Y-%m-%d %H:%M:%S')" "$*"
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

need_docker_compose() {
  docker compose version >/dev/null 2>&1 || die "docker compose plugin is required."
}

linux_arch() {
  case "$(uname -m)" in
    x86_64|amd64) echo "amd64" ;;
    aarch64|arm64) echo "arm64" ;;
    *)
      die "Unsupported architecture: $(uname -m)"
      ;;
  esac
}

install_local_bin() {
  local name="$1"
  local url="$2"
  mkdir -p "$HOME/.local/bin"
  curl -fsSL "$url" -o "$HOME/.local/bin/$name"
  chmod +x "$HOME/.local/bin/$name"
}

ensure_kubectl() {
  if command -v kubectl >/dev/null 2>&1; then
    return
  fi
  local arch
  local stable
  arch="$(linux_arch)"
  stable="$(curl -fsSL https://dl.k8s.io/release/stable.txt)"
  log "kubectl not found. Installing kubectl ${stable} to ~/.local/bin ..."
  install_local_bin "kubectl" "https://dl.k8s.io/release/${stable}/bin/linux/${arch}/kubectl"
}

ensure_kind() {
  if command -v kind >/dev/null 2>&1; then
    return
  fi
  local arch
  arch="$(linux_arch)"
  log "kind not found. Installing kind ${KIND_VERSION} to ~/.local/bin ..."
  install_local_bin "kind" "https://kind.sigs.k8s.io/dl/${KIND_VERSION}/kind-linux-${arch}"
}

confirm_destruction() {
  if [[ "$AUTO_YES" == true ]]; then
    return
  fi
  echo "This will remove ALL Docker containers/images/volumes on this host."
  read -r -p "Continue? [y/N]: " answer
  if [[ ! "$answer" =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
  fi
}

docker_reset() {
  log "Resetting Docker (containers/images/volumes)..."
  docker compose -f "$COMPOSE_FILE" down --remove-orphans >/dev/null 2>&1 || true

  local running
  running="$(docker ps -q)"
  if [[ -n "$running" ]]; then
    # shellcheck disable=SC2086
    docker stop $running >/dev/null
  fi

  docker system prune -a --volumes -f >/dev/null
}

deploy_compose() {
  log "Deploying MSA with Docker Compose..."
  docker compose -f "$COMPOSE_FILE" up --build -d
  docker compose -f "$COMPOSE_FILE" ps
}

build_jars_with_docker() {
  log "Building service JARs in Docker (no local Java needed)..."
  mkdir -p "$HOME/.m2"
  for svc in "${SERVICES[@]}"; do
    log "  - Building ${svc}"
    docker run --rm \
      -v "$ROOT_DIR:/workspace" \
      -v "$HOME/.m2:/root/.m2" \
      -w "/workspace/${svc}" \
      "$MAVEN_IMAGE" \
      mvn -DskipTests clean package
  done
}

build_k8s_images() {
  log "Building Docker images for Kubernetes manifest..."
  docker build -t chat-app-eureka-server:latest "$ROOT_DIR/eureka-server"
  docker build -t chat-app-api-gateway:latest "$ROOT_DIR/api-gateway"
  docker build -t chat-app-auth-service:latest "$ROOT_DIR/oauth"
  docker build -t chat-app-chat-service:latest "$ROOT_DIR/chat-service"
  docker build -t chat-app-profile-service:latest "$ROOT_DIR/profile-service"
  docker build -t chat-app-frontend:latest "$ROOT_DIR/frontend"
}

recreate_kind_cluster() {
  log "Recreating kind cluster: ${CLUSTER_NAME}"
  kind delete cluster --name "$CLUSTER_NAME" >/dev/null 2>&1 || true
  kind create cluster --name "$CLUSTER_NAME" --wait 180s
}

load_images_into_kind() {
  log "Loading images into kind cluster..."
  for image in "${K8S_IMAGES[@]}"; do
    kind load docker-image "$image" --name "$CLUSTER_NAME"
  done
}

deploy_kubernetes() {
  log "Applying Kubernetes manifest..."
  kubectl apply -f "$K8S_FILE"

  log "Waiting for deployments to be ready..."
  for dep in "${K8S_DEPLOYMENTS[@]}"; do
    kubectl -n chat-app rollout status "deployment/${dep}" --timeout=240s
  done

  kubectl -n chat-app get pods,svc,pvc
}

main() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      -y|--yes)
        AUTO_YES=true
        shift
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        die "Unknown option: $1"
        ;;
    esac
  done

  need_cmd docker
  need_cmd curl
  need_docker_compose
  [[ -f "$COMPOSE_FILE" ]] || die "Compose file not found: $COMPOSE_FILE"
  [[ -f "$K8S_FILE" ]] || die "Kubernetes manifest not found: $K8S_FILE"

  docker info >/dev/null 2>&1 || die "Docker daemon is not running."

  confirm_destruction
  docker_reset
  deploy_compose

  ensure_kind
  ensure_kubectl
  export PATH="$HOME/.local/bin:$PATH"

  build_jars_with_docker
  build_k8s_images
  recreate_kind_cluster
  load_images_into_kind
  deploy_kubernetes

  cat <<EOF

Done.
- Docker MSA: docker compose -f "$COMPOSE_FILE" ps
- Kubernetes: kubectl -n chat-app get pods
- Frontend access (port-forward):
  kubectl -n chat-app port-forward svc/frontend 5173:5173
- API gateway access (port-forward):
  kubectl -n chat-app port-forward svc/api-gateway 9999:9999
EOF
}

main "$@"
