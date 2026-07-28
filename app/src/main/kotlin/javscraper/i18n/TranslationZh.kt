package javscraper.i18n

/**
 * Chinese (Simplified) translations.
 */
class TranslationZh : TranslationEn() {

    override val appTitle: String = "JavScraper - JAV 影片刮削器"
    override val statusInitializing: String = "初始化中..."
    override val statusStarting: String = "启动中..."
    override val statusFailed: String = "启动失败"
    override val statusFileNotFound: String = "文件未找到"

    override fun statusReady(sites: Int): String = "就绪（$sites 个站点）"
    override fun statusError(msg: String): String = "错误：$msg"
    override fun statusDirError(msg: String): String = "目录选择错误：$msg"
    override fun statusScanError(msg: String): String = "扫描错误：$msg"
    override fun statusScrapeError(msg: String): String = "刮削错误：$msg"

    override val navScan: String = "扫描"
    override val navScrape: String = "刮削"
    override val navGallery: String = "图库"
    override val navSettings: String = "设置"

    override val scanTitle: String = "文件扫描"
    override val scanDirectoryLabel: String = "扫描目录"
    override val scanButtonScanning: String = "扫描中..."
    override val scanButtonScan: String = "扫描视频"
    override val scanButtonScrape: String = "开始刮削"
    override val scanNoNumber: String = "无番号"
    override val scanEmptyHint: String = "请选择目录后扫描"

    override fun scanFound(count: Int): String = "发现 $count 个含番号的视频"

    override val progressTitle: String = "刮削进度"
    override val progressCancel: String = "取消"
    override val progressStartAll: String = "全部开始"

    override fun progressCompleted(ok: Int, total: Int): String = "$ok / $total 已完成"

    override val galleryTitle: String = "刮削结果"
    override val galleryOpenOutput: String = "打开输出目录"
    override val galleryClear: String = "清空"
    override val galleryEmptyHint: String = "暂无结果"

    override fun galleryCount(n: Int): String = "$n 部影片已刮削"

    override val settingsTitle: String = "设置"
    override val settingsWorkerPath: String = "工作进程路径"
    override val settingsScanDir: String = "扫描目录"
    override val settingsScanRecursive: String = "递归扫描子目录"
    override val settingsOutputDir: String = "输出目录"
    override val settingsMovieFolders: String = "影片文件夹"
    override val settingsHardlinks: String = "硬链接"
    override val settingsDownloadImages: String = "下载图片"
    override val settingsScraperSites: String = "刮削站点"
    override val settingsReset: String = "恢复默认"
    override val settingsLanguage: String = "语言"
    override val settingsLanguageEn: String = "English"
    override val settingsLanguageZh: String = "中文"
    override val settingsLanguageRestartHint: String = "重启后生效"

    override val statusbarTitle: String = "刮削站点"

    override val commonBrowse: String = "浏览"
}
