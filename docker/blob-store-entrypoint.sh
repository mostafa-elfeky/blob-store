#!/bin/sh
set -eu

CONFIG_DIR="${BLOBSTORE_CONFIG_DIR:-/var/lib/blob-store/config}"
STORAGE_ROOT="${BLOBSTORE_STORAGE_ROOT_DIR:-/var/lib/blob-store/storage}"
BOOTSTRAP_DB_PATH="${BLOBSTORE_DATABASE_BOOTSTRAP_DATABASE_PATH:-$CONFIG_DIR/bootstrap/blob-store}"

ADMIN_FILE="${BLOBSTORE_ADMIN_CONFIG_FILE:-$CONFIG_DIR/admin-user.properties}"
DB_FILE="${BLOBSTORE_DATABASE_CONFIG_FILE:-$CONFIG_DIR/database-connection.properties}"
API_FILE="${BLOBSTORE_API_SECURITY_CONFIG_FILE:-$CONFIG_DIR/api-security.properties}"

mkdir -p "$CONFIG_DIR" "$STORAGE_ROOT" "$(dirname "$BOOTSTRAP_DB_PATH")"

export BLOBSTORE_ADMIN_CONFIG_FILE="$ADMIN_FILE"
export BLOBSTORE_DATABASE_CONFIG_FILE="$DB_FILE"
export BLOBSTORE_API_SECURITY_CONFIG_FILE="$API_FILE"
export BLOBSTORE_DATABASE_BOOTSTRAP_DATABASE_PATH="$BOOTSTRAP_DB_PATH"
export BLOBSTORE_STORAGE_ROOT_DIR="$STORAGE_ROOT"

write_property_file() {
  file_path="$1"
  shift
  mkdir -p "$(dirname "$file_path")"
  : > "$file_path"
  while [ "$#" -gt 0 ]; do
    printf '%s\n' "$1" >> "$file_path"
    shift
  done
}

if [ -n "${BLOBSTORE_ADMIN_USERNAME:-}" ] && [ -n "${BLOBSTORE_ADMIN_PASSWORD:-}" ]; then
  write_property_file "$ADMIN_FILE" \
    "username=${BLOBSTORE_ADMIN_USERNAME}" \
    "password=${BLOBSTORE_ADMIN_PASSWORD}"
fi

if [ -n "${BLOBSTORE_DB_VENDOR:-}" ] \
  && [ -n "${BLOBSTORE_DB_HOST:-}" ] \
  && [ -n "${BLOBSTORE_DB_PORT:-}" ] \
  && [ -n "${BLOBSTORE_DB_NAME:-}" ] \
  && [ -n "${BLOBSTORE_DB_USERNAME:-}" ]; then
  write_property_file "$DB_FILE" \
    "vendor=${BLOBSTORE_DB_VENDOR}" \
    "host=${BLOBSTORE_DB_HOST}" \
    "port=${BLOBSTORE_DB_PORT}" \
    "databaseName=${BLOBSTORE_DB_NAME}" \
    "schema=${BLOBSTORE_DB_SCHEMA:-}" \
    "username=${BLOBSTORE_DB_USERNAME}" \
    "password=${BLOBSTORE_DB_PASSWORD:-}"
fi

if [ -n "${BLOBSTORE_STORAGE_ROOT_DIR:-}" ]; then
  write_property_file "$CONFIG_DIR/storage-settings.properties" \
    "rootDir=${BLOBSTORE_STORAGE_ROOT_DIR}"
fi

if [ -n "${BLOBSTORE_API_JWT_VALIDATION_MODE:-}" ] \
  || [ -n "${BLOBSTORE_API_BASIC_AUTH_ENABLED:-}" ] \
  || [ -n "${BLOBSTORE_API_BASIC_USERNAME:-}" ] \
  || [ -n "${BLOBSTORE_API_BASIC_PASSWORD:-}" ]; then
  write_property_file "$API_FILE" \
    "jwtValidationMode=${BLOBSTORE_API_JWT_VALIDATION_MODE:-SHARED_SECRET}" \
    "jwtSharedSecret=${BLOBSTORE_API_JWT_SHARED_SECRET:-}" \
    "jwtJwkSetUri=${BLOBSTORE_API_JWT_JWK_SET_URI:-}" \
    "jwtIssuer=${BLOBSTORE_API_JWT_ISSUER:-}" \
    "jwtAudience=${BLOBSTORE_API_JWT_AUDIENCE:-}" \
    "basicAuthEnabled=${BLOBSTORE_API_BASIC_AUTH_ENABLED:-false}" \
    "basicUsername=${BLOBSTORE_API_BASIC_USERNAME:-}" \
    "basicPassword=${BLOBSTORE_API_BASIC_PASSWORD:-}"
fi

exec java ${JAVA_OPTS:-} -jar /app/blob-store.jar
