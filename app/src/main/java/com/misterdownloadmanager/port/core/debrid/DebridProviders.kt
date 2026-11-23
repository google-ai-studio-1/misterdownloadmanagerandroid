package com.misterdownloadmanager.port.core.debrid

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

abstract class HttpDebridService : DebridService {
    protected val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30_000, TimeUnit.MILLISECONDS)
        .readTimeout(30_000, TimeUnit.MILLISECONDS)
        .build()

    private var lastCallMillis: Long = 0L

    protected suspend fun throttle() {
        val minInterval = 60_000L / provider.rateLimitPerMinute.coerceAtLeast(1)
        val wait = minInterval - (System.currentTimeMillis() - lastCallMillis)
        if (wait > 0L) kotlinx.coroutines.delay(wait)
        lastCallMillis = System.currentTimeMillis()
    }

    protected suspend fun postForm(url: String, token: String, fields: Map<String, String>): JSONObject {
        return withContext(Dispatchers.IO) {
            val bodyBuilder = FormBody.Builder()
            fields.forEach { (key, value) -> bodyBuilder.add(key, value) }
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .post(bodyBuilder.build())
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) throw IllegalStateException("Debrid request failed with HTTP ${response.code}.")
                JSONObject(text)
            }
        }
    }

    protected suspend fun getJson(url: String, token: String): JSONObject {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) throw IllegalStateException("Debrid request failed with HTTP ${response.code}.")
                JSONObject(text)
            }
        }
    }
}

class RealDebridService : HttpDebridService() {
    override val provider = DebridProvider.REAL_DEBRID

    override suspend fun isSupported(account: DebridAccount, link: String): Boolean {
        return link.startsWith("http://") || link.startsWith("https://")
    }

    override suspend fun unrestrict(account: DebridAccount, link: String): DebridResult {
        return try {
            throttle()
            val json = postForm(
                "${provider.baseUrl}/rest/1.0/unrestrict/link",
                account.apiKey,
                mapOf("link" to link, "remote" to "0")
            )
            val url = json.optString("download", "")
            if (url.isBlank()) return DebridResult.Error("Real-Debrid returned no download link.")
            val chunks = json.optInt("chunks", 0)
            DebridResult.Unrestricted(url, json.optString("filename").ifBlank { null }, json.optLong("filesize", -1L), if (chunks >= 1) chunks else null)
        } catch (e: Exception) {
            DebridResult.Error(e.message ?: "Real-Debrid request failed.")
        }
    }
}

class AllDebridService : HttpDebridService() {
    override val provider = DebridProvider.ALL_DEBRID

    override suspend fun isSupported(account: DebridAccount, link: String): Boolean {
        return link.startsWith("http://") || link.startsWith("https://") || link.startsWith("magnet:")
    }

    override suspend fun unrestrict(account: DebridAccount, link: String): DebridResult {
        return try {
            throttle()
            val json = postForm(
                "${provider.baseUrl}/v4/link/unlock",
                account.apiKey,
                mapOf("link" to link)
            )
            val data = json.optJSONObject("data") ?: return DebridResult.Error("AllDebrid returned no data.")
            val delayedId = data.optString("delayed", "")
            if (delayedId.isNotBlank()) {
                return pollDelayed(account, delayedId)
            }
            val url = data.optString("link", "")
            if (url.isBlank()) return DebridResult.Error("AllDebrid returned no download link.")
            DebridResult.Unrestricted(url, data.optString("filename").ifBlank { null }, data.optLong("filesize", -1L))
        } catch (e: Exception) {
            DebridResult.Error(e.message ?: "AllDebrid request failed.")
        }
    }

    private suspend fun pollDelayed(account: DebridAccount, delayedId: String): DebridResult {
        val deadline = System.currentTimeMillis() + 600_000L
        while (System.currentTimeMillis() < deadline) {
            throttle()
            val json = postForm(
                "${provider.baseUrl}/v4/link/delayed",
                account.apiKey,
                mapOf("id" to delayedId)
            )
            val data = json.optJSONObject("data") ?: return DebridResult.Error("AllDebrid delayed check returned no data.")
            val status = data.optInt("status", 0)
            if (status == 2) {
                val url = data.optString("link", "")
                if (url.isBlank()) return DebridResult.Error("AllDebrid returned no download link.")
                return DebridResult.Unrestricted(url, data.optString("filename").ifBlank { null }, data.optLong("filesize", -1L))
            }
            if (status == 3) return DebridResult.Error("AllDebrid reports the file is not available.")
            val timeLeft = data.optLong("time_left", 20L)
            val interval = maxOf(timeLeft / 4L, 5L) * 1000L
            kotlinx.coroutines.delay(interval)
        }
        return DebridResult.Error("AllDebrid delayed download timed out.")
    }
}

