package com.misterdownloadmanager.port.core.cli

sealed class CliCommand {
    data class Get(
        val url: String,
        val output: String = ".",
        val connections: Int = 8,
        val headers: List<String> = emptyList(),
        val referer: String? = null,
        val userAgent: String? = null,
        val cookie: String? = null,
        val checksum: String? = null,
        val dns: String? = null,
        val adapter: String? = null,
        val proxy: String? = null,
        val direct: Boolean = false
    ) : CliCommand()

    data class Status(
        val id: Long? = null,
        val status: String? = null,
        val search: String? = null,
        val category: String? = null,
        val queue: String? = null,
        val limit: Int? = null
    ) : CliCommand()

    data class InstallTool(val tool: String, val proxy: String? = null) : CliCommand()
    object UpdateHelpers : CliCommand()

    data class GalleryGet(
        val url: String,
        val output: String? = null,
        val limit: Long? = null,
        val adapter: String? = null,
        val proxy: String? = null
    ) : CliCommand()

    data class VideoInfo(val url: String) : CliCommand()

    data class VideoGet(
        val url: String,
        val format: String? = null,
        val quality: String? = null,
        val output: String? = null,
        val subs: String = "none",
        val autoSubs: Boolean = false,
        val embedSubs: Boolean = false,
        val connections: Int = 8,
        val adapter: String? = null,
        val proxy: String? = null
    ) : CliCommand()

    data class PlaylistInfo(val url: String, val from: Int = 1, val count: Int? = null) : CliCommand()

    data class PlaylistGet(
        val url: String,
        val from: Int = 1,
        val count: Int? = null,
        val format: String = "bv*+ba/b",
        val output: String? = null,
        val connections: Int = 8,
        val proxy: String? = null
    ) : CliCommand()

    data class TorrentCreate(val path: String, val output: String) : CliCommand()
    data class TorrentInfo(val source: String) : CliCommand()

    data class TorrentGet(
        val source: String,
        val output: String? = null,
        val files: List<Int> = emptyList(),
        val seconds: Long? = null,
        val useExisting: Boolean = false
    ) : CliCommand()

    data class Pause(val ids: List<Long>) : CliCommand()
    data class Start(val ids: List<Long>) : CliCommand()
    data class Remove(val ids: List<Long>) : CliCommand()
    data class Delete(val ids: List<Long>) : CliCommand()

    data class Add(
        val url: String,
        val output: String? = null,
        val name: String? = null,
        val category: String? = null,
        val queue: String? = null,
        val checksum: String? = null,
        val referer: String? = null,
        val userAgent: String? = null,
        val cookie: String? = null,
        val later: Boolean = false,
        val silent: Boolean = false,
        val private: Boolean = false
    ) : CliCommand()

    data class StartQueue(val name: String? = null) : CliCommand()
    data class StopQueue(val name: String? = null) : CliCommand()
    object Pair : CliCommand()
    data class Api(val on: Boolean) : CliCommand()
    data class Resume(val stateFile: String) : CliCommand()
    object Version : CliCommand()
    object Help : CliCommand()

