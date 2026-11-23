# misterdownloadmanager — Android Port

An Android port of Mister Download Manager (MDM) version 0.5.2, translated
from the upstream Rust project `anefzaoui/misterdownloadmanager`
(ref `43d8fe2cc204fe30088b70adc07b861133b850fa`) to Kotlin with Jetpack
Compose. The app name is `misterdownloadmanager` and the package and
applicationId are `com.misterdownloadmanager.port`.

Mister Download Manager is a free, open source download manager. This port
is a translation only: the state machines, validation rules, limits,
ordering of steps, and behaviour of the original code are preserved
wherever the platform allows it. Where Android forces an adaptation, the
adaptation is listed in this README instead of silently changing the logic.

## License

This port is licensed under the GNU General Public License version 3 or
later (GPL-3.0-or-later), the same license as the upstream project. The
full license text is in `LICENSE`.

## Requirements

- Android 7.0 (API 24) or later, up to the latest Android release.
- JDK 17.
- Android SDK with platform API 36 and build-tools 36.0.0.

## Build

```
./gradlew assembleDebug
```

The Gradle wrapper jar is present in `gradle/wrapper/`. The debug APK is
produced at `app/build/outputs/apk/debug/app-debug.apk`.

**Build status in this environment:** the full Gradle build could
not be completed in this VM. The Gradle daemon starts, but the client
cannot receive a message from it: the daemon handshake over loopback
TCP fails with a deserialization error (`Unexpected type tag 109`),
which means the loopback socket stream is being interfered with in
this sandbox, even though a plain TCP connection to `127.0.0.1`
succeeds. The same failure happens with `--no-daemon`,
`org.gradle.daemon=false`, IPv6 loopback flags and `--offline`, and
with the installed Gradle 8.7 binary as well as `./gradlew`. The
wrapper jar originally shipped in this repo was also the wrong
artifact: it was the Gradle distribution's bootstrap container jar
(49,625 bytes), whose outer `GradleWrapperMain` references
`org.gradle.wrapper.IDownload`, a class that does not exist in
Gradle 8.7, so `./gradlew` failed immediately with
`NoClassDefFoundError: org/gradle/wrapper/IDownload`. It has been
replaced with the real flat project wrapper jar (43,453 bytes,
SHA-256 `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`)
taken from the Gradle project's own `v8.7.0` wrapper, together with
the official `gradlew` and `gradlew.bat` scripts. With the correct
jar, `./gradlew` starts and begins downloading the Gradle 8.7
distribution; in this VM that download is blocked by the sandbox
proxy (`SSLException: Unsupported or unrecognized SSL message`), so
this is an environment limitation, not a project error. All 50 Kotlin
files were instead type-checked directly with kotlinc 2.0.21 against
the real dependency artifacts (see "What was verified"), and all 24
unit tests pass. Run `./gradlew assembleDebug` on a machine with
unrestricted networking and loopback before release.

## Project layout

```
settings.gradle.kts
build.gradle.kts
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/com/misterdownloadmanager/port/
  MainActivity.kt
  MisterDownloadManagerApp.kt
  core/model/        DownloadItem, Category, Checksum, Settings, Queue, DownloadError
  core/engine/       SegmentPlanner, SpeedLimiter, SpeedTracker, ChecksumVerifier,
                     FileNaming, DiskWriter, HttpDownloader, DownloadEngine,
                     FtpClient, DnsResolver, HtmlParser
  core/torrent/      TorrentState, TorrentSession
  core/debrid/       DebridService, DebridProviders
  core/extract/      ArchiveExtractor
  core/geo/          GeoInfo, GeoDatabase
  core/cnl/          ClickNLoad
  core/crash/        CrashReporter
  core/ipc/          IpcApi
  core/remote/       RemotePairing
  core/mcp/          McpTools
  core/cli/          CliCommand
  core/tools/        VideoTools
  core/labserver/    LabServer
  core/nmhost/       NativeMessagingHost
  data/              Store, DownloadRepository, DownloadContainer
  service/           DownloadService, DownloadBootReceiver
  ui/                DownloadViewModel, theme/Theme, icons/MdmIcons,
                     screens/DownloadListScreen, AddDownloadScreen,
                     DownloadDetailScreen, SettingsScreen, CategoriesScreen
  app/src/main/res/values/strings.xml
  app/src/main/res/drawable/  contextual vector icons
  app/src/test/java/com/misterdownloadmanager/port/PortLogicTest.kt
```

