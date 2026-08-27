package javscraper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsControllerSyncTest {

    @Test
    fun `新增站点自动加入启用列表`() {
        val result = mergeSiteLists(
            currentEnabled = listOf("mmtv", "fc2"),
            currentKnown = listOf("mmtv", "fc2", "javbus"),
            availableSiteIds = listOf("mmtv", "fc2", "javbus", "newsite")
        )
        assertEquals(listOf("mmtv", "fc2", "newsite"), result.enabledSites)
        assertEquals(listOf("mmtv", "fc2", "javbus", "newsite"), result.knownSites)
    }

    @Test
    fun `用户主动禁用的站点保持禁用`() {
        val result = mergeSiteLists(
            currentEnabled = listOf("mmtv"),
            currentKnown = listOf("mmtv", "fc2"),
            availableSiteIds = listOf("mmtv", "fc2")
        )
        assertEquals(listOf("mmtv"), result.enabledSites)
        assertEquals(listOf("mmtv", "fc2"), result.knownSites)
    }

    @Test
    fun `已下线站点从启用与已知列表移除`() {
        val result = mergeSiteLists(
            currentEnabled = listOf("mmtv", "fc2", "d2pass"),
            currentKnown = listOf("mmtv", "fc2", "d2pass"),
            availableSiteIds = listOf("mmtv", "fc2")
        )
        assertEquals(listOf("mmtv", "fc2"), result.enabledSites)
        assertEquals(listOf("mmtv", "fc2"), result.knownSites)
        assertTrue("d2pass" !in result.knownSites)
    }

    @Test
    fun `无变化时保持原列表与顺序`() {
        val result = mergeSiteLists(
            currentEnabled = listOf("fc2", "mmtv"),
            currentKnown = listOf("fc2", "mmtv"),
            availableSiteIds = listOf("mmtv", "fc2")
        )
        assertEquals(listOf("fc2", "mmtv"), result.enabledSites)
        assertEquals(listOf("mmtv", "fc2"), result.knownSites)
    }

    @Test
    fun `输入重复项被去重`() {
        val result = mergeSiteLists(
            currentEnabled = listOf("mmtv", "mmtv"),
            currentKnown = listOf("mmtv"),
            availableSiteIds = listOf("mmtv", "fc2", "fc2")
        )
        assertEquals(listOf("mmtv", "fc2"), result.enabledSites)
        assertEquals(listOf("mmtv", "fc2"), result.knownSites)
    }
}
