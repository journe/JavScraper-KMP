package javscraper.models

import kotlin.test.Test
import kotlin.test.assertEquals

class SingleScrapeDialogStateTest {

    @Test
    fun `preview exposes and updates selected candidate`() {
        val first = Video("ABC-123", title = "First")
        val second = Video("ABC-123", title = "Second")
        val preview = SingleScrapeDialogState.Preview(listOf(first, second), selectedIndex = 1)

        assertEquals(second, preview.video)
        assertEquals(0, preview.select(0).selectedIndex)
        assertEquals(first, preview.select(0).video)
    }
}