#!/usr/bin/env bash
# Runs the whole app locally:
#   backend  (Spring Boot)                      http://localhost:8080
#   frontend (dev server, live reload, proxies the API to the backend)  http://localhost:5173
# Ctrl-C stops both. Works with macOS's default bash 3.2. (Cross-platform alternative: java scripts/Dev.java)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_PORT=8080
FRONTEND_PORT=5173
DB_PORT=5432

port_open() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null
}

prefix() {
  awk -v tag="$1" '{ print tag " " $0; fflush() }'
}

if ! port_open "$DB_PORT"; then
  echo "WARN: PostgreSQL is not reachable on localhost:$DB_PORT - the backend will fail to start until it is running."
fi
for port in "$BACKEND_PORT" "$FRONTEND_PORT"; do
  if port_open "$port"; then
    echo "Port $port is already in use; stop whatever is running there first." >&2
    exit 1
  fi
done

if [ ! -d "$ROOT/frontend/node_modules" ]; then
  echo "Installing frontend dependencies..."
  (cd "$ROOT/frontend" && npm install)
fi

# job control: each background pipeline gets its own process group, so cleanup can kill whole trees
set -m
(cd "$ROOT/backend" && exec ./mvnw -q spring-boot:run) 2>&1 | prefix "[BE]" &
(cd "$ROOT/frontend" && exec npm run dev) 2>&1 | prefix "[FE]" &
GROUPS_TO_STOP="$(jobs -p)"

cleanup() {
  trap - EXIT INT TERM
  echo
  echo "Stopping backend and frontend..."
  for group in $GROUPS_TO_STOP; do
    kill -TERM -- "-$group" 2>/dev/null || true
  done
  wait 2>/dev/null || true
}
trap cleanup EXIT INT TERM

echo "Frontend: http://localhost:$FRONTEND_PORT  Backend: http://localhost:$BACKEND_PORT  (Ctrl-C stops both)"

# stop everything as soon as either side exits
while true; do
  for group in $GROUPS_TO_STOP; do
    if ! kill -0 -- "-$group" 2>/dev/null; then
      echo "A process exited; shutting down the other one."
      exit 1
    fi
  done
  sleep 1
done
