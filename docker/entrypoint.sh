#!/bin/bash

set -eo pipefail

# Set default values if not provided
PUID=${PUID:-1000}
PGID=${PGID:-1000}

# Modify the java user/group to match PUID/PGID
groupmod -o -g "$PGID" java
usermod -o -u "$PUID" java

# Check that mounted volumes are writable by the configured user
for dir in /app/config /app/cache /app/media; do
  if [ -d "$dir" ] && ! gosu java test -w "$dir"; then
    echo "ERROR: $dir is not writable by user $PUID:$PGID"
    echo "Fix the permissions on the host with: chown -R $PUID:$PGID $(realpath "$dir")"
    exit 1
  fi
done

# If the first argument is not "java", treat it as an app command
if [ "$1" != "java" ]; then
  exec gosu java java -jar /app/xtream2jellyfin.jar "$@"
fi

exec gosu java "$@"
