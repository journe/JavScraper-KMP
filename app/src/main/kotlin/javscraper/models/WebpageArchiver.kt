package javscraper.models

import kotlinx.serialization.Serializable
import java.nio.file.Path

interface WebpageArchiver {
    suspend fun extractImages(
        mhtmlPath: Path,
        outputDir: Path,
        video: Video,
        /** 为 false 时不写 poster.jpg（保护更新模式下已存在的用户编辑版海报）。 */
        writePoster: Boolean = true
    ): WebpageImageResult
}

@Serializable
data class WebpageImageResult(
    val success: Boolean,
    val message: String = ""
)