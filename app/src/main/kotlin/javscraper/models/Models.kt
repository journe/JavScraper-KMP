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
data class ScannedFile(val path: String, val fileName: String, val number: String = "")
@Serializable
data class SiteInfo(val id: String, val name: String)