package javscraper.scrape

import javscraper.models.Video
import javscraper.models.WebpageArchiver
import javscraper.models.WebpageImageResult
import javscraper.sidecar.SidecarManager
import java.nio.file.Path

class SidecarWebpageArchiver(private val sidecar: SidecarManager) : WebpageArchiver {
    override suspend fun extractImages(
        mhtmlPath: Path,
        outputDir: Path,
        video: Video
    ): WebpageImageResult = sidecar.extractWebpageImages(
        mhtmlPath = mhtmlPath.toString(),
        outputDir = outputDir.toString(),
        coverUrl = video.coverUrl,
        posterUrl = video.posterUrl,
        sampleImages = video.sampleImages
    )
}