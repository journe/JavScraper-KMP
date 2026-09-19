package javscraper.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Actress(val name: String)

@Serializable
data class Ranking(
    val rank: Int,
    val listName: String
)

@Serializable
data class Review(
    val id: String = "",
    val author: String = "",
    val date: String = "",
    val score: Double? = null,
    val likes: Int? = null,
    val content: String = ""
)

@Serializable
data class Video(
    val number: String, val title: String = "", val actresses: List<String> = emptyList(),
    val date: String = "", val maker: String = "", val label: String = "", val series: String = "",
    val director: String = "", val duration: Int? = null, val rating: Double? = null,
    val wantCount: Int? = null, val watchedCount: Int? = null, val ratingCount: Int? = null,
    val tags: List<String> = emptyList(), val coverUrl: String = "", val posterUrl: String = "",
    val rankings: List<Ranking> = emptyList(), val reviews: List<Review> = emptyList(),
    val sampleImages: List<String> = emptyList(), val summary: String = "",
    val source: String = "", val detailUrl: String = "", val webpage: String = "",
    val path: String = "",
    val version: String = ""
)

@Serializable
data class ScrapeResult(
    val success: Boolean,
    val data: Video? = null,
    val error: ScrapeError? = null
)

@Serializable
data class ScrapeError(val code: Int = -1, val message: String = "")
data class ScannedFile(
    val path: String,
    val fileName: String,
    val number: String = "",
    val isScraped: Boolean = false,
    val metadata: Video? = null
)

@Serializable
enum class SiteCategory {
    @SerialName("censored")
    CENSORED,

    @SerialName("uncensored")
    UNCENSORED,

    @SerialName("domestic")
    DOMESTIC,

    @SerialName("mixed")
    MIXED,

    @SerialName("unknown")
    UNKNOWN;

    companion object {
        fun fromId(id: String?): SiteCategory = when (id) {
            "censored" -> CENSORED
            "uncensored" -> UNCENSORED
            "domestic" -> DOMESTIC
            "mixed" -> MIXED
            else -> UNKNOWN
        }
    }
}

@Serializable
data class SiteInfo(
    val id: String,
    val name: String,
    val category: SiteCategory = SiteCategory.UNKNOWN,
    @SerialName("base_url") val baseUrl: String = "",
    @SerialName("mirror_urls") val mirrorUrls: List<String> = emptyList()
)

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
        val selectedIndex: Int = 0,
        val error: String? = null
    ) : SingleScrapeDialogState {
        val video: Video
            get() = candidates[selectedIndex]

        fun select(index: Int): Preview {
            if (candidates.isEmpty()) return this
            return copy(selectedIndex = index.coerceIn(0, candidates.lastIndex))
        }
    }

    data class FieldUpdateSelection(
        val candidates: List<Video>,
        val selectedIndex: Int,
        val existing: ExistingVideoMetadata,
        val choices: List<VideoFieldUpdateChoice>
    ) : SingleScrapeDialogState {
        val incoming: Video
            get() = candidates[selectedIndex]
    }

    data class FieldUpdateConfirm(
        val selection: FieldUpdateSelection,
        val merged: Video,
        val selectedFields: Set<VideoUpdateField>
    ) : SingleScrapeDialogState

    data class Result(val video: Video?, val error: String?) : SingleScrapeDialogState
}
