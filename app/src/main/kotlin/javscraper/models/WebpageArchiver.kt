package javscraper.models

import kotlinx.serialization.Serializable
import java.nio.file.Path

interface WebpageArchiver {
    suspend fun extractImages(
        mhtmlPath: Path,
        outputDir: Path,
        video: Video
    ): WebpageImageResult
}

@Serializable
data class WebpageImageResult(
    val success: Boolean,
    val message: String = ""
)