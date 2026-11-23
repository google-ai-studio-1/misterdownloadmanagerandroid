package com.misterdownloadmanager.port

import com.misterdownloadmanager.port.core.cnl.ClickNLoad
import com.misterdownloadmanager.port.core.cli.CliCommand
import com.misterdownloadmanager.port.core.extract.ArchiveExtractor
import com.misterdownloadmanager.port.core.engine.Claim
import com.misterdownloadmanager.port.core.engine.DownloadEngine
import com.misterdownloadmanager.port.core.engine.FileNaming
import com.misterdownloadmanager.port.core.engine.SavedSegment
import com.misterdownloadmanager.port.core.engine.SegmentPlanner
import com.misterdownloadmanager.port.core.extract.ArchiveFormat
import com.misterdownloadmanager.port.core.extract.SafePath
import com.misterdownloadmanager.port.core.geo.GeoDatabase
import com.misterdownloadmanager.port.core.geo.GeoDatabaseFormat
import com.misterdownloadmanager.port.core.ipc.IpcCommand
import com.misterdownloadmanager.port.core.ipc.IpcLimits
import com.misterdownloadmanager.port.core.ipc.IpcToken
import com.misterdownloadmanager.port.core.mcp.McpTools
import com.misterdownloadmanager.port.core.model.Category
import com.misterdownloadmanager.port.core.model.Checksum
import com.misterdownloadmanager.port.core.model.ChecksumAlgorithm
import com.misterdownloadmanager.port.core.model.DownloadQueue
import com.misterdownloadmanager.port.core.model.QueueLogic
import com.misterdownloadmanager.port.core.model.QueueMemberState
import com.misterdownloadmanager.port.core.model.Settings
import com.misterdownloadmanager.port.core.engine.DnsPresets
import com.misterdownloadmanager.port.core.remote.RemoteDeviceTokens
import com.misterdownloadmanager.port.core.remote.RemotePairing
import com.misterdownloadmanager.port.core.torrent.MagnetLink
import com.misterdownloadmanager.port.core.torrent.TrackerFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortLogicTest {
    @Test
    fun plannerSplitsLargestSegmentInHalf() {
        val planner = SegmentPlanner(1_000_000L, 4096L)
        val first = planner.claim(0, null)
        assertTrue(first is Claim.Write)
        val second = planner.claim(1, null)
        assertTrue(second is Claim.Write)
        assertEquals(2, planner.segments().size)
    }

    @Test
    fun plannerResumeRequiresExactTiling() {
        val planner = SegmentPlanner.resume(
            1000L, 1L,
            listOf(SavedSegment(0L, 400L, 100L), SavedSegment(400L, 1000L, 400L))
        )
        assertEquals(1000L, planner.segments().sumOf { it.end - it.start })
        assertEquals(900L, planner.remainingBytes())
    }

    @Test
    fun checksumParsesByNameAndByLength() {
        val sha = Checksum.parse("sha256:" + "a".repeat(64))
        assertNotNull(sha)
        assertEquals(ChecksumAlgorithm.SHA256, sha?.algorithm)
        val md5 = Checksum.parse("b".repeat(32))
        assertEquals(ChecksumAlgorithm.MD5, md5?.algorithm)
        val sha512 = Checksum.parse("c".repeat(128))
        assertEquals(ChecksumAlgorithm.SHA512, sha512?.algorithm)
        assertNull(Checksum.parse("not-a-checksum"))
    }

    @Test
    fun categoryMatchesCustomFirstThenBuiltIn() {
        assertEquals(Category.VIDEO, Category.of("movie.mp4"))
        assertEquals(Category.MUSIC, Category.of("song.mp3"))
        assertEquals(Category.COMPRESSED, Category.of("part.r01"))
        assertEquals(Category.GENERAL, Category.of("README"))
        assertEquals(Category.GENERAL, Category.of("archive.unknown"))
    }

    @Test
    fun fileNamingSanitizesAndUniquifies() {
        assertEquals("a_b.txt", FileNaming.sanitize("a/b.txt"))
        assertEquals("file (1).txt", FileNaming.unique("file.txt") { it == "file.txt" })
        assertEquals("download", FileNaming.sanitize("   "))
        assertEquals("_CON.txt", FileNaming.sanitize("CON.txt"))
        assertEquals("_CONIN\$.txt", FileNaming.sanitize("CONIN\$.txt"))
        assertEquals(230, FileNaming.sanitize("a".repeat(300) + ".txt").length)
        assertEquals("setup.zip", FileNaming.fromUrl("https://example.com/download?file=setup.zip"))
    }

    @Test
    fun fileNamingPrefersQueryFileOverScriptEndpoint() {
        assertEquals(
            "Shin Megami Tensei - Persona 4 (PSP Port b450) [dreikk91].7z",
            FileNaming.fromUrl("https://meryl.retrogametalk.com/download.php?file=Shin%20Megami%20Tensei%20-%20Persona%204%20%28PSP%20Port%20b450%29%20%5Bdreikk91%5D.7z&id=303631&platform=psp&exp=1795455385&u=116188&w=0&key=f3ab8fafcf502a6f076e6e23a52836a5")
        )
        assertEquals("setup.zip", FileNaming.fromUrl("https://example.com/getfile.aspx?filename=setup.zip"))
        assertEquals("movie.mkv", FileNaming.fromUrl("https://example.com/DOWNLOAD.PHP?file=movie.mkv"))
        assertEquals("download", FileNaming.fromUrl("https://example.com/download.php?id=42"))
        assertEquals("archive.tar.gz", FileNaming.fromUrl("https://example.com/files/archive.tar.gz"))
        assertEquals("setup.zip", FileNaming.fromUrl("https://example.com/download?file=setup.zip"))
        assertEquals(
            "Shin Megami Tensei - Persona 4 (PSP Port b450) [dreikk91].7z",
            FileNaming.fromContentDisposition("attachment; filename=\"Shin Megami Tensei - Persona 4 (PSP Port b450) [dreikk91].7z\"; filename*=UTF-8''Shin%20Megami%20Tensei%20-%20Persona%204%20%28PSP%20Port%20b450%29%20%5Bdreikk91%5D.7z")
        )
    }

    @Test
    fun plannerSplitAlignsAndFallsBackToUnalignedMidpoint() {
        val planner = SegmentPlanner(100_000L, 12_288L)
        val first = planner.claim(0, null)
        assertTrue(first is Claim.Write)
        val second = planner.claim(1, null)
        assertTrue(second is Claim.Write)
        assertEquals(2, planner.segments().size)
        assertEquals(49_152L, planner.segments()[1].start)
        val small = SegmentPlanner(30_000L, 12_288L)
        assertTrue(small.claim(0, null) is Claim.Write)
        small.markReceived(0, 1_000L)
        assertTrue(small.claim(1, null) is Claim.Write)
        assertEquals(15_500L, small.segments()[1].start)
    }

    @Test
    fun checksumRejectsWrongLengthForNamedAlgorithm() {
        assertNull(Checksum.parse("sha256:" + "a".repeat(32)))
        assertNotNull(Checksum.parse("sha256:" + "a".repeat(64)))
        assertNotNull(Checksum.parse("sha256:" + "a".repeat(64) + " *file.iso"))
        assertNull(Checksum.parse("sha999:" + "a".repeat(64)))
        assertNull(Checksum.parse("zzzzzzzz"))
    }

    @Test
    fun magnetRequiresPrefixCaseInsensitive() {
        val magnet = MagnetLink.parse("MAGNET:?xt=urn:btih:" + "a".repeat(40))
        assertNotNull(magnet)
        assertEquals("a".repeat(40), magnet?.infoHashHex)
    }

    @Test
    fun trackerFilterRejectsLocalTrackers() {
        assertFalse(TrackerFilter.allowed("udp://tracker.localhost:80/announce", false))
        assertFalse(TrackerFilter.allowed("udp://192.168.1.1:80/announce", false))
        assertFalse(TrackerFilter.allowed("udp://tracker.example.com:80/announce", true))
        assertTrue(TrackerFilter.allowed("udp://tracker.example.com:80/announce", false))
        assertTrue(TrackerFilter.allowed("https://tracker.example.com/announce", true))
        assertFalse(TrackerFilter.isPublicIp("100.64.1.2"))
        assertTrue(TrackerFilter.isPublicIp("8.8.8.8"))
    }

    @Test
    fun clickNLoadDecryptsWithKeyAsIv() {
        val keyHex = "31323334353637383930393837363534"
        val jk = "function f() { return '31323334353637383930393837363534'; }"
        val key = ClickNLoad.keyOf(jk) ?: throw AssertionError("keyOf returned null")
        assertEquals(16, key.size)
        val cipher = javax.crypto.Cipher.getInstance("AES/CBC/NoPadding")
        val keySpec = javax.crypto.spec.SecretKeySpec(key, "AES")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, javax.crypto.spec.IvParameterSpec(key))
        val plain = "https://example.com/file.zip".toByteArray(Charsets.UTF_8)
        val padded = plain.copyOf(((plain.size + 15) / 16) * 16)
        val encrypted = cipher.doFinal(padded)
        val encoded = java.util.Base64.getEncoder().encodeToString(encrypted)
        val decrypted = ClickNLoad.decrypt(encoded, jk)
        assertEquals("https://example.com/file.zip", decrypted)
        assertEquals(listOf("https://example.com/file.zip"), ClickNLoad.lines(decrypted))
        assertTrue(ClickNLoad.lines("https://192.168.1.5/file.zip").isEmpty())
        assertTrue(ClickNLoad.isInternalLink("https://localhost/file.zip"))
    }

    @Test
    fun pairingCodeIsEightDigitsAndLastsTenMinutes() {
        val code = RemotePairing.generateCode(1_000_000L)
        assertEquals(8, code.code.length)
        assertTrue(code.code.all { it.isDigit() })
        assertEquals(1_000_000L + 600_000L, code.expiresEpochMillis)
        assertTrue(RemotePairing.verify(code, code.code, 1_000_000L + 599_999L))
        assertFalse(RemotePairing.verify(code, code.code, 1_000_000L + 600_001L))
        assertFalse(RemotePairing.verify(code, "00000000", 1_000_000L))
        val token = RemoteDeviceTokens.generate()
        assertEquals(64, token.length)
        assertEquals(64, RemoteDeviceTokens.hash(token).length)
        assertFalse(RemoteDeviceTokens.hash(token) == token)
    }

    @Test
    fun dnsPresetsMatchUpstream() {
        val cloudflare = DnsPresets.byId("cloudflare")
        assertNotNull(cloudflare)
        assertEquals("1.1.1.1", cloudflare?.plainServers?.first())
        assertEquals("one.one.one.one", cloudflare?.tlsName)
        assertEquals("https://cloudflare-dns.com/dns-query", cloudflare?.dohUrl)
        assertEquals(5, DnsPresets.ALL.size)
        assertFalse(DnsPresets.NEXTDNS.offersPlain)
        assertTrue(DnsPresets.isValidNextDnsProfile(""))
        assertTrue(DnsPresets.isValidNextDnsProfile("a1b2c3"))
        assertFalse(DnsPresets.isValidNextDnsProfile("a1b2c"))
        assertEquals("a1b2c3.dns.nextdns.io", DnsPresets.nextDnsTlsName("A1B2C3"))
    }

    @Test
    fun archiveDetectionUsesMagicBytes() {
        val temp = java.io.File.createTempFile("port-test", ".bin")
        temp.deleteOnExit()
        temp.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0))
        assertEquals(ArchiveFormat.ZIP, ArchiveExtractor().detectFormat(temp))
        temp.writeBytes(byteArrayOf(0x1F, 0x8B.toByte(), 0, 0, 0, 0, 0, 0))
        val gzFile = java.io.File.createTempFile("port-test", ".tar.gz")
        gzFile.deleteOnExit()
        gzFile.writeBytes(byteArrayOf(0x1F, 0x8B.toByte(), 0, 0, 0, 0, 0, 0))
        assertEquals(ArchiveFormat.TAR_GZ, ArchiveExtractor().detectFormat(gzFile))
        assertFalse(SafePath.isSafe("../escape.txt"))
        assertFalse(SafePath.isSafe("a.txt:Zone.Identifier"))
        assertTrue(SafePath.isSafe("folder/file.txt"))
        assertEquals(3, SafePath.depth("a/b/c.txt"))
    }

    @Test
    fun geoFlagCellsAndReservedAddresses() {
        assertEquals(Pair(0, 0), GeoDatabaseFormat.flagCell(1))
        assertEquals(Pair(32, 0), GeoDatabaseFormat.flagCell(2))
        assertEquals(Pair(0, 24), GeoDatabaseFormat.flagCell(17))
        assertNull(GeoDatabaseFormat.flagCell(0))
        assertTrue(GeoDatabase.isReservedAddress("192.168.0.1"))
        assertTrue(GeoDatabase.isReservedAddress("10.0.0.1"))
        assertTrue(GeoDatabase.isReservedAddress("127.0.0.1"))
        assertFalse(GeoDatabase.isReservedAddress("8.8.8.8"))
        assertNull(GeoDatabase.load(byteArrayOf(1, 2, 3)))
    }

    @Test
    fun retryWaitFollowsUpstreamBackoff() {
        val engine = DownloadEngine(Settings())
        assertEquals(0L, engine.retryWaitMillis(1, null))
        assertEquals(1_000L, engine.retryWaitMillis(2, null))
        assertEquals(2_000L, engine.retryWaitMillis(3, null))
        assertEquals(30_000L, engine.retryWaitMillis(20, null))
        assertEquals(300_000L, engine.retryWaitMillis(1, 600_000L))
        assertEquals(120_000L, engine.retryWaitMillis(1, 120_000L))
    }

    @Test
    fun cliParsesFullCommandSet() {
        val get = CliCommand.parse(listOf("get", "https://example.com/file.zip", "-c", "4", "--direct"))
        assertTrue(get is CliCommand.Get)
        assertEquals(4, (get as CliCommand.Get).connections)
        assertTrue(get.direct)
        assertTrue(CliCommand.parse(listOf("status", "--status", "Complete")) is CliCommand.Status)
        assertTrue(CliCommand.parse(listOf("torrent-get", "magnet:?xt=urn:btih:abc")) is CliCommand.TorrentGet)
        assertTrue(CliCommand.parse(listOf("api", "on")) is CliCommand.Api)
        assertTrue(CliCommand.parse(listOf("install-ytdlp")) is CliCommand.InstallTool)
        assertNull(CliCommand.parse(listOf("frobnicate")))
    }

    @Test
    fun ipcLimitsAndToken() {
        assertEquals(1_048_576, IpcLimits.MAX_MESSAGE_BYTES)
        assertEquals(8, IpcLimits.MAX_IN_FLIGHT)
        assertEquals(64, IpcToken.random().length)
        assertNotNull(IpcCommand.validateAdd(IpcCommand(url = "gopher://example.com")))
        assertNull(IpcCommand.validateAdd(IpcCommand(url = "magnet:?xt=urn:btih:abc")))
        assertEquals(7, McpTools.ALL.size)
        assertEquals(100, McpTools.DEFAULT_LIMIT)
    }

    @Test
    fun magnetParsesInfoHashAndTrackers() {
        val magnet = MagnetLink.parse("magnet:?xt=urn:btih:" + "a".repeat(40) + "&dn=Example&tr=udp%3A%2F%2Ftracker.example%3A80")
        assertNotNull(magnet)
        assertEquals("a".repeat(40), magnet?.infoHashHex)
        assertEquals("Example", magnet?.displayName)
        assertEquals(1, magnet?.trackers?.size)
        assertNull(MagnetLink.parse("https://example.com/file.zip"))
    }

    @Test
    fun queueStartsWaitingItemsWithinSlots() {
        val queue = DownloadQueue(name = "Queue # 1", filesAtOnce = 4, itemIds = listOf(1L, 2L, 3L, 4L, 5L))
        val members = mapOf<Long, QueueMemberState>(
            1L to QueueMemberState.RUNNING,
            2L to QueueMemberState.WAITING,
            3L to QueueMemberState.WAITING,
            4L to QueueMemberState.WAITING,
            5L to QueueMemberState.WAITING
        )
        val started = QueueLogic.toStart(queue, members, emptyMap(), emptyMap(), 100_000L)
        assertEquals(listOf(2L, 3L, 4L), started)
        assertFalse(QueueLogic.isFinished(queue, members, emptyMap()))
    }
}
