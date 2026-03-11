#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-$ROOT_DIR/docker-compose.yml}"
AUTO_YES=false

usage() {
  cat <<'EOF'
Usage: ./reset-and-run.sh [-y]

This script:
1) Stops and removes all running Docker containers
2) Prunes all Docker images, volumes, and networks not in use
3) Rebuilds and starts this project's Docker Compose stack

Options:
  -y, --yes    Run non-interactively
  -h, --help   Show this help
EOF
}

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
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

if [[ "$AUTO_YES" != true ]]; then
  echo "This will remove ALL Docker containers/images/volumes on this host."
  read -r -p "Continue? [y/N]: " answer
  if [[ ! "$answer" =~ ^[Yy]$ ]]; then
    echo "Cancelled."
    exit 0
  fi
fi

docker info >/dev/null 2>&1 || {
  echo "Docker daemon is not running." >&2
  exit 1
}

docker compose -f "$COMPOSE_FILE" down --remove-orphans >/dev/null 2>&1 || true

running="$(docker ps -q || true)"
if [[ -n "${running}" ]]; then
  # shellcheck disable=SC2086
  docker stop ${running} >/dev/null
fi

docker system prune -a --volumes -f >/dev/null
docker compose -f "$COMPOSE_FILE" up --build -d
docker compose -f "$COMPOSE_FILE" ps

echo
echo "Done."
echo "- Frontend SPA: http://localhost:5173"
echo "- API Gateway:  http://localhost:9999"

