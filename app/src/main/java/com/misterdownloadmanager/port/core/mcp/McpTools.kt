package com.misterdownloadmanager.port.core.mcp

import kotlinx.serialization.Serializable

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String
)

@Serializable
data class McpToolResult(
    val content: String,
    val isError: Boolean = false
)

object McpTools {
    const val DEFAULT_LIMIT: Int = 100

    val LIST_DOWNLOADS = McpToolDefinition("list_downloads", "List downloads with status and progress.")
    val ADD_DOWNLOAD = McpToolDefinition("add_download", "Add a download from a URL.")
    val PAUSE_DOWNLOADS = McpToolDefinition("pause_downloads", "Pause downloads by id.")
    val RESUME_DOWNLOADS = McpToolDefinition("resume_downloads", "Resume downloads by id.")
    val REMOVE_DOWNLOADS = McpToolDefinition("remove_downloads", "Remove downloads by id, optionally deleting their files.")
    val START_QUEUE = McpToolDefinition("start_queue", "Start a queue.")
    val STOP_QUEUE = McpToolDefinition("stop_queue", "Stop a queue.")

    val ALL = listOf(
        LIST_DOWNLOADS, ADD_DOWNLOAD, PAUSE_DOWNLOADS, RESUME_DOWNLOADS,
        REMOVE_DOWNLOADS, START_QUEUE, STOP_QUEUE
    )

    fun addable(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://") ||
            url.startsWith("ftp://") || url.startsWith("ftps://") ||
            url.startsWith("magnet:")
    }
}
