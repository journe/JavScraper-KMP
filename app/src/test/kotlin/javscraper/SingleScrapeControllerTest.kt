@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package javscraper

import javscraper.models.ScannedFile
import javscraper.models.SingleScrapeDialogState
import javscraper.models.ExistingVideoMetadata
import javscraper.models.Video
import javscraper.models.VideoUpdateField
import javscraper.scrape.ScrapeOptions
import javscraper.scrape.ScrapeOrchestrator
import javscraper.settings.AppSettings
import javscraper.sidecar.SidecarManager
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertIs
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import javscraper.ui.screens.ScrapeTask
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SingleScrapeControllerTest {

    @Test
    fun `opening a new scrape defaults to last selected site`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        val file = ScannedFile(path = "video.mp4", fileName = "video.mp4", number = "ABC-001")

        assertEquals(null, controller.singleScrapeSite)
        controller.updateSingleScrapeSite("javbus")
        controller.openSingleScrape(file)
        assertEquals("javbus", controller.singleScrapeSite)

        controller.closeSingleScrape()
        controller.openSingleScrape(file)

        assertEquals("javbus", controller.singleScrapeSite)
    }

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
    fun `field update selection merges only chosen fields and supports navigation`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        val existing = ExistingVideoMetadata(
            video = Video(number = "OLD-001", title = "Old title", summary = "Old plot"),
            presentFields = setOf(VideoUpdateField.NUMBER, VideoUpdateField.TITLE, VideoUpdateField.SUMMARY)
        )
        val incoming = Video(number = "NEW-001", title = "New title", summary = "New plot", maker = "New maker")
        val candidates = listOf(Video(number = "OTHER"), incoming)
        controller.showPreview(candidates)

        controller.selectPreviewCandidate(1)
        controller.showFieldUpdateSelection(candidates, 1, existing)

        val selection = controller.singleScrapeDialogState as SingleScrapeDialogState.FieldUpdateSelection
        assertEquals(incoming, selection.incoming)
        assertEquals(existing, selection.existing)
        assertTrue(selection.choices.single { it.field == VideoUpdateField.MAKER }.selected)

        controller.toggleFieldUpdateField(VideoUpdateField.SUMMARY, false)
        controller.confirmFieldUpdateSelection()

        val confirm = controller.singleScrapeDialogState as SingleScrapeDialogState.FieldUpdateConfirm
        assertEquals("Old plot", confirm.merged.summary)
        assertEquals("OLD-001", confirm.merged.number)

        controller.backToFieldUpdateSelection()
        controller.toggleFieldUpdateField(VideoUpdateField.SUMMARY, true)
        controller.confirmFieldUpdateSelection()
        val updatedConfirm = controller.singleScrapeDialogState as SingleScrapeDialogState.FieldUpdateConfirm
        assertEquals("New plot", updatedConfirm.merged.summary)

        controller.backToFieldUpdateSelection()
        controller.backToPreviewFromFieldUpdate()

        val preview = controller.singleScrapeDialogState as SingleScrapeDialogState.Preview
        assertEquals(1, preview.selectedIndex)
        assertEquals(candidates, preview.candidates)
    }

    @Test
    fun `configuring field update reads existing metadata without losing preview`() {
        val scope = TestScope()
        val folder = createTempDirectory("controller-field-update")
        val source = folder.resolve("ABC-001.mp4")
        source.writeText("video")
        folder.resolve("ABC-001.nfo").writeText("<movie><num>ABC-001</num><title>Old</title></movie>")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(updateMode = true))
        )
        val controller = SingleScrapeController(
            scope = scope,
            orch = { orchestrator },
            outputDir = { "output" },
            updateMode = { true }
        )
        val candidate = Video(number = "ABC-001", title = "New")
        controller.singleScrapeFile = ScannedFile(source.toString(), source.fileName.toString(), "ABC-001")
        controller.showPreview(listOf(candidate))

        controller.configureFieldUpdate()
        scope.advanceUntilIdle()
        waitForFieldUpdatePreparation(controller, scope)

        val selection = assertIs<SingleScrapeDialogState.FieldUpdateSelection>(controller.singleScrapeDialogState)
        assertEquals("ABC-001", selection.existing.video.number)
        assertEquals(candidate, selection.incoming)
    }

    @Test
    fun `field update metadata failure keeps preview candidates`() {
        val scope = TestScope()
        val folder = createTempDirectory("controller-field-update-missing")
        val source = folder.resolve("ABC-001.mp4")
        source.writeText("video")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(AppSettings(updateMode = true))
        )
        val controller = SingleScrapeController(
            scope = scope,
            orch = { orchestrator },
            outputDir = { "output" },
            updateMode = { true }
        )
        val candidate = Video(number = "ABC-001", title = "New")
        controller.singleScrapeFile = ScannedFile(source.toString(), source.fileName.toString(), "ABC-001")
        controller.showPreview(listOf(candidate))

        controller.configureFieldUpdate()
        scope.advanceUntilIdle()
        waitForFieldUpdatePreparation(controller, scope)

        val preview = assertIs<SingleScrapeDialogState.Preview>(controller.singleScrapeDialogState)
        assertEquals(listOf(candidate), preview.candidates)
        assertTrue(preview.error.orEmpty().contains("NFO"))
    }

    @Test
    fun `confirming field update writes merged video with selected fields`() {
        val scope = TestScope()
        val folder = createTempDirectory("controller-field-write")
        val source = folder.resolve("ABC-001.mp4")
        source.writeText("video")
        val nfo = folder.resolve("ABC-001.nfo")
        nfo.writeText("<movie><num>ABC-001</num><title>Old</title><plot>Old plot</plot></movie>")
        val orchestrator = ScrapeOrchestrator(
            sidecar = SidecarManager("unused-worker.exe"),
            options = ScrapeOptions.from(
                AppSettings(updateMode = true, scanDir = folder.toString(), downloadImages = false)
            )
        )
        val controller = SingleScrapeController(
            scope = scope,
            orch = { orchestrator },
            outputDir = { "output" },
            updateMode = { true }
        )
        val existing = ExistingVideoMetadata(
            video = Video(number = "ABC-001", title = "Old", summary = "Old plot"),
            presentFields = setOf(VideoUpdateField.NUMBER, VideoUpdateField.TITLE, VideoUpdateField.SUMMARY)
        )
        val incoming = Video(number = "ABC-001", title = "New", summary = "New plot")
        controller.singleScrapeFile = ScannedFile(source.toString(), source.fileName.toString(), "ABC-001")
        controller.showFieldUpdateSelection(listOf(incoming), 0, existing)
        controller.toggleFieldUpdateField(VideoUpdateField.TITLE, true)
        controller.toggleFieldUpdateField(VideoUpdateField.SUMMARY, true)
        controller.confirmFieldUpdateSelection()
        val confirm = controller.singleScrapeDialogState as SingleScrapeDialogState.FieldUpdateConfirm

        controller.confirmFieldUpdateWrite()
        scope.advanceUntilIdle()
        waitForControllerWrite(controller, scope)

        val result = assertIs<SingleScrapeDialogState.Result>(controller.singleScrapeDialogState)
        assertEquals("New", result.video?.title)
        assertEquals("New plot", result.video?.summary)
        assertTrue(nfo.readText().contains("<plot>New plot</plot>"))
        assertTrue(nfo.readText().contains("<title>New</title>"))
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
    fun `opening a grouped task keeps its part count`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        val task = ScrapeTask(number = "FC2-4694056", fileName = "FC2-4694056.mp4", partCount = 4)
        val files = listOf(
            ScannedFile("one.mp4", "FC2-4694056.mp4", task.number),
            ScannedFile("two.mp4", "FC2-4694056-2.mp4", task.number),
            ScannedFile("three.mp4", "FC2-4694056-3.mp4", task.number),
            ScannedFile("four.mp4", "FC2-PPV 4694056-4.mp4", task.number)
        )

        controller.openSingleScrapeFromTask(task, files)

        assertEquals(4, controller.singleScrapeTask?.partCount)
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

    private fun waitForFieldUpdatePreparation(
        controller: SingleScrapeController,
        scope: TestScope
    ) {
        val deadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < deadline) {
            val state = controller.singleScrapeDialogState
            if (state is SingleScrapeDialogState.FieldUpdateSelection) return
            if (state is SingleScrapeDialogState.Preview && state.error != null) return
            Thread.sleep(20)
            scope.advanceUntilIdle()
        }
    }

    private fun waitForControllerWrite(
        controller: SingleScrapeController,
        scope: TestScope
    ) {
        val deadline = System.currentTimeMillis() + 5000
        while (System.currentTimeMillis() < deadline) {
            if (controller.singleScrapeDialogState is SingleScrapeDialogState.Result) return
            Thread.sleep(20)
            scope.advanceUntilIdle()
        }
    }

    @Test
    fun `opening a grouped file list keeps its part count`() {
        val controller = SingleScrapeController(
            scope = TestScope(),
            orch = { null },
            outputDir = { "output" }
        )
        val files = listOf(
            ScannedFile("H:/fc2/[FC2-4620098]/FC2-4620098-1.mp4", "FC2-4620098-1.mp4", "FC2-4620098"),
            ScannedFile("H:/fc2/[FC2-4620098]/FC2-4620098-2.mp4", "FC2-4620098-2.mp4", "FC2-4620098"),
            ScannedFile("H:/fc2/[FC2-4620098]/FC2-4620098-3.mp4", "FC2-4620098-3.mp4", "FC2-4620098")
        )

        controller.openSingleScrapeGroup(files)

        assertEquals(3, controller.singleScrapeTask?.partCount)
        assertEquals("FC2-4620098-1.mp4", controller.singleScrapeTask?.fileName)
    }

}
