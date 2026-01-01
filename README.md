# Xtream2Jellyfin

Import IPTV VOD streams into Jellyfin.

🚧 **WORK IN PROGRESS** 🚧

## Overview

`xtream2jellyfin` is a Java-based IPTV stream converter that bridges Xtream Codes IPTV providers with Jellyfin. It converts IPTV content from Xtream API endpoints into organized file structures (STRM files) that Jellyfin can consume.

## Features

- multi-provider support: handle multiple IPTV providers simultaneously
- content types:
  - movies (VOD) with .strm files
  - TV series with organized season/episode structures
- metadata support: NFO files (Jellyfin/Kodi compatible) for movies, series, and episodes
- flexible configuration:
  - per-media-type settings with regex-based name cleaning
  - category filtering (include/exclude lists)
  - HTTP client configuration (timeouts, rate limiting, retries)
  - customizable logging levels
- Jellyfin integration: automatic library refresh after content updates
- intelligent caching: optional file manager that prevents unnecessary disk writes

## Cached File Manager

The cached file manager is an optional optimization that significantly improves performance when syncing with Jellyfin by preventing unnecessary metadata refreshes.

### How it works

When enabled (`file_manager_type: "cached"`), the application:

1. **Tracks file content using MD5 hashing**: before writing any file (STRM, NFO), it computes an MD5 hash of the content
2. **Compares against previous state**: checks if the file was previously written with the same content hash
3. **Skips unchanged files**: only writes files to disk when content has actually changed
4. **Manages stale files**: automatically removes files that are no longer present in the IPTV provider's catalog

### Why this matters for Jellyfin

Jellyfin's library scanner uses **file modification timestamps** (mtime) to detect changes, not content hashing. This means:

- **Without caching**: every run rewrites all files → updates all mtimes → triggers full metadata refresh in Jellyfin
- **With caching**: only changed files are written → only changed items trigger metadata refresh

During a metadata refresh, Jellyfin performs expensive operations:
- Re-probes video files with FFmpeg
- Re-reads and parses NFO metadata
- Regenerates extracted data (chapters, trickplay images)
- Updates the database

### Performance impact

For a library with thousands of items where content rarely changes:

- **Without caching**: Jellyfin re-scans entire library every sync (potentially hours)
- **With caching**: Jellyfin only processes new/modified items (seconds to minutes)

### Configuration

Enable in `config/config.yaml`:

```yaml
app:
  file_manager_type: "cached"  # use cached file manager
```

The cache database is stored in `{media_dir}/{provider_name}/.cache/files.json`.

### When to use each mode

- **`simple`**: first-time setup, troubleshooting, or when you want every file rewritten
- **`cached`**: production use with scheduled syncs (recommended for best performance)

## Requirements

- Java 21 or higher
- Maven 3.6 or higher
- access to Xtream Codes IPTV provider(s)
- Jellyfin server (optional, for library refresh)

## Installation

### Build From Source

```bash
mvn clean package
```

This will create a JAR file with all dependencies in `target/xtream2jellyfin-1.0.0-jar-with-dependencies.jar`.

## Configuration

1. Copy the example configuration file:
```bash
cp config/config.example.yaml config/config.yaml
```

2. Edit `config/config.yaml` with your settings:

```yaml
app:
  file_manager_type: "simple"    # file manager: "simple" or "cached" (default: "simple")
  media_dir: "media"             # base media output directory (default: "media")
  write_metadata_nfo: true       # write NFO metadata files for Jellyfin/Kodi (default: true)

  # Logging configuration
  logging:
    level: "INFO"                # root log level: TRACE, DEBUG, INFO, WARN, ERROR
    loggers:
      "io.prometheus": "WARN"
      "uk.humbkr.xtream2jellyfin": "DEBUG"
      "uk.humbkr.xtream2jellyfin.filemanager.CachedFileManager": "INFO"

providers:
  provider1:
    username: "your_username"
    password: "your_password"
    url: "http://your-xtream-server.com"
    interval: 360  # scan interval in minutes

    # HTTP client configuration
    http_client:
      timeout_seconds: 30      # request timeout (default: 30)
      rate_limit: "100/s"      # rate limit: {count}/{unit} - s=second, m=minute, h=hour, d=day (default: "100/s")
      retry_delay_seconds: 1   # delay before retrying failed requests (default: 1)
      max_retries: 3           # maximum number of retry attempts (default: 3)

    # Category name cleanup patterns (applied to folder names)
    category_name_cleanup_patterns:
      "^\\[.*\\]\\s*": ""  # remove leading brackets
      "\\s*\\[.*\\]$": ""  # remove trailing brackets

    # Optional: Jellyfin library refresh after sync
    libraryRefresh:
      enabled: true
      protocol: "http"
      hostname: "localhost"
      port: 8096
      token: "your_jellyfin_api_token"

    settings:
      movies:
        enabled: true
        category_folder: true
        use_server_info: false

        # Movie title cleanup patterns
        name_cleanup_patterns:
          "^\\[.*\\]\\s*": ""
          "\\s*\\[.*\\]$": ""

        include_category_ids: []
        exclude_category_ids: []

      series:
        enabled: true
        category_folder: true
        use_server_info: false

        # Series title cleanup patterns
        name_cleanup_patterns:
          "^\\[.*\\]\\s*": ""
          "\\s*\\[.*\\]$": ""

        include_category_ids: []
        exclude_category_ids: []
```

### Configuration Options

#### Global Application Settings (`app`)

- `file_manager_type`: file manager implementation - `simple` or `cached` (default: `simple`)
  - `simple`: writes files directly to disk
  - `cached`: tracks changes and only writes modified files
- `media_dir`: base media output directory (default: `media`)
- `write_metadata_nfo`: write NFO metadata files compatible with Jellyfin/Kodi (default: `true`)
- `logging`: logging configuration
  - `level`: root log level - `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR` (default: `INFO`)
  - `loggers`: per-package log levels (optional)

#### Provider Settings (`providers`)

Each provider can have the following settings:

- `username`/`password`: Xtream provider credentials
- `url`: Xtream provider URL
- `interval`: scan interval in minutes (default: 360 = 6 hours)
- `http_client`: HTTP client configuration
  - `timeout_seconds`: request timeout in seconds (default: 30)
  - `rate_limit`: rate limiting format `{count}/{unit}` where unit is `s`, `m`, `h`, or `d` (default: `100/s`)
  - `retry_delay_seconds`: delay before retrying failed requests (default: 1)
  - `max_retries`: maximum number of retry attempts (default: 3)
- `category_name_cleanup_patterns`: regex patterns to clean category names (key: regex pattern, value: replacement string)
- `libraryRefresh`:
  - `enabled`: whether to trigger library refresh after updates
  - `protocol`/`hostname`/`port`: Jellyfin server details
  - `token`: API token for authentication

#### Media-Type Settings (movies, series)

- `enabled`: enable/disable this media type
- `category_folder`: organize content by category
- `use_server_info`: use server-provided URL (if false, constructs URL from provider details)
- `name_cleanup_patterns`: regex patterns to clean stream names (key: regex pattern, value: replacement string)
- `include_category_ids`: list of category IDs to include (if set, only these categories are processed)
- `exclude_category_ids`: list of category IDs to exclude (ignored if `include_category_ids` is set)

## Running

### Run Directly

```bash
java -jar target/xtream2jellyfin-1.0.0-jar-with-dependencies.jar
```

All configuration is now centralized in `config/config.yaml`. No environment variables are needed.

## Output Structure

```
media/
  {provider_name}/
    movies/
      {category}/       # optional category folder
        {movie_name}/
          {movie_name}.strm
          {movie_name}.nfo
    series/
      {category}/       # optional category folder
        {series_name}/
          tvshow.nfo
          Season 01/
            {series} - S01E01.strm
            {series} - S01E01.nfo
            {series} - S01E02.strm
            {series} - S01E02.nfo
```

## Docker Support

Build and run using the provided Dockerfile:

```bash
docker build -t xtream2jellyfin .
docker run -v $(pwd)/config:/app/config -v $(pwd)/media:/app/media xtream2jellyfin
```

All configuration is managed through the `config/config.yaml` file mounted as a volume.
