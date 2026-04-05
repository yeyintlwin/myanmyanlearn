#!/usr/bin/env bash
# Run on the Lightsail Ubuntu host (SSH as ubuntu). Strips a full git clone down to
# deploy-only files, keeps .env, preserves application config under config/.
#
# Usage:
#   curl -fsSL .../scripts/lightsail-setup.sh | bash -s -- ~/myanmyanlearn
#   bash scripts/lightsail-setup.sh [APP_DIR]
# APP_DIR defaults to $HOME/myanmyanlearn
#
# Requires: docker with Compose v2 (docker compose), curl.
# Set DOCKER_IMAGE in .env (e.g. ghcr.io/yeyintlwin/myanmyanlearn:latest).

set -euo pipefail

APP_DIR="${1:-$HOME/myanmyanlearn}"
APP_DIR="$(cd "$APP_DIR" && pwd)"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ "$SCRIPT_DIR" == "$APP_DIR" ]] || [[ "$SCRIPT_DIR" == "$APP_DIR"/* ]]; then
  TMP="$(mktemp /tmp/lightsail-setup.XXXXXX.sh)"
  cp "${BASH_SOURCE[0]}" "$TMP"
  chmod +x "$TMP"
  exec bash "$TMP" "$APP_DIR"
fi

REPO_RAW="${LIGHTSAIL_REPO_RAW:-https://raw.githubusercontent.com/yeyintlwin/myanmyanlearn/main}"

cd "$APP_DIR"

if [[ ! -f .env ]]; then
  echo "Missing .env in $APP_DIR — create it first (MYSQL_*, DOCKER_IMAGE)." >&2
  exit 1
fi

echo "Stopping existing stack (if any)..."
docker compose down 2>/dev/null || true

BACKUP="$(mktemp -d /tmp/myanmyanlearn-backup.XXXXXX)"
cp -f .env "$BACKUP/.env"
mkdir -p "$BACKUP/config"
if [[ -f config/application.properties ]]; then
  cp -f config/application.properties "$BACKUP/config/"
elif [[ -f src/main/resources/application.properties ]]; then
  cp -f src/main/resources/application.properties "$BACKUP/config/application.properties"
fi

echo "Removing old project files (keeping backup under $BACKUP)..."
shopt -s dotglob nullglob
for path in "$APP_DIR"/*; do
  base="$(basename "$path")"
  case "$base" in
    .|..) continue ;;
    .env) continue ;;
  esac
  rm -rf "$path"
done
shopt -u dotglob

mkdir -p config
cp -f "$BACKUP/.env" .env
if [[ -f "$BACKUP/config/application.properties" ]]; then
  cp -f "$BACKUP/config/application.properties" config/application.properties
else
  echo "No application.properties in old tree; fetching example from GitHub."
  curl -fsSL -o config/application.properties.example "${REPO_RAW}/config/application.properties.example"
  cp -f config/application.properties.example config/application.properties
  echo "Wrote config/application.properties from example — edit secrets before relying on it." >&2
fi

echo "Fetching docker-compose.yml from ${REPO_RAW} ..."
curl -fsSL -o docker-compose.yml "${REPO_RAW}/docker-compose.yml"

if ! grep -q '^DOCKER_IMAGE=' .env; then
  echo 'DOCKER_IMAGE=ghcr.io/yeyintlwin/myanmyanlearn:latest' >> .env
  echo "Appended DOCKER_IMAGE=ghcr.io/yeyintlwin/myanmyanlearn:latest to .env (adjust if needed)." >&2
fi

echo "Pulling images and starting stack..."
docker compose pull
docker compose up -d

echo "Done. Backup of previous .env/config: $BACKUP"
docker compose ps
