package javscraper.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelsTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Video serialization to JSON`() {
        val video = Video(
            number = "SONE-205",
            title = "完全タイトル",
            actresses = listOf("女優A", "女優B"),
            date = "2024-06-11",
            maker = "SOD Create",
            label = "STAR",
            director = "監督名",
            duration = 120,
            rating = 7.5,
            tags = listOf("高清画质", "中文字幕"),
            coverUrl = "https://example.com/cover.jpg",
            summary = "影片简介"
        )
        val jsonStr = json.encodeToString(Video.serializer(), video)
        val decoded = json.decodeFromString(Video.serializer(), jsonStr)
        assertEquals(video.number, decoded.number)
        assertEquals(video.title, decoded.title)
        assertEquals(video.maker, decoded.maker)
        assertEquals(2, decoded.actresses.size)
        assertNotNull(decoded.rating)
        assertEquals(7.5, decoded.rating!!, 0.001)
    }

    @Test
    fun `Video default values`() {
        val video = Video(number = "ABP-123")
        assertEquals("", video.title)
        assertEquals(emptyList<String>(), video.actresses)
        assertEquals(null, video.duration)
        assertEquals(null, video.rating)
        assertEquals("", video.webpage)
    }

    @Test
    fun `Video webpage content round-trip`() {
        val video = Video(number = "FC2-PPV-1723984", webpage = "bWFodG1s")
        val decoded = json.decodeFromString(Video.serializer(), json.encodeToString(Video.serializer(), video))
        assertEquals("bWFodG1s", decoded.webpage)
    }
    @Test
    fun `ScrapeResult success variant`() {
        val video = Video(number = "TEST-001")
        val result = ScrapeResult(success = true, data = video)
        assertTrue(result.success)
        assertNotNull(result.data)
        assertEquals("TEST-001", result.data!!.number)
    }

    @Test
    fun `ScrapeResult error variant`() {
        val error = ScrapeError(code = -1, message = "No number")
        val result = ScrapeResult(success = false, error = error)
        assertEquals(false, result.success)
        assertNotNull(result.error)
        assertEquals(-1, result.error!!.code)
    }

    @Test
    fun `ScannedFile creation`() {
        val sf = ScannedFile(path = "C:\\videos\\SONE-205.mp4", fileName = "SONE-205.mp4", number = "SONE-205")
        assertEquals("SONE-205", sf.number)
        assertEquals("SONE-205.mp4", sf.fileName)
    }

    @Test
    fun `ScannedFile empty number`() {
        val sf = ScannedFile(path = "/videos/unknown.avi", fileName = "unknown.avi")
        assertEquals("", sf.number)
    }

    @Test
    fun `SiteInfo data class`() {
        val site = SiteInfo(id = "javbus", name = "JavBus")
        assertEquals("javbus", site.id)
        assertEquals("JavBus", site.name)
    }

    @Test
    fun `ScrapeError default values`() {
        val error = ScrapeError()
        assertEquals(-1, error.code)
        assertEquals("", error.message)
    }

    @Test
    fun `Video JSON round-trip with special characters`() {
        val video = Video(
            number = "FC2-1234567",
            title = "完全タイトル with 日本語 & special: <>&\"'",
            actresses = listOf("女優A"),
            tags = listOf("日本語タグ")
        )
        val jsonStr = json.encodeToString(Video.serializer(), video)
        val decoded = json.decodeFromString(Video.serializer(), jsonStr)
        assertEquals(video.title, decoded.title)
        assertEquals("女優A", decoded.actresses[0])
    }
}