## Crate mapping

| Upstream crate | Kotlin package | Status |
|---|---|---|
| app (model, settings, categories, checksum, queue, scheduler, store) | core.model, data | Translated. App status has 5 values: Queued, Downloading, Stopped, Complete, Error. Engine state has 9 values: Probing, Downloading, Verifying, Merging, Completing, Done, Stopping, Cancelled, Failed, mapped to app status in EngineState.toAppStatus. |
| engine (download, planner, limiter, verify, ftp, dns, naming, disk) | core.engine | Translated. See the logic notes below. |
| torrent | core.torrent | Translated as a state machine: magnet parsing, tracker filtering, piece manager with rarest-first selection, peer and tracker state, IP blocklist conversion. The upstream peer-to-peer wire protocol (librqbit) is not reimplemented; see Limitations. |
| debrid | core.debrid | Translated. Real-Debrid, AllDebrid, Premiumize, TorBox and MegaDebrid with the same provider endpoints, unrestrict flows, polling intervals and rate limits. TorBox and MegaDebrid flows follow the documented endpoints and were not tested against a live account; see Limitations. |
| extract | core.extract | Translated. ZIP, TAR and TAR.GZ extraction with magic-byte detection, the upstream safe-path rules, size and entry limits, and staging before moving files into place. 7z, RAR and XZ are detected and reported as NoTool, because they need an external 7-Zip binary on desktop. |
| geo | core.geo | Translated. The upstream DB-IP binary table format (magic `MDMGEO`, LEB128 deltas, sparse index every 256 entries) is parsed by `GeoDatabase`, with the same reserved-address rejection and flag-atlas cell math, and the DB-IP attribution. The country database file itself is not bundled, so lookups return null until it is added as an asset. |
| cnl | core.cnl | Translated. Click'n'Load form parsing, AES-128-CBC decryption with the key taken from the `jk` function (the key is also the IV, zero padding), the internal-link filter, and the upstream limits (port 9666, 256 KiB body, 500 links, 10 posts per minute per site). The loopback server itself is not started on Android; links arrive through share intents instead. |
| crash | core.crash | Translated. Uncaught-exception handler that writes a text crash report to the app files directory, using the upstream report shape (header, signature, system details), the freeze classification thresholds (0.6 running, 0.3 starved), the home-path scrub, and the GitHub issue URL budget of 7000 characters. Native minidumps are not available on Android. |
| ipc | core.ipc | Translated as the upstream command model: the same `IpcCommand` fields, the same limits (1 MiB message, 8 in flight, 2 s answer wait, 3 s request deadline), the same 64-hex token, and the same list/add/act answer shapes. The TCP localhost transport is replaced by an in-process command bus, because Android has a single app process. |
| remote | core.remote | Translated. Eight-digit pairing code, ten-minute validity, constant-time comparison, attempt limits (5 per address, 20 total, 1 s pace, 256 remembered addresses), device tokens stored only as SHA-256 hashes, and the upstream port 8463 and body limits. The HTTPS remote server itself is not started by this port. |
| mcp | core.mcp | Translated as the same seven tools: list_downloads, add_download, pause_downloads, resume_downloads, remove_downloads, start_queue, stop_queue, with the default list limit of 100. There is no stdio MCP binary on Android; the tools are an in-app model. |
| cli | core.cli | Translated as the same command set: get, status/list, install-ytdlp, install-ffmpeg, install-gallery-dl, install-js-runtime, update-helpers, gallery-get, video-info, video-get, playlist-info, playlist-get, torrent-create, torrent-info, torrent-get, pause, start, remove, delete, add, start-queue, stop-queue, pair, api, resume. There is no terminal on Android, so the commands are exposed as an in-app model, not as a binary. |
| tools | core.tools | Translated in part. Video URL detection, playlist entries and the 99.9 percent running cap are translated. yt-dlp, ffmpeg and gallery-dl are external desktop binaries and are not bundled; see Limitations. |
| labserver | core.labserver | Translated as the test route table. This is a test-only server from upstream and is not started by the app. |
| nmhost | core.nmhost | Translated as the native-messaging framing: 4-byte little-endian length prefix, 1 MiB maximum message. There is no desktop browser to host it on Android; see Limitations. |
| fuzz | (none) | Not ported. Fuzz targets are a development-only workspace in upstream. |

## Logic notes (values taken from the upstream source)

- Segmentation (`SegmentPlanner`, from `engine/planner.rs`): segments are
  aligned to 4096 bytes. A worker first claims an unassigned unfinished
  segment, then the segment right after the one it just finished when that
  segment has not started receiving, otherwise the largest remaining
  segment is split in half, unless a half would be smaller than
  `min_split` (default 12 KiB). When the aligned split point would fall
  below `reserved + min_split`, the unaligned midpoint is used. A worker
  may preempt a segment right after the one it finished when that
  segment's worker has not started receiving and the preempt delay has
  passed. Resume state must tile `[0, size)` exactly or it is rejected.
  Resume state is stored in `<dest>.mdm.json` (version 1) next to the
  `<dest>.mdm.part` file; legacy `.idmnext` state files are migrated.
- Retries (`DownloadEngine`, from `engine/download.rs`): the first retry
  is immediate; later retries wait `retry_delay * 2^min(failures - 2, 10)`,
  capped at 30 seconds, with at most 5 retries. A throttled response
  waits for `Retry-After`, capped at 300 seconds. A 200 answer to a
  Range request means the server ignores ranges, so the download falls
  back to a single connection.
- Speed limiting (`SpeedLimiter`, from `engine/limiter.rs`): token bucket
  with a burst of 1.0 seconds of the configured rate. Removing a limit
  forgives the accumulated debt.
- File naming (`FileNaming`, from `engine/naming.rs`): the name comes
  from `Content-Disposition` first, then from the URL (a `file=` or
  `filename=` query value when the last path segment names nothing,
  then the last path segment). A last path segment that is a server
  script (`download.php`, `getfile.aspx`, and similar) is never used
  as the file name: the `file=`/`filename=` query value is used
  instead, and when there is none the name falls back to `download`.
  When the `HEAD` response carries a `Content-Disposition` name that
  differs from the name chosen at add time, the engine renames the
  target file to it before writing (an existing file with that name
  is not overwritten; a numbered name is used instead). Names are
  sanitized (format characters removed, control characters and
  `<>:"/\|?*` become `_`, trailing dots and spaces removed, empty
  becomes `download`, Windows device names get a `_` prefix), capped
  at 230 characters. A taken name becomes `stem (1).ext`,
  `stem (2).ext`, and so on.
- Disk space (`DiskSpace`, from `engine/disk.rs`): 128 MiB is kept free
  besides the file size, and files over 4 GiB minus 1 byte are refused
  on FAT32. Sparse files are only used above 5 MiB upstream; Android
  storage does not need the sparse flag.
- DNS (`DnsResolver`, from `engine/dns.rs`): the five upstream presets
  (Cloudflare, Google, Quad9, AdGuard, NextDNS) with their exact server
  addresses, DoT names and DoH URLs. NextDNS offers no plain mode, and
  its profile id must be empty or exactly 6 hex characters. Local names
  (`localhost`, dotless names, `.local`, `.lan`, `.home.arpa`,
  `.internal`) always use the system resolver. Lookup timeout is 4
  seconds per server, with a 250 ms stagger and a 3 second race budget.
  Actual DoT and DoH transport is not implemented in this port; the
  system resolver is used, and the presets are kept for configuration.
- FTP (`FtpClient`, from `engine/ftp.rs`): `ftp` on port 21, `ftps`
  (implicit TLS) on port 990, `ftpes` (explicit TLS) on port 21. Passive
  mode is the default. Anonymous login is `anonymous` with password
  `mdm@`. A 550 answer to `SIZE` means the file does not exist. Idle
  sessions are closed after 30 seconds. Full segmented FTP transfer is
  not implemented; the client covers the SIZE probe and login flow.
- Torrent (`TorrentState`, `TorrentSession`, from `torrent/lib.rs`,
  `trackers.rs`, `names.rs`, `blocklist.rs`): phases are Checking,
  Downloading, Finished, Paused, Error. A `.torrent` file over 10 MiB
  is rejected. The lowest torrent rate is 16 KiB/s. DHT bootstrap uses
  the four upstream nodes. The listen port is tried 20 times and kept
  in `listen-port`; session state lives in `session.json`. Trackers are
  filtered: `http`/`https` are always allowed, `udp` only without a
  proxy, and `localhost`, `*.localhost`, `*.local` and non-public IPs
  are rejected. The blocklist converter reads PeerGuardian and eMule
  formats (including gzip, unpacked at most 256 MiB) and emits checked
  `mdm:start-end` lines.
- Debrid (`DebridProviders`, from `debrid/`): the five services with
  their base URLs and the upstream self-imposed rate limits (Real-Debrid
  120/min, AllDebrid 300/min, Premiumize 60/min, TorBox 120/min,
  Mega-Debrid 60/min). Real-Debrid posts `link` and `remote=0` and
  reads `chunks` as the connection count. AllDebrid posts to
  `v4/link/unlock` and polls a delayed link every `time_left / 4`
  seconds (minimum 5 seconds) for up to 10 minutes. Premiumize
  requires exactly one content entry. TorBox creates a web download,
  polls its list every 5 seconds for up to 1 hour, and requests the
  link with `web_id` and `file_id`. Mega-Debrid posts
  `api.php?action=debridLink` with `token` and `link`. An unsupported
  host is not an error: the file is downloaded directly instead.
- Click'n'Load (`ClickNLoad`, from `cnl/`): the key is read from the
  `jk` function (`return '...'` with 32 hex digits), used as both the
  AES-128-CBC key and the IV, with zero padding. Only `http`, `https`
  and `ftp` links are kept, duplicates are removed, and internal links
  (loopback, private, link-local, CGNAT, `localhost`, dotless names,
  `.localhost`, `.local`, `.internal`, `.lan`, `.home.arpa`,
  `.intranet`) are dropped.
- Remote pairing (`RemotePairing`, from `remote/pairing.rs` and
  `devices.rs`): the code is 8 digits, valid for 10 minutes, compared
  in constant time. Guessing is limited to 5 tries per address and 20
  total, with 1 second between tries per address and 256 remembered
  addresses. Device tokens are 64 hex characters and only their
  SHA-256 hash is stored.
- Crash reports (`CrashReporter`, from `crash/`): the report header,
  signature (`<kind> in <function>`), freeze classification (CPU share
  0.6 or above means running, 0.3 or below means starved), 12 frames
  per other thread, home-path scrubbing to `~`, and the 7000-character
  GitHub issue URL budget.
- Extraction (`ArchiveExtractor`, from `extract/`): formats are detected
  from magic bytes (ZIP `PK`, 7z, RAR, `ustar` at offset 257, gzip
  `1F 8B`, xz `FD 37 7A 58 5A 00`). Entries must be relative, without
  `..`, drive prefixes or `:`, and symlinks are never followed. Limits
  are `max(200 x archive size, 20 GiB)` total, 100,000 entries and a
  depth of 64, counting bytes read from the decoder including skipped
  entries. Files are extracted to a staging folder and then moved into
  place, with taken names numbered.
- Geo lookup (`GeoDatabase`, from `geo/`): the bundled table format is
  magic `MDMGEO` version 1, a list of two-letter country codes, then
  IPv4 and IPv6 sections of address-delta LEB128 entries with a sparse
  index every 256 entries. Reserved addresses return no country. The
  flag atlas has 250 flags of 32x24 pixels in 16 columns. Attribution:
  "IP Geolocation by DB-IP" (`https://db-ip.com`).
- IPC (`IpcApi`, from `ipc/`): one JSON command per message, at most
  1 MiB per message, 8 in flight, 2 seconds to answer, 3 seconds per
  request. The token is 64 random hex characters, regenerated at each
  start.
- Lab server (`LabServer`, from `labserver/`): the test-only route
  table, including the generated-file, redirect, HLS/DASH, RSS and fake
  debrid routes, on the default bind `127.0.0.1:8090`. It is never
  started by the app.
- Speed display (`SpeedTracker`, from `app/model.rs`): exponential average
  with a time constant of 3.0 seconds, shown value refreshed every 0.5
  seconds, speed reported as zero after 5.0 seconds without bytes.
- Checksums (`ChecksumVerifier`, from `engine/verify.rs` and
  `app/checksum.rs`): CRC32, MD5, SHA-1, SHA-256 and SHA-512. A checksum
  may be written with a name (`sha256:…`, `md5=…`) or recognised by its
  length (8, 32, 40, 64 or 128 hex characters). The strongest hash wins
  when several are offered. Verification reads the file in 1 MiB chunks.
- Categories (from `app/model.rs` and `app/categories.rs`): built-in
  categories are General, Compressed, Documents, Music, Programs, Video
  and Torrents, with the upstream shipped extension lists. Custom
  categories are matched before built-in ones. A pattern ending in `*`
  matches a prefix, other patterns match exactly. Torrents is never
  matched from a file name. General and Torrents cannot be deleted.
- Queue (from `app/queue.rs`): a queue downloads 4 files at a time by
  default. A failed member is retried after 30 seconds, forever when no
  retry limit is set. A manually held member is left alone until it is
  started again.
- Settings defaults (from `app/settings.rs`): speed limit 10 KB/s per file
  when enabled, global limit 500 KB/s, 8 connections per download, 32
  connections maximum (128 when many connections are unlocked), download
  limit 200 MB per 5 hours, torrent upload limit 100 KB/s, stop ratio
  2.00, stop after 1440 minutes, interface size 100 percent.
- Downloads are persisted in `downloads.json`, format version 2, with
  atomic writes and backup generations `.bak`, `.1.bak` and `.2.bak`.
  A download that was running when the app stopped is restored as
  Stopped. The download log keeps at most 50 entries.
- Download item log, credentials from the browser, and the archive
  password are never written to the store in plain form; the port stores
  no browser credentials at all.
- `PRODUCT.md` and `DESIGN.md` in the upstream repository describe the
  project website, not the desktop application, so they are not used as
  the specification for this Android user interface.

## Platform adaptations

- egui desktop UI becomes Jetpack Compose screens: download list, add
  download, download detail, settings and categories.
- tokio async tasks become Kotlin coroutines on `Dispatchers.IO`, with
  cancellation mapped to the Stopped status.
- The upstream HTTP client becomes OkHttp with the same Range requests,
  redirect following, proxy support and credential handling.
- The upstream Windows, macOS and Linux system APIs (tray icon, wake
  timers, power management, file icons, native dialogs) are replaced by
  Android equivalents: a foreground service with a low-importance
  notification channel, `WorkManager` for background work, and the
  system file picker. Wake timers and shutting the computer down have no
  Android equivalent and are not implemented.
- Downloads are executed by the foreground service, not by the UI.
  Adding, resuming or retrying a download starts the service, which
  runs queued downloads one at a time and then stops itself when the
  queue is empty. The UI and the service share one repository and one
  engine through `DownloadContainer`, so progress, status and the
  corrected file name reach the list live. Progress updates are
  offered to a conflated channel the moment they happen, and a single
  consumer hands them to the list one by one; if the UI is slower
  than the download, the channel keeps only the newest update instead
  of queueing a backlog, so the list always shows the latest progress
  with no fixed delay and no spam. Pausing from the UI cancels the
  running engine job. New downloads go to the folder from Settings
  when it names an absolute path, otherwise to the app-specific
  Download folder; the folder can be typed in or picked with the
  Browse button, which opens the system folder picker. A picked folder
  is resolved to a path on device storage and checked for writability
  before it is saved, and a folder that cannot be used or written to
  shows an error instead of being saved. The engine creates the folder
  before writing, and
  a folder that cannot be created fails the download at once with a
  clear message instead of retrying. The total size from the HEAD
  response is shown as soon as it is known. If the server rejects a
  requested part of the file with HTTP 416, the engine discards the
  partial file and restarts the download from the beginning, at most
  twice, instead of failing at once, because a 416 means the range
  the app asked for no longer matches the file on the server. On boot
  the receiver starts the service, which resumes queued downloads
  when `resumeOnStart` is on.
- The upstream `unsafe_code = "deny"` workspace lint is honoured in
  spirit: this port contains no native code and no unsafe calls.
- The theme follows the system. Material 3 DayNight is used, with
  dynamic colour on Android 12 and later and a fixed light and dark
  scheme below that. The app never forces light or dark mode.

## Android compatibility

- `minSdk = 24` (Android 7.0), `targetSdk = 36`, `compileSdk = 36`.
- Only APIs available on API 24 are used without a guard. Every newer
  API is guarded by a version check or provided by AndroidX. The APIs
  used, with their minimum level, are listed below.

| API used | Minimum API level | Guard or fallback |
|---|---|---|
| Notification channels (`NotificationChannel`) | 26 | Created only when `SDK_INT >= 26` (Android 8). Below that the notification is posted without a channel. |
| `POST_NOTIFICATIONS` runtime permission | 33 | Requested only when `SDK_INT >= 33` (Android 13). Below that notifications are allowed by default. |
| Foreground service (`startForegroundService`, `FOREGROUND_SERVICE_DATA_SYNC`) | 26 for the call, 29 for the type, 34 for the dataSync permission | `startForegroundService` is used on API 26 and later, `startService` below it. The service declares `foregroundServiceType="dataSync"`, which the system ignores below API 29. |
| `PendingIntent.FLAG_IMMUTABLE` | 23 | Safe on minSdk 24. |
| Material 3 dynamic colour (`dynamicDarkColorScheme`, `dynamicLightColorScheme`) | 31 | Used only when `SDK_INT >= 31` (Android 12); a fixed light and dark scheme is used below that. |
| `FileProvider` | 24 via AndroidX | Provided by `androidx.core`. |
| Scoped storage, app-specific external files (`getExternalFilesDir`) | 24 | No storage permission is requested. Files are written to app-specific storage, which needs no permission on any supported version. |
| `WorkManager` | 24 via AndroidX | Provided by `androidx.work`. |
| `java.time` / `java.nio` / `MessageDigest` / `CRC32` | 24 | Available on API 24. The port avoids `java.time` APIs added after API 26. |
| `SecureRandom` | 24 | Available on API 24. |
| `android.os.Build.MODEL`, `VERSION.SDK_INT` | 24 | Available on API 24. |

Behaviour changes considered per version:

- Android 8 (API 26): notification channels are required; the port
  creates the downloads channel before starting the foreground service.
- Android 10 (API 29): scoped storage; the port writes only to
  app-specific storage and never requests broad storage access.
- Android 11 (API 30): package visibility and scoped storage
  enforcement; the port declares no package queries and uses no
  `MANAGE_EXTERNAL_STORAGE`.
- Android 12 (API 31): dynamic colour and the approximate-location
  model; the port uses dynamic colour only on this level and above and
  requests no location permission. Pending intents must be mutable or
  immutable; the port always uses `FLAG_IMMUTABLE`.
- Android 13 (API 33): the notification permission is runtime; the port
  requests `POST_NOTIFICATIONS` only on this level and above, and
  continues to download without it when denied. Media permissions are
  not used.
- Android 14 (API 34): foreground service types are enforced; the
  service declares `dataSync` and the matching permission, and the
  service stops itself when its work finishes.
- Android 15 (API 35) and 16 (API 36): 16 KB page size and edge-to-edge
  enforcement; the port uses no native libraries, so page size does not
  apply, and the Compose layout uses the scaffold padding for system
  bars.

## What was verified

- Static verification was performed on this machine after the last
  edits: every Kotlin file is in package `com.misterdownloadmanager.port`,
  every XML file parses, brace and bracket balance holds in every
  Kotlin file after string literals are stripped, the manifest declares
  the same applicationId, every string referenced from code exists in
  `strings.xml`, every vector drawable referenced exists, the project
  contains no emoji and no code comments, and all UI strings in
  `strings.xml` are English. The unit tests in `PortLogicTest` cover
  the segment planner (including the aligned and unaligned split), resume
  tiling, checksum parsing (including rejection of a wrong length for a
  named algorithm), category matching, file naming (230-character cap,
  reserved names, numbering from 1, and preferring the `file=` query
  value over a script endpoint such as `download.php`), download
  folder resolution (absolute paths from Settings, blank and relative
  values falling back to the default), magnet parsing,
  tracker filtering, Click'n'Load decryption with the key as IV, the
  eight-digit pairing code and its ten-minute expiry, DNS presets,
  archive magic detection and safe paths, geo flag cells and reserved
  addresses, the retry backoff, the CLI command set, and the IPC limits.
- JDK 17 was installed in this environment and the Android SDK
  command-line tools, platform API 36, build-tools 36.0.0 and
  platform-tools were installed from the official Google downloads.
