package com.misterdownloadmanager.port.core.crash

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val timestampEpochMillis: Long,
    val appVersion: String,
    val deviceModel: String,
    val androidVersion: Int,
    val threadName: String,
    val exceptionType: String,
    val message: String?,
    val stackTrace: String
)

object CrashReporter {
    const val REPO: String = "anefzaoui/misterdownloadmanager"
    const val URL_BUDGET: Int = 7000
    const val OTHER_THREAD_FRAMES: Int = 12
    const val RUNNING_SHARE: Double = 0.6
    const val STARVED_SHARE: Double = 0.3

    enum class FreezeKind {
        RUNNING,
        WAITING,
        STARVED
    }

    fun classifyFreeze(cpuMillis: Long, wallMillis: Long, waiting: Boolean): FreezeKind {
        if (waiting) return FreezeKind.WAITING
        val share = if (wallMillis > 0L) cpuMillis.toDouble() / wallMillis.toDouble() else 0.0
        if (share >= RUNNING_SHARE) return FreezeKind.RUNNING
        if (share <= STARVED_SHARE) return FreezeKind.STARVED
        return FreezeKind.WAITING
    }

    fun signature(kind: String, function: String?): String {
        return if (function.isNullOrBlank()) kind else "$kind in $function"
    }

    fun scrub(text: String, homePath: String): String {
        if (homePath.isBlank()) return text
        return text.replace(homePath, "~").replace(homePath.replace('/', '\\'), "~")
    }

    fun newIssueUrl(title: String, report: String): String {
        val trimmed = report.take(URL_BUDGET)
        val encodedTitle = java.net.URLEncoder.encode(title, "UTF-8")
        val encodedReport = java.net.URLEncoder.encode(trimmed, "UTF-8")
        return "https://github.com/$REPO/issues/new?template=crash.yml&title=$encodedTitle&report=$encodedReport"
    }
    fun fromThrowable(throwable: Throwable, appVersion: String): CrashReport {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        return CrashReport(
            timestampEpochMillis = System.currentTimeMillis(),
            appVersion = appVersion,
            deviceModel = android.os.Build.MODEL ?: "unknown",
            androidVersion = android.os.Build.VERSION.SDK_INT,
            threadName = Thread.currentThread().name,
            exceptionType = throwable.javaClass.name,
            message = throwable.message,
            stackTrace = writer.toString()
        )
    }

    fun fileName(report: CrashReport): String {
        val format = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
        return "crash-${format.format(Date(report.timestampEpochMillis))}.txt"
    }

    fun write(report: CrashReport, directory: File): File {
        directory.mkdirs()
        val file = File(directory, fileName(report))
        val home = System.getProperty("user.home") ?: ""
        val stack = scrub(report.stackTrace, home)
        file.writeText(
            "---- Mister Download Manager crash report ----\n" +
                "Time: ${formatIsoUtc(report.timestampEpochMillis)}\n" +
                "Description: ${report.exceptionType}: ${report.message ?: ""}\n" +
                "Signature: ${signature("crash", report.exceptionType)}\n" +
                "Thread: ${report.threadName}\n" +
                "-- System details --\n" +
                "MDM ${report.appVersion}\n" +
                "Android ${report.androidVersion} (${report.deviceModel})\n" +
                "-- Rust backtrace --\n" +
                stack
        )
        return file
    }

    fun formatIsoUtc(epochMillis: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        format.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return format.format(Date(epochMillis))
    }
}

class CrashHandler(
    private val directory: File,
    private val appVersion: String,
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val report = CrashReporter.fromThrowable(throwable, appVersion)
            CrashReporter.write(report, directory)
        } catch (ignored: Exception) {
        }
        previous?.uncaughtException(thread, throwable)
    }

    companion object {
        fun install(directory: File, appVersion: String) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(directory, appVersion, previous))
        }
    }
}