    companion object {
        const val FLAG: String = "--cli"
        const val NAME: String = "misterdownloadmanager-cli"

        fun parse(args: List<String>): CliCommand? {
            if (args.isEmpty()) return null
            val flags = args.drop(1)
            fun option(vararg names: String): String? {
                for (index in flags.indices) {
                    if (names.contains(flags[index]) && index + 1 < flags.size) return flags[index + 1]
                }
                return null
            }
            fun hasFlag(vararg names: String): Boolean = flags.any { names.contains(it) }
            fun ids(): List<Long> = flags.filter { it.toLongOrNull() != null }.map { it.toLong() }
            return when (args[0]) {
                "get" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    Get(
                        url = url,
                        output = option("-o", "--output") ?: ".",
                        connections = option("-c", "--connections")?.toIntOrNull() ?: 8,
                        headers = flags.windowed(2).filter { it[0] == "-H" || it[0] == "--header" }.map { it[1] },
                        referer = option("-r", "--referer", "--referrer"),
                        userAgent = option("--user-agent", "--ua"),
                        cookie = option("--cookie"),
                        checksum = option("--checksum"),
                        dns = option("--dns"),
                        adapter = option("--adapter"),
                        proxy = option("--proxy"),
                        direct = hasFlag("--direct")
                    )
                }
                "status", "list" -> Status(
                    id = option("--id")?.toLongOrNull(),
                    status = option("-s", "--status"),
                    search = option("--search"),
                    category = option("-c", "--category"),
                    queue = option("-q", "--queue"),
                    limit = option("-l", "--limit")?.toIntOrNull()
                )
                "install-ytdlp" -> InstallTool("yt-dlp", option("--proxy"))
                "install-ffmpeg" -> InstallTool("ffmpeg", option("--proxy"))
                "install-gallery-dl" -> InstallTool("gallery-dl", option("--proxy"))
                "install-js-runtime", "install-deno" -> InstallTool("node", option("--proxy"))
                "update-helpers" -> UpdateHelpers
                "gallery-get" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    GalleryGet(url, option("--output"), option("--limit")?.toLongOrNull(), option("--adapter"), option("--proxy"))
                }
                "video-info" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    VideoInfo(url)
                }
                "video-get" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    VideoGet(
                        url = url,
                        format = option("--format"),
                        quality = option("--quality"),
                        output = option("--output"),
                        subs = option("--subs") ?: "none",
                        autoSubs = hasFlag("--auto-subs"),
                        embedSubs = hasFlag("--embed-subs"),
                        connections = option("--connections")?.toIntOrNull() ?: 8,
                        adapter = option("--adapter"),
                        proxy = option("--proxy")
                    )
                }
                "playlist-info" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    PlaylistInfo(url, option("--from")?.toIntOrNull() ?: 1, option("--count")?.toIntOrNull())
                }
                "playlist-get" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    PlaylistGet(
                        url = url,
                        from = option("--from")?.toIntOrNull() ?: 1,
                        count = option("--count")?.toIntOrNull(),
                        format = option("--format") ?: "bv*+ba/b",
                        output = option("--output"),
                        connections = option("--connections")?.toIntOrNull() ?: 8,
                        proxy = option("--proxy")
                    )
                }
                "torrent-create" -> {
                    val path = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    TorrentCreate(path, option("-o") ?: return null)
                }
                "torrent-info" -> {
                    val source = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    TorrentInfo(source)
                }
                "torrent-get" -> {
                    val source = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    TorrentGet(
                        source = source,
                        output = option("--output"),
                        files = option("--files")?.split(',')?.mapNotNull { it.toIntOrNull() } ?: emptyList(),
                        seconds = option("--seconds")?.toLongOrNull(),
                        useExisting = hasFlag("--use-existing")
                    )
                }
                "pause" -> Pause(ids())
                "start" -> Start(ids())
                "remove" -> Remove(ids())
                "delete" -> Delete(ids())
                "add" -> {
                    val url = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    Add(
                        url = url,
                        output = option("-o", "--output"),
                        name = option("-f", "--name"),
                        category = option("-c", "--category"),
                        queue = option("-q", "--queue"),
                        checksum = option("--checksum"),
                        referer = option("--referer"),
                        userAgent = option("--user-agent"),
                        cookie = option("--cookie"),
                        later = hasFlag("-l", "--later"),
                        silent = hasFlag("-s", "--silent"),
                        private = hasFlag("-p", "--private")
                    )
                }
                "start-queue" -> StartQueue(flags.firstOrNull { !it.startsWith("-") })
                "stop-queue" -> StopQueue(flags.firstOrNull { !it.startsWith("-") })
                "pair" -> Pair
                "api" -> {
                    val value = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    Api(value == "on")
                }
                "resume" -> {
                    val file = flags.firstOrNull { !it.startsWith("-") } ?: return null
                    Resume(file)
                }
                "--version", "version" -> Version
                "--help", "help" -> Help
                else -> null
            }
        }
    }
}
