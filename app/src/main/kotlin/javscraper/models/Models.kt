package javscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class Actress(val name: String)

@Serializable
data class Video(
    val number: String, val title: String = "", val actresses: List<String> = emptyList(),
    val date: String = "", val maker: String = "", val label: String = "", val series: String = "",
    val director: String = "", val duration: Int? = null, val rating: Double? = null,
    val tags: List<String> = emptyList(), val coverUrl: String = "", val posterUrl: String = "",
    val sampleImages: List<String> = emptyList(), val summary: String = "",
    val source: String = "", val detailUrl: String = ""
)

@Serializable
data class ScrapeResult(val success: Boolean, val data: Video? = null, val error: ScrapeError? = null)

@Serializable
data class ScrapeError(val code: Int = -1, val message: String = "")
data class ScannedFile(
    val path: String,
    val fileName: String,
    val number: String = "",
    val isScraped: Boolean = false
)

@Serializable
data class SiteInfo(val id: String, val name: String)

@Serializable
data class SiteCheckResult(
    val id: String,
    val ok: Boolean,
    val status: Int? = null,
    val latencyMs: Int? = null,
    val error: String? = null,
    val url: String? = null
)

sealed interface SingleScrapeDialogState {
    data object Closed : SingleScrapeDialogState
    data object Input : SingleScrapeDialogState
    data object Scraping : SingleScrapeDialogState
    data class Preview(
        val candidates: List<Video>,
        val selectedIndex: Int = 0
    ) : SingleScrapeDialogState {
        val video: Video
            get() = candidates[selectedIndex]

        fun select(index: Int): Preview {
            if (candidates.isEmpty()) return this
            return copy(selectedIndex = index.coerceIn(0, candidates.lastIndex))
        }
    }
    data class Result(val video: Video?, val error: String?) : SingleScrapeDialogState
}