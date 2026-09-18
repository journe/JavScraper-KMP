package javscraper.ui.components

import javscraper.models.SiteCategory
import kotlin.test.Test
import kotlin.test.assertEquals

class SiteSelectorTest {

    @Test
    fun `groups sites in category order and drops empty categories`() {
        val sites = listOf(
            SiteItem("mixed", "Mixed", SiteCategory.MIXED),
            SiteItem("javbus", "JavBus", SiteCategory.CENSORED),
            SiteItem("fc2", "FC2", SiteCategory.UNCENSORED),
            SiteItem("mdtv", "MDTV", SiteCategory.DOMESTIC),
            SiteItem("javdb", "JavDB", SiteCategory.CENSORED)
        )

        val groups = groupSitesByCategory(sites)

        assertEquals(
            listOf(
                SiteCategory.CENSORED,
                SiteCategory.UNCENSORED,
                SiteCategory.DOMESTIC,
                SiteCategory.MIXED
            ),
            groups.map { it.first }
        )
        assertEquals(listOf("javbus", "javdb"), groups[0].second.map { it.id })
        assertEquals(listOf("fc2"), groups[1].second.map { it.id })
        assertEquals(listOf("mdtv"), groups[2].second.map { it.id })
        assertEquals(listOf("mixed"), groups[3].second.map { it.id })
    }
}
