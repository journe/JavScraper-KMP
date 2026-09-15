package javscraper.ui.screens.settings

import javscraper.models.SiteCategory
import javscraper.models.SiteInfo
import kotlin.test.Test
import kotlin.test.assertEquals

class SiteMirrorSettingsTest {

    @Test
    fun `shows only sites with base urls and applies configured mirrors`() {
        val sites = listOf(
            SiteInfo("javbus", "JavBus", SiteCategory.CENSORED, "https://www.javbus.com"),
            SiteInfo("legacy", "Legacy", SiteCategory.UNKNOWN)
        )
        val configured = mapOf("javbus" to "https://www.dmmsee.casa")

        val items = siteMirrorDisplayItems(sites, configured)

        assertEquals(listOf("javbus"), items.map { it.id })
        assertEquals("https://www.dmmsee.casa", items.single().effectiveUrl)
    }

    @Test
    fun `mirror settings are available when a site exposes a base url`() {
        val withoutBaseUrl = listOf(SiteInfo("legacy", "Legacy", SiteCategory.UNKNOWN))
        val withBaseUrl = listOf(
            SiteInfo("javbus", "JavBus", SiteCategory.CENSORED, "https://www.javbus.com")
        )

        assertEquals(false, mirrorSettingsAvailable(withoutBaseUrl))
        assertEquals(true, mirrorSettingsAvailable(withBaseUrl))
    }

    @Test
    fun `builds default built-in and custom choices without duplicates`() {
        val site = SiteInfo(
            id = "javbus",
            name = "JavBus",
            category = SiteCategory.CENSORED,
            baseUrl = "https://www.javbus.com",
            mirrorUrls = listOf("https://www.dmmsee.casa", "https://www.buscdn.casa")
        )

        val choices = siteMirrorChoices(site, "https://mirror.example")

        assertEquals(
            listOf(
                "https://www.javbus.com",
                "https://www.dmmsee.casa",
                "https://www.buscdn.casa",
                "https://mirror.example"
            ),
            choices.map { it.value }
        )
    }
}
