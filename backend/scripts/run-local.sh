#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
BACKEND_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)

cd "$BACKEND_DIR"

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

mvn spring-boot:run
