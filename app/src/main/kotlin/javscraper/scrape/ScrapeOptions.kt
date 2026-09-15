package javscraper.scrape

import javscraper.settings.AppSettings

/**
 * 刮削与写盘的输出选项，聚合从 [AppSettings] 投影而来的全部配置项。
 *
 * 引入本类是为了让 [ScrapeOrchestrator] 只依赖「协作者 + 选项」两类参数，
 * 避免构造参数随设置项增长而膨胀；默认值统一由 [AppSettings] 提供，避免两处维护。
 */
data class ScrapeOptions(
    val outputDir: String,
    val createMovieFolders: Boolean,
    val moveInsteadOfCopy: Boolean,
    val downloadImages: Boolean,
    val downloadPreviewImages: Boolean,
    val downloadWebPages: Boolean,
    val lockData: Boolean,
    val folderLayers: List<String>,
    val filenameFormat: String,
    val maxTitleLength: Int,
    val maxFilenameLength: Int,
    val suffixKeywords: List<String>,
    val siteMirrorUrls: Map<String, String>,
    val enabledSites: Set<String>?
) {
    companion object {
        /** 将用户设置投影为刮削选项，是本仓库中这些配置的默认值来源。 */
        fun from(settings: AppSettings): ScrapeOptions = ScrapeOptions(
            outputDir = settings.outputDir,
            createMovieFolders = settings.createMovieFolders,
            moveInsteadOfCopy = settings.moveInsteadOfCopy,
            downloadImages = settings.downloadImages,
            downloadPreviewImages = settings.downloadPreviewImages,
            downloadWebPages = settings.downloadWebPages,
            lockData = settings.lockData,
            folderLayers = settings.folderLayers,
            filenameFormat = settings.filenameFormat,
            maxTitleLength = settings.maxTitleLength,
            maxFilenameLength = settings.maxFilenameLength,
            suffixKeywords = settings.suffixKeywords,
            siteMirrorUrls = settings.siteMirrorUrls,
            enabledSites = settings.enabledSites.toSet()
        )
    }
}