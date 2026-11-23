package com.misterdownloadmanager.port.core.model

sealed class DownloadError {
    data class Network(val message: String) : DownloadError()
    data class Http(val statusCode: Int, val message: String) : DownloadError()
    data class DiskFull(val message: String) : DownloadError()
    data class ChecksumMismatch(val expected: String, val actual: String) : DownloadError()
    data class FileExists(val fileName: String) : DownloadError()
    data class Unauthorized(val message: String) : DownloadError()
    data class Forbidden(val message: String) : DownloadError()
    data class NotFound(val message: String) : DownloadError()
    data class InvalidUrl(val url: String) : DownloadError()
    data class Unknown(val message: String) : DownloadError()
    object BotCheck : DownloadError()
    data class Throttled(val statusCode: Int, val retryAfterSeconds: Long?) : DownloadError()
    data class BadRange(val detail: String) : DownloadError()
    object ShortBody : DownloadError()
    object FileChanged : DownloadError()
    object NoRanges : DownloadError()
    object RangesUnavailable : DownloadError()
    object LinkExpired : DownloadError()
    object SlowConnection : DownloadError()
    data class FtpReply(val code: Int, val text: String) : DownloadError()
    data class Certificate(val problem: String) : DownloadError()
    object NoCertificates : DownloadError()
    data class NoSpace(val needed: Long, val free: Long) : DownloadError()
    data class TooBigForDrive(val size: Long, val fileSystem: String) : DownloadError()
    object Cancelled : DownloadError()
    data class InUse(val path: String) : DownloadError()
    data class DestinationUnavailable(val path: String) : DownloadError()
    object DriveGone : DownloadError()
    data class PathTooLong(val path: String) : DownloadError()
    object GotWebPage : DownloadError()
    data class NotTheFile(val size: Long, val sent: Long) : DownloadError()

    fun message(): String {
        return when (this) {
            is Network -> message
            is Http -> "HTTP $statusCode: $message"
            is DiskFull -> message
            is ChecksumMismatch -> "Checksum does not match. Expected $expected, got $actual."
            is FileExists -> "A file named $fileName already exists."
            is Unauthorized -> message
            is Forbidden -> message
            is NotFound -> message
            is InvalidUrl -> "Invalid URL: $url"
            is Unknown -> message
            is BotCheck -> "This site only accepts browsers."
            is Throttled -> "The server is busy (HTTP $statusCode). Try again later."
            is BadRange -> "The server sent an unexpected part of the file."
            is ShortBody -> "The connection closed before the part was complete."
            is FileChanged -> "The file changed on the server."
            is NoRanges -> "The server cannot send parts of the file, so it cannot be resumed."
            is RangesUnavailable -> "The server did not send the requested part."
            is LinkExpired -> "The link has expired."
            is SlowConnection -> "Far slower than the other connections; reconnecting."
            is FtpReply -> "The FTP server answered $code $text."
            is Certificate -> "Certificate problem: $problem."
            is NoCertificates -> "This device has no certificates."
            is NoSpace -> "Not enough disk space: $needed bytes needed, $free bytes available."
            is TooBigForDrive -> "$size bytes is too large for this $fileSystem drive."
            is Cancelled -> "Cancelled."
            is InUse -> "Already downloading: $path."
            is DestinationUnavailable -> "The folder $path is not available."
            is DriveGone -> "The drive or share is no longer available."
            is PathTooLong -> "The path is too long: $path."
            is GotWebPage -> "The site sent a web page, not a file."
            is NotTheFile -> "The server only sent $sent bytes, but the file is $size bytes."
        }
    }

    companion object {
        fun fromHttpStatus(statusCode: Int): DownloadError {
            return when (statusCode) {
                401 -> Unauthorized("Authorization required.")
                403 -> Forbidden("Access forbidden.")
                404 -> NotFound("File not found on the server.")
                429 -> Throttled(statusCode, null)
                else -> Http(statusCode, "Server returned HTTP $statusCode.")
            }
        }

        fun wantsNewAddress(statusCode: Int): Boolean {
            return statusCode == 401 || statusCode == 403 || statusCode == 404 || statusCode == 410
        }
    }
}