- The Gradle build was attempted in this environment and could not
  complete: the daemon starts, but the client cannot receive a message
  from it. The daemon handshake over loopback TCP fails with a
  deserialization error (`Unexpected type tag 109`), which means the
  loopback socket stream is being interfered with in this sandbox,
  even though a plain TCP connection to `127.0.0.1` succeeds. The same
  failure happens with `--no-daemon`, `org.gradle.daemon=false`, IPv6
  loopback flags and `--offline`, and with the installed Gradle 8.7
  binary as well as `./gradlew`. The wrapper jar originally shipped
  here was the wrong artifact (the distribution's 49,625-byte
  bootstrap container jar, whose outer `GradleWrapperMain` references
  the non-existent `org.gradle.wrapper.IDownload`); it has been
  replaced with the real flat project wrapper jar (43,453 bytes,
  SHA-256 `cb0da6751c2b753a16ac168bb354870ebb1e162e9083f116729cec9c781156b8`)
  from the Gradle project's own `v8.7.0` wrapper, and `./gradlew`
  now starts and begins downloading the distribution (blocked in
  this VM only by the sandbox proxy), so the remaining failure is an
  environment limitation, not a project error.
- The Kotlin code was type-checked directly with kotlinc 2.0.21 (the
  version the project declares), the Compose compiler plugin 2.0.21
  and the serialization compiler plugin, against the real dependency
  artifacts resolved from Google Maven and Maven Central (Compose
  1.7.2, Material 3 1.3.0, lifecycle 2.8.7, activity 1.9.3, coroutines
  1.8.1, OkHttp 4.12.0, serialization 1.7.3) plus `android.jar` from
  platform API 36 and `R`/`BuildConfig` stubs generated from the
  project's own resources (in a real Gradle build those two classes
  are generated by the Android Gradle plugin). All 49 of the 49 main
  Kotlin files compile cleanly with 0 errors, including the 11 files
  that were previously unverified: the 5 Compose screens
  (`DownloadListScreen`, `AddDownloadScreen`, `DownloadDetailScreen`,
  `SettingsScreen`, `CategoriesScreen`), `DownloadViewModel`,
  `DownloadService`, `MainActivity`, `MisterDownloadManagerApp`,
  `ui/theme/Theme` and `ui/icons/MdmIcons`. The only diagnostics are
  two deprecation warnings for the `Locale(String, String)`
  constructor in `core/geo/GeoInfo.kt`, which is still functional on
  every supported API level. This check found and fixed three real
  errors in those 11 files: `CategoriesScreen` was missing the
  `stringResource` and `R` imports; `DownloadViewModel` called
  `Category.forUrl`, which does not exist, and now uses the existing
  `Category.of(name)` classifier on the file name already derived
  from the URL; and the classpath was missing
  `lifecycle-common-jvm` (which provides `LifecycleOwner`) and
  `navigation-runtime`, both now resolved from Google Maven. The
  earlier kotlinc 1.9.22 check against hand-written stubs had already
  found and fixed four errors (a missing `password` field on
  `DownloadItem`, a non-exhaustive `when` on the proxy scheme, a
  non-comparable `Pair` selector in the planner, and `fromHeaders`
  placed on the wrong companion object).
- All 24 unit tests in `PortLogicTest` pass under JUnit 4.13.2,
  re-run with kotlinc 2.0.21 against the freshly compiled project
  classes (run directly with the kotlinc output, since Gradle cannot
  run here). Running them earlier caught two more real bugs
  (case-insensitive magnet parsing and a wrong resume-bytes
  expectation), both fixed.
- Live testing against a real signed download link
  (`download.php?file=...7z`, 3.6 GB, nginx, `Accept-Ranges: bytes`,
  `Content-Disposition` with the real file name) found and fixed two
  real bugs that static checks could not see: `FileNaming.fromUrl`
  used the last path segment whenever it contained a dot, so a script
  endpoint such as `download.php` became the file name even when the
  `file=` query value named the real file; and no download ever
  started, because nothing called `DownloadService.start`, the boot
  receiver did nothing, the service's repository read a different
  `downloads.json` than the UI's and was never loaded, and engine
  progress went to a flow nobody collected. The fix wires the UI and
  the service to one shared repository and engine
  (`data/DownloadContainer`), starts the service when a download is
  added, resumed or retried, loads the shared store once, forwards
  engine progress to the list, cancels the running job when the item
  is paused or deleted, and lets the engine rename the target file to
  the `HEAD` response's `Content-Disposition` name before writing.
  Testing the fixed build on a real device found one more bug the
  static checks could not see: adding a download ignored the folder
  from Settings entirely and always used the app-specific Download
  folder, and the engine never created that folder, so the first
  download on a fresh install failed opening the target file with
  `ENOENT`. The folder from Settings is now used for new downloads,
  and the engine creates the destination folder before writing.
  Testing the fixed build on a real device then found an HTTP 416:
  the server rejects any range that starts at or after the end of
  the file, and the app used to fail at once on it. The engine now
  discards the partial file and restarts the download from the
  beginning, at most twice, and only then fails with a clear message.
  Testing the fixed build on a real device then found a stuck
  notification: when the queue emptied, the service stopped with
  `STOP_FOREGROUND_DETACH`, which detaches the service from the
  notification but leaves the notification itself posted, and because
  it is ongoing it cannot be swiped away, so the "downloading"
  notification stayed forever after a failed download. The service
  now stops with `STOP_FOREGROUND_REMOVE`, which removes the
  notification as well, and cancels it again in `onDestroy` so no
  stop path can leave it behind.

## What still needs testing on a real device or emulator

- A full `./gradlew assembleDebug` run has not yet been completed in
  this environment. Run it before release, and run the app on at
  least one device or emulator per Android version:
  Android 7.0 (API 24), 8.0 (API 26), 10 (API 29), 11 (API 30),
  12 (API 31), 13 (API 33), 14 (API 34) and 15 or 16 (API 35/36).
- On each version, check: the app starts, the notification permission
  prompt appears only on API 33 and later, the foreground notification
  appears while a download runs, downloads pause and resume, the theme
  follows the system in both light and dark mode, and the dynamic
  colour is applied only on API 31 and later.
- Torrent peer connections, debrid unrestrict calls (especially TorBox
  and MegaDebrid, whose flows follow the documented endpoints but were
  not tested against a live account) and FTP transfers need a live
  server and network access on the test device; they were verified only
  by code review, not by a live transfer.
- The geo lookup needs the DB-IP country database file bundled as an
  asset before it can return a country; without it, lookups return
  null.

## Limitations

- The torrent peer-to-peer wire protocol is implemented in upstream by
  librqbit. This port contains the torrent state machine, magnet
  parsing, piece selection and tracker state, but not a full
  BitTorrent peer implementation. A torrent download therefore needs a
  native BitTorrent engine to be added before it can transfer data.
- yt-dlp, ffmpeg and gallery-dl are external desktop binaries in
  upstream. They are not bundled in this Android port, so video and
  gallery downloads are detected and listed but cannot be fetched by
  those tools yet.
- The browser extension, the native messaging host, the desktop
  clipboard watcher and the browser-capture pipeline have no Android
  equivalent. Links can be added from the app itself or from Android
  share intents instead.
- Remote control over the local network and the headless server mode
  are modelled (pairing, device list) but the HTTPS remote server from
  upstream is not started by this port.
- The lab server is a test-only tool from upstream and is not started
  by the app.
- The Click'n'Load loopback listener on port 9666 is not started on
  Android; its decryption and link-filtering logic is kept, and links
  arrive through share intents instead.
- 7z and RAR extraction need an external 7-Zip binary on desktop; on
  Android they are reported as NoTool until a native library is added.
  XZ decompression needs an XZ library, which is not bundled.
- DoT and DoH DNS transport is not implemented; the system resolver is
  used and the upstream DNS presets are kept for configuration.
- PAC proxy scripts are not executed on Android (upstream runs them
  with an embedded JavaScript engine); manual and system proxies are
  applied as configured. Per-host learned connection caps from
  upstream are not implemented either; the configured connection limit
  is used for every host.
- The torrent peer-to-peer wire protocol needs a native BitTorrent
  engine to be added before it can transfer data.
- Themes from upstream (Classic, Metro, Adwaita, Breeze, Luna, Aero,
  Aqua and text theme files) are not ported. The Android port uses the
  system Material 3 theme only.
- The app interface is English only. Upstream ships 86 languages; no
  other language is included in this port.
