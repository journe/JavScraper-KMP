package javscraper.models

import kotlin.test.Test
import kotlin.test.assertEquals

class WatermarkTest {

    @Test
    fun `infer marks returns empty for video without watermark hints`() {
        val video = Video(number = "ABP-123", path = "F:/Videos/ABP-123.mp4")

        assertEquals(emptySet(), inferWatermarkMarks(video))
    }

    @Test
    fun `infer marks from subtitle and hd tags`() {
        val video = Video(number = "ABP-123", tags = listOf("中文字幕", "高清画质"))

        assertEquals(setOf(WatermarkMark.SUB), inferWatermarkMarks(video))
    }

    @Test
    fun `infer hd mark from filename case-insensitively`() {
        val lower = Video(number = "ABP-123", path = "F:/Videos/ABP-123-4k.mp4")
        assertEquals(setOf(WatermarkMark.HD_4K), inferWatermarkMarks(lower))

        val upper = Video(number = "ABP-123", path = "F:/Videos/ABP-123-8K.mp4")
        assertEquals(setOf(WatermarkMark.HD_8K), inferWatermarkMarks(upper))
    }

    @Test
    fun `infer mosaic marks from tags`() {
        val uncensored = Video(number = "ABP-123", tags = listOf("无码"))
        assertEquals(setOf(WatermarkMark.UNCENSORED), inferWatermarkMarks(uncensored))

        val umr = Video(number = "ABP-123", tags = listOf("破解"))
        assertEquals(setOf(WatermarkMark.UMR), inferWatermarkMarks(umr))

        val leak = Video(number = "ABP-123", tags = listOf("流出"))
        assertEquals(setOf(WatermarkMark.LEAK), inferWatermarkMarks(leak))
    }

    @Test
    fun `censored mark is never inferred by default`() {
        val video = Video(number = "ABP-123", tags = listOf("有码"))

        assertEquals(emptySet(), inferWatermarkMarks(video))
    }

    @Test
    fun `infer multiple marks together`() {
        val video = Video(
            number = "ABP-123",
            path = "F:/Videos/ABP-123-4k.mp4",
            tags = listOf("中文字幕", "无码")
        )

        val marks = inferWatermarkMarks(video)

        assertEquals(
            setOf(WatermarkMark.HD_4K, WatermarkMark.SUB, WatermarkMark.UNCENSORED),
            marks
        )
    }

    @Test
    fun `infer subtitle and leak marks from video version field`() {
        assertEquals(
            setOf(WatermarkMark.SUB),
            inferWatermarkMarks(Video(number = "ABP-123", version = "C"))
        )
        assertEquals(
            setOf(WatermarkMark.LEAK),
            inferWatermarkMarks(Video(number = "ABP-123", version = "U"))
        )
        assertEquals(
            setOf(WatermarkMark.SUB, WatermarkMark.LEAK),
            inferWatermarkMarks(Video(number = "ABP-123", version = "CU"))
        )
    }

    @Test
    fun `infer version marks case insensitively and ignores unknown version chars`() {
        assertEquals(
            setOf(WatermarkMark.SUB, WatermarkMark.LEAK),
            inferWatermarkMarks(Video(number = "ABP-123", version = "uc"))
        )
        assertEquals(
            emptySet(),
            inferWatermarkMarks(Video(number = "ABP-123", version = "2"))
        )
    }

    @Test
    fun `version marks combine with tag and filename hints`() {
        val video = Video(
            number = "ABP-123",
            path = "F:/Videos/ABP-123-4k.mp4",
            tags = listOf("无码"),
            version = "U"
        )

        assertEquals(
            setOf(WatermarkMark.HD_4K, WatermarkMark.LEAK, WatermarkMark.UNCENSORED),
            inferWatermarkMarks(video)
        )
    }

    @Test
    fun `watermark options normalize size`() {
        assertEquals(5, WatermarkOptions.DEFAULT_SIZE)
        assertEquals(1, WatermarkOptions.normalizedSize(0))
        assertEquals(10, WatermarkOptions.normalizedSize(99))
        assertEquals(7, WatermarkOptions.normalizedSize(7))
    }
}
