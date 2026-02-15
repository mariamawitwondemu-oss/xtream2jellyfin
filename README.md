# Xtream2Jellyfin

Import IPTV movies and series into Jellyfin.

## Overview

Many IPTV providers expose their content catalog through the Xtream API. `xtream2jellyfin` connects to these providers and builds a Jellyfin-compatible media library from their movies and TV series catalog.

For each title, it generates:
- **.strm files** containing the stream URL, so Jellyfin can play the content directly from the IPTV provider
- **.nfo files** with metadata (title, plot, poster, cast, etc.) in a format Jellyfin and Kodi understand

No video files are downloaded. The media library is just a collection of lightweight text files that point to the IPTV provider's streams.

The tool supports multiple providers, automatic periodic syncing, and can trigger a Jellyfin library refresh after each sync.

## Output structure

```
media/
  {provider_name}/
    movies/
      {category}/
        {movie_name}/
          {movie_name}.strm
          {movie_name}.nfo
    series/
      {category}/
        {series_name}/
          tvshow.nfo
          Season 01/
            {series} - S01E01.strm
            {series} - S01E01.nfo
```

## Getting started

### 1. Create a configuration file

```bash
mkdir config
cp config/config.example.yaml config/config.yaml
```

Edit `config/config.yaml` with the IPTV provider details:

```yaml
providers:
  myprovider:
    username: "your_username"
    password: "your_password"
    url: "http://your-xtream-server.com"
```

That's the minimum required configuration. See [config/config.example.yaml](config/config.example.yaml) for the full list of available options.

### 2. Run with Docker

```bash
docker run -d \
  -v $(pwd)/config:/app/config \
  -v $(pwd)/media:/app/media \
  -v $(pwd)/cache:/app/cache \
  -e PUID=$(id -u) \
  -e PGID=$(id -g) \
  ghcr.io/mishka81/xtream2jellyfin:latest
```

The mounted directories (`config`, `media`, `cache`) must be writable by the user specified via `PUID`/`PGID`. If permissions are incorrect, the container will exit with an error message showing the `chown` command to run.

The application syncs all configured providers every 6 hours by default.

#### Docker Compose

```yaml
services:
  jellyfin:
    image: jellyfin/jellyfin:latest
    container_name: jellyfin
    # ...

  xtream2jellyfin:
    image: ghcr.io/mishka81/xtream2jellyfin:latest
    container_name: xtream2jellyfin
    environment:
      - PUID=1000
      - PGID=1000
      - TZ=${TZ}
    volumes:
      - /path/to/config:/app/config
      - /path/to/cache:/app/cache
      - /path/to/media:/app/media
```

### 3. Add the library in Jellyfin

1. In Jellyfin, go to **Dashboard > Libraries > Add Media Library**
2. For movies: select **Movies** as the content type and point to `media/{provider_name}/movies`
3. For series: select **Shows** as the content type and point to `media/{provider_name}/series`
4. Disable all metadata downloaders for best performance. Jellyfin parses NFO files by default, so all the metadata from the IPTV provider will still be available without any additional downloaders.

> **Note:** if metadata downloaders (TMDB, etc.) are left enabled, the first library scan can take many hours on large libraries, as Jellyfin will fetch metadata for every single title.

## Commands

One-off commands are available instead of the default continuous sync:

```bash
# Import only movies (one-shot, then exit)
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin import-movies

# Import only series
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin import-series

# List available movie categories (useful for filtering)
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin get-movies-categories

# List available series categories
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin get-series-categories

# Trigger a Jellyfin library refresh (movies + series)
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin refresh-jellyfin

# Refresh movies library only
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin refresh-jellyfin-movies

# Refresh series library only
docker run --rm -v ... ghcr.io/mishka81/xtream2jellyfin refresh-jellyfin-series
```

## Jellyfin library refresh

The application can automatically trigger a Jellyfin library refresh after each sync. Add the following to the provider configuration:

```yaml
providers:
  myprovider:
    # ...
    libraryRefresh:
      enabled: true
      protocol: "http"
      hostname: "localhost"
      port: 8096
      token: "your_jellyfin_api_token"
      moviesLibraryId: "your_movies_library_id"
      seriesLibraryId: "your_series_library_id"
```

Library IDs can be found via the Jellyfin API: `GET /Library/VirtualFolders` with header `Authorization: MediaBrowser Token=<token>`. Each library in the response has an `ItemId` field.

## Building from source

Requires Java 21+ and Maven 3.6+.

```bash
mvn clean package
java -jar target/xtream2jellyfin-1.0.0-jar-with-dependencies.jar
```

## Configuration reference

See [config/config.example.yaml](config/config.example.yaml) for the full list of available options.
