#!/bin/sh

# Set default values if not provided
PUID=${PUID:-1000}
PGID=${PGID:-1000}

# Modify the java user/group to match PUID/PGID
groupmod -o -g "$PGID" java
usermod -o -u "$PUID" java

# Fix ownership of app directories (only files not already owned correctly)
for dir in /app/config /app/cache /app/media; do
  if [ -d "$dir" ]; then
    find "$dir" \( ! -user "$PUID" -o ! -group "$PGID" \) -exec chown java:java {} +
  fi
done

# Execute the command as the java user
exec gosu java "$@"
