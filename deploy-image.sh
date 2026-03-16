#!/usr/bin/env bash
# Automates build -> save -> scp of a Docker image
# Usage:
#   REMOTE_HOST=1.2.3.4 [REMOTE_USER=kallo] [REMOTE_PATH=/tmp] \
#   [PLATFORM=linux/amd64] [DOCKERFILE=./Dockerfile] [CONTEXT=.] \
#   [COMPRESS=false] \
#   ./deploy-image.sh image:tag
#
# Example:
#   REMOTE_HOST=203.0.113.10 ./deploy-image.sh test-image:1.0.0
#
# Notes:
# - Requires: docker (buildx), scp, gzip (if COMPRESS=true)
# - The tar file name is generated from the image name (special chars replaced)

set -Eeuo pipefail

### --- log helpers ---
info()  { printf "ℹ️  %s\n" "$*"; }
ok()    { printf "✅ %s\n" "$*"; }
warn()  { printf "⚠️  %s\n" "$*" >&2; }
fail()  { printf "❌ %s\n" "$*" >&2; exit 1; }

trap 'fail "Error at line $LINENO. Aborting."' ERR
### --- parameters ---
if [[ $# -lt 1 ]]; then
  fail "Provide the image with tag, e.g: ./deploy-image.sh my-app:1.2.3"
fi
IMAGE="$1"

# Default environment variables
REMOTE_USER="${REMOTE_USER:-kallo}"
REMOTE_HOST="${REMOTE_HOST:-}"
REMOTE_PATH="${REMOTE_PATH:-/tmp}"
PLATFORM="${PLATFORM:-linux/amd64}"
DOCKERFILE="${DOCKERFILE:-./Dockerfile}"
CONTEXT="${CONTEXT:-.}"
COMPRESS="${COMPRESS:-false}"   # true = gzip the tar file
STEP="${STEP:-1}" # Optional STEP parameter (default = 1)

if ! [[ "$STEP" =~ ^[1-3]$ ]]; then
  fail "STEP must be 1, 2, or 3"
fi
info "Executing from STEP $STEP"

[[ -z "$REMOTE_HOST" ]] && fail "REMOTE_HOST is required (e.g: REMOTE_HOST=1.2.3.4)."

command -v docker >/dev/null 2>&1 || fail "docker not found."
command -v scp    >/dev/null 2>&1 || fail "scp not found."
if [[ "$COMPRESS" == "true" ]]; then command -v gzip >/dev/null 2>&1 || fail "gzip not found."; fi

# Output file name
# 1) Strip tag from image name (everything after the first ':')
IMAGE_NAME_NO_TAG="${IMAGE%%:*}"

# 2) Replace '/' with '_' to make it filesystem-safe
SAFE_IMAGE_NAME="${IMAGE_NAME_NO_TAG//\//_}"

# 3) Build tar filename (without version/tag)
TAR_FILE="${SAFE_IMAGE_NAME}.tar"
[[ "$COMPRESS" == "true" ]] && TAR_FILE="${TAR_FILE}.gz"

### --- buildx availability check ---
if ! docker buildx version >/dev/null 2>&1; then
  fail "docker buildx is not available. Install or enable buildx."
fi

info "Target image: $IMAGE"
info "Platform: $PLATFORM"
info "Dockerfile: $DOCKERFILE"
info "Context: $CONTEXT"
info "Server: ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}"
[[ "$COMPRESS" == "true" ]] && info "Compression: gzip enabled"

### --- step 1: remove existing local image ---
if [[ "$STEP" -le 1 ]]; then
  info "Starting STEP 1"
  if docker image inspect "$IMAGE" >/dev/null 2>&1; then
    info "Removing existing local image: $IMAGE"
    docker rmi -f "$IMAGE" >/dev/null
  else
    warn "Image $IMAGE not found locally (ok)."
  fi

  ### --- step 2: build image ---
  info "Building image using buildx..."
  docker buildx build \
    --platform "$PLATFORM" \
    -t "$IMAGE" \
    -f "$DOCKERFILE" \
    "$CONTEXT" \
    --load

  ok "Build completed."
fi

### --- step 3: save image as tar (optional gzip) ---
if [[ "$STEP" -le 2 ]]; then
  info "Starting STEP 2"
  [[ -f "${TAR_FILE%.gz}" ]] && rm -f "${TAR_FILE%.gz}"
  [[ -f "$TAR_FILE" ]] && rm -f "$TAR_FILE"

  if [[ "$COMPRESS" == "true" ]]; then
    info "Exporting image and compressing..."
    TMP_TAR="${SAFE_IMAGE_NAME}.tar"
    docker save -o "$TMP_TAR" "$IMAGE"
    gzip -9 "$TMP_TAR"
    mv "${TMP_TAR}.gz" "$TAR_FILE"
  else
    info "Exporting image to tar..."
    docker save -o "$TAR_FILE" "$IMAGE"
  fi

  if command -v sha256sum >/dev/null 2>&1; then
    SHA="$(sha256sum "$TAR_FILE" | awk '{print $1}')"
    info "SHA256 checksum: $SHA"
  fi
  ok "Archive created: $TAR_FILE"
fi

### --- step 4: transfer to server ---
if [[ "$STEP" -le 3 ]]; then
  info "Starting STEP 3"
  info "Transferring to ${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH} ..."
  scp "$TAR_FILE" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_PATH}"
  ok "Transfer completed."
fi