class PremiumizeService : HttpDebridService() {
    override val provider = DebridProvider.PREMIUMIZE

    override suspend fun isSupported(account: DebridAccount, link: String): Boolean {
        return link.startsWith("http://") || link.startsWith("https://")
    }

    override suspend fun unrestrict(account: DebridAccount, link: String): DebridResult {
        return try {
            throttle()
            val json = postForm(
                "${provider.baseUrl}/api/transfer/directdl",
                account.apiKey,
                mapOf("src" to link)
            )
            val content = json.optJSONArray("content")
                ?: return DebridResult.Error("Premiumize returned no content.")
            if (content.length() != 1) {
                return DebridResult.NotSupported("Premiumize returned a folder, which is not supported.")
            }
            val entry = content.optJSONObject(0)
                ?: return DebridResult.Error("Premiumize returned no content.")
            val url = entry.optString("link", "")
            if (url.isBlank()) return DebridResult.Error("Premiumize returned no download link.")
            val name = entry.optString("path", "").substringAfterLast('/')
            DebridResult.Unrestricted(url, name.ifBlank { null }, entry.optLong("size", -1L))
        } catch (e: Exception) {
            DebridResult.Error(e.message ?: "Premiumize request failed.")
        }
    }
}

class TorBoxService : HttpDebridService() {
    override val provider = DebridProvider.TORBOX

    override suspend fun isSupported(account: DebridAccount, link: String): Boolean {
        return link.startsWith("http://") || link.startsWith("https://") || link.startsWith("magnet:")
    }

    override suspend fun unrestrict(account: DebridAccount, link: String): DebridResult {
        return try {
            throttle()
            val created = postForm(
                "${provider.baseUrl}/v1/api/webdl/createwebdownload",
                account.apiKey,
                mapOf("link" to link)
            )
            val data = created.optJSONObject("data")
                ?: return DebridResult.Error("TorBox returned no web download.")
            val webId = data.optString("webdownload_id", "")
                .ifBlank { data.optString("id", "") }
            if (webId.isBlank()) return DebridResult.Error("TorBox returned no web download id.")
            val deadline = System.currentTimeMillis() + 3_600_000L
            while (System.currentTimeMillis() < deadline) {
                kotlinx.coroutines.delay(5_000L)
                throttle()
                val list = getJson(
                    "${provider.baseUrl}/v1/api/webdl/mylist?bypass_cache=true",
                    account.apiKey
                )
                val entries = list.optJSONArray("data") ?: continue
                var entry: JSONObject? = null
                for (index in 0 until entries.length()) {
                    val candidate = entries.optJSONObject(index) ?: continue
                    val candidateId = candidate.optString("id", "")
                    if (candidateId == webId || candidate.optString("webdownload_id", "") == webId) {
                        entry = candidate
                        break
                    }
                }
                val found = entry ?: continue
                if (!found.optBoolean("download_finished", false)) continue
                val files = found.optJSONArray("files")
                if (files == null || files.length() != 1) {
                    return DebridResult.NotSupported("TorBox returned more than one file, which is not supported.")
                }
                val file = files.optJSONObject(0) ?: continue
                val fileId = file.optLong("id", 0L)
                val fileName = file.optString("name", "").ifBlank { null }
                val fileSize = file.optLong("size", -1L)
                throttle()
                val linkJson = getJson(
                    "${provider.baseUrl}/v1/api/webdl/requestdl?token=${account.apiKey}&web_id=$webId&file_id=$fileId",
                    account.apiKey
                )
                val url = linkJson.optString("data", "")
                if (url.isBlank()) return DebridResult.Error("TorBox returned no download link.")
                return DebridResult.Unrestricted(url, fileName, fileSize)
            }
            DebridResult.Error("TorBox web download timed out.")
        } catch (e: Exception) {
            DebridResult.Error(e.message ?: "TorBox request failed.")
        }
    }
}

class MegaDebridService : HttpDebridService() {
    override val provider = DebridProvider.MEGA_DEBRID

    override suspend fun isSupported(account: DebridAccount, link: String): Boolean {
        return link.startsWith("http://") || link.startsWith("https://")
    }

    override suspend fun unrestrict(account: DebridAccount, link: String): DebridResult {
        return try {
            throttle()
            val json = postForm(
                "${provider.baseUrl}/api.php?action=debridLink",
                account.apiKey,
                mapOf("token" to account.apiKey, "link" to link)
            )
            val url = json.optString("debridLink", "")
            if (url.isBlank()) return DebridResult.Error("MegaDebrid returned no download link.")
            DebridResult.Unrestricted(url, null, -1L)
        } catch (e: Exception) {
            DebridResult.Error(e.message ?: "MegaDebrid request failed.")
        }
    }
}
