#!/bin/sh

# Set default values if not provided
PUID=${PUID:-1000}
PGID=${PGID:-1000}

# Modify the java user/group to match PUID/PGID
groupmod -o -g "$PGID" java
usermod -o -u "$PUID" java

# Fix ownership of app directories
chown -R java:java /app/config /app/cache /app/media

# Execute the command as the java user
exec gosu java "$@"
