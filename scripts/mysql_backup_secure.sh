#!/bin/bash
set -euo pipefail

export PATH="/opt/homebrew/opt/mysql-client/bin:$PATH"

PROPS_PATH="${1:-src/main/resources/application.properties}"
MODE="${2:-full}"
if [ "$MODE" = "--schema-only" ]; then
  MODE="schema-only"
fi

get_prop() {
  local key="$1"
  local file="$2"
  local escaped
  escaped="${key//./\\.}"
  local line
  line="$(grep -E "^[[:space:]]*${escaped}=" "$file" | tail -n 1 || true)"
  if [ -z "$line" ]; then
    return 1
  fi
  printf '%s' "${line#*=}"
}

JDBC_URL="$(get_prop "spring.datasource.url" "$PROPS_PATH")"
DB_USER="$(get_prop "spring.datasource.username" "$PROPS_PATH")"
DB_PASS="$(get_prop "spring.datasource.password" "$PROPS_PATH" || true)"
BACKUP_DIR="$(get_prop "app.mysql.backup.directory" "$PROPS_PATH" || true)"
RETENTION="$(get_prop "app.mysql.backup.retention" "$PROPS_PATH" || true)"

if [ -z "${BACKUP_DIR:-}" ]; then
  BACKUP_DIR="scripts/backups"
fi
if [ -z "${RETENTION:-}" ]; then
  RETENTION="7"
fi

if [[ "$JDBC_URL" != jdbc:mysql://* ]]; then
  echo "Unsupported JDBC url: $JDBC_URL" >&2
  exit 1
fi

URL_NO_PREFIX="${JDBC_URL#jdbc:mysql://}"
HOST_PORT="${URL_NO_PREFIX%%/*}"
DB_AND_QUERY="${URL_NO_PREFIX#*/}"
DB_NAME="${DB_AND_QUERY%%\?*}"

DB_HOST="${HOST_PORT%%:*}"
DB_PORT="${HOST_PORT#*:}"
if [ "$DB_PORT" = "$HOST_PORT" ]; then
  DB_PORT="3306"
fi

if [ -z "$DB_HOST" ] || [ -z "$DB_PORT" ] || [ -z "$DB_NAME" ] || [ -z "$DB_USER" ]; then
  echo "Missing db connection fields parsed from properties." >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"
TIMESTAMP="$(date +"%Y%m%d_%H%M%S")"
BACKUP_FILE="${BACKUP_DIR}/${DB_NAME}_${TIMESTAMP}.sql"

CONFIG_FILE="$(mktemp /tmp/mysql_backup_XXXXX.cnf)"
chmod 600 "$CONFIG_FILE"
trap 'rm -f "$CONFIG_FILE"' EXIT

{
  echo "[client]"
  echo "host=$DB_HOST"
  echo "port=$DB_PORT"
  echo "user=$DB_USER"
  if [ -n "${DB_PASS:-}" ]; then
    echo "password=$DB_PASS"
  fi
} > "$CONFIG_FILE"

if [ "$MODE" = "schema-only" ]; then
  mysqldump \
    --defaults-extra-file="$CONFIG_FILE" \
    --no-data \
    --skip-triggers \
    --single-transaction \
    --quick \
    --skip-lock-tables \
    --no-tablespaces \
    --set-gtid-purged=OFF \
    "$DB_NAME" > "$BACKUP_FILE"
else
  mysqldump \
    --defaults-extra-file="$CONFIG_FILE" \
    --routines \
    --triggers \
    --events \
    --single-transaction \
    --quick \
    --skip-lock-tables \
    --no-tablespaces \
    --set-gtid-purged=OFF \
    "$DB_NAME" > "$BACKUP_FILE"
fi

ls -lh "$BACKUP_FILE"

if [ "$RETENTION" -gt 0 ] 2>/dev/null; then
  ls -1t "${BACKUP_DIR}/${DB_NAME}_"*.sql 2>/dev/null | tail -n +"$((RETENTION + 1))" | xargs -I {} rm -f "{}" || true
fi
