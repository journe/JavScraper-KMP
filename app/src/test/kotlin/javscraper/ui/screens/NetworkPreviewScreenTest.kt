package javscraper.ui.screens

import javscraper.models.Video
import javscraper.models.mergeNetworkPreviewCandidates
import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkPreviewScreenTest {

    @Test
    fun `network candidates accumulate without duplicate detail urls`() {
        val existing = listOf(
            Video(number = "ABC-001", source = "site-a", detailUrl = "https://example.com/a1"),
            Video(number = "ABC-002", source = "site-a", detailUrl = "https://example.com/a2")
        )
        val incoming = listOf(
            Video(number = "ABC-003", source = "site-b", detailUrl = "https://example.com/b1"),
            Video(number = "ABC-001", title = "Updated", source = "site-a", detailUrl = "https://example.com/a1")
        )

        val merged = mergeNetworkPreviewCandidates(existing, incoming)

        assertEquals(
            listOf("ABC-001", "ABC-002", "ABC-003"),
            merged.map { it.number }
        )
        assertEquals("", merged.first { it.number == "ABC-001" }.title)
    }

    @Test
    fun `network candidates without detail url use source number and title`() {
        val existing = listOf(
            Video(number = "ABC-001", title = "First", source = "site-a"),
            Video(number = "ABC-001", title = "Second", source = "site-a")
        )

        val merged = mergeNetworkPreviewCandidates(existing, listOf(
            Video(number = "ABC-001", title = "Second", source = "site-a"),
            Video(number = "ABC-001", title = "Third", source = "site-a")
        ))

        assertEquals(listOf("First", "Second", "Third"), merged.map { it.title })
    }
    @Test
    fun `network preview state exposes candidates and clear action`() {
        val candidate = Video(number = "ABC-001", detailUrl = "https://example.com/1")
        val state = NetworkPreviewState(listOf(candidate))
        var cleared = false
        val actions = NetworkPreviewActions(onClear = { cleared = true })

        assertEquals(listOf(candidate), state.candidates)

        actions.onClear()

        assertEquals(true, cleared)
    }
}
