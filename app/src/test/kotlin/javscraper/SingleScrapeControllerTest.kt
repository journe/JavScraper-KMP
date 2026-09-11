package javscraper

import javscraper.models.ScannedFile
import javscraper.models.SingleScrapeDialogState
import javscraper.models.Video
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlin.test.assertEquals

class SingleScrapeControllerTest {

    @Test
    fun `showing preview publishes network candidates`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        val candidate = Video(number = "ABC-001", detailUrl = "https://example.com/1")
        var published = emptyList<Video>()

        controller.onPreviewCandidates = { published = it }
        controller.showPreview(listOf(candidate))

        assertEquals(listOf(candidate), published)
        assertEquals(
            SingleScrapeDialogState.Preview(listOf(candidate)),
            controller.singleScrapeDialogState
        )
    }

    @Test
    fun `updating number keeps task display in sync`() {
        val controller = SingleScrapeController(
            scope = kotlinx.coroutines.test.TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        controller.openSingleScrape(
            ScannedFile(path = "video.mp4", fileName = "video.mp4", number = "OLD-001")
        )

        controller.updateSingleScrapeNumber("NEW-001")

        assertEquals("NEW-001", controller.singleScrapeTask?.number)
    }

    @Test
    fun `scrape failure keeps timeout stage for localized ui`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )

        controller.failSingleScrape(
            message = "Saving webpage timed out",
            stage = "webpage_archive"
        )

        assertEquals("webpage_archive", controller.singleScrapeErrorStage)
    }

    @Test
    fun `timeout stage translations distinguish request and archive`() {
        val translations = javscraper.i18n.TranslationZh()

        assertEquals("请求网页超时：站点响应过慢，可重试或更换站点。", translations.singleScrapeErrorWebpageRequestTimeout)
        assertEquals(
            "保存网页超时：元数据已找到，但归档页面资源耗时过长；可关闭“下载网页”后重试。",
            translations.singleScrapeErrorWebpageArchiveTimeout
        )
        assertEquals(
            "Worker 响应超时且未收到明确阶段；请查看应用日志中的最后一条 Scrape progress。",
            translations.singleScrapeErrorWorkerTimeout
        )
    }
}
