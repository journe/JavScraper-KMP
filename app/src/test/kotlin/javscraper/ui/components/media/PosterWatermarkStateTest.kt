package javscraper.ui.components.media

import javscraper.models.Video
import javscraper.models.WatermarkMark
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PosterWatermarkStateTest {

    @Test
    fun `default state maps inferred marks to dialog groups`() {
        val video = Video(
            number = "ABP-123",
            path = "F:/Videos/ABP-123-4k.mp4",
            tags = listOf("中文字幕", "无码")
        )

        val state = defaultWatermarkState(video, enabled = true)

        assertEquals(WatermarkMark.HD_4K, state.hdMark)
        assertTrue(state.subtitle)
        assertEquals(WatermarkMark.UNCENSORED, state.mosaicMark)
    }

    @Test
    fun `default state keeps mosaic empty when only censored is recognized`() {
        val state = defaultWatermarkState(
            Video(number = "ABP-123", tags = listOf("有码")),
            enabled = true
        )

        assertNull(state.hdMark)
        assertFalse(state.subtitle)
        assertNull(state.mosaicMark)
    }

    @Test
    fun `default state prefers umr over leak and uncensored`() {
        val umr = defaultWatermarkState(
            Video(number = "ABP-123", tags = listOf("破解", "无码")),
            enabled = true
        )
        assertEquals(WatermarkMark.UMR, umr.mosaicMark)

        val leak = defaultWatermarkState(
            Video(number = "ABP-123", tags = listOf("流出", "无码")),
            enabled = true
        )
        assertEquals(WatermarkMark.LEAK, leak.mosaicMark)
    }

    @Test
    fun `to options returns null when disabled or empty`() {
        val disabled = PosterWatermarkState(
            enabled = false,
            hdMark = WatermarkMark.HD_4K,
            subtitle = true,
            mosaicMark = WatermarkMark.UNCENSORED
        )
        assertNull(disabled.toOptions(5))

        val empty = PosterWatermarkState(enabled = true)
        assertNull(empty.toOptions(5))
    }

    @Test
    fun `to options keeps manually selected censored mark`() {
        val state = PosterWatermarkState(
            enabled = true,
            mosaicMark = WatermarkMark.CENSORED
        )

        val options = state.toOptions(5)

        assertEquals(listOf(WatermarkMark.CENSORED), options?.marks)
        assertEquals(5, options?.size)
    }

    @Test
    fun `to options orders hd before subtitle and mosaic`() {
        val state = PosterWatermarkState(
            enabled = true,
            hdMark = WatermarkMark.HD_8K,
            subtitle = true,
            mosaicMark = WatermarkMark.LEAK
        )

        assertEquals(
            listOf(WatermarkMark.HD_8K, WatermarkMark.SUB, WatermarkMark.LEAK),
            state.toOptions(5)?.marks
        )
    }
}