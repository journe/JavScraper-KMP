package javscraper.models

import java.io.File

/**
 * 水印类型:与 mdcx 内置素材一一对应。
 * [CENSORED] 仅在用户手动选择时添加,推断逻辑默认不勾选。
 */
enum class WatermarkMark(val assetName: String) {
    HD_4K("4k.png"),
    HD_8K("8k.png"),
    SUB("sub.png"),
    CENSORED("youma.png"),
    UMR("umr.png"),
    LEAK("leak.png"),
    UNCENSORED("wuma.png");

    val isHd: Boolean
        get() = this == HD_4K || this == HD_8K
}

/** 水印大小沿用 mdcx 公式:水印高度 = 目标图高 * size / 40。 */
data class WatermarkOptions(
    val marks: List<WatermarkMark> = emptyList(),
    val size: Int = DEFAULT_SIZE
) {
    companion object {
        const val MIN_SIZE = 1
        const val MAX_SIZE = 10
        const val DEFAULT_SIZE = 5

        fun normalizedSize(size: Int): Int = size.coerceIn(MIN_SIZE, MAX_SIZE)
    }
}

/** 从元数据标签与文件名推断默认水印;推断不到则不添加,由用户在对话框手动选择。 */
fun inferWatermarkMarks(video: Video): Set<WatermarkMark> {
    val fileName = video.path.takeIf { it.isNotBlank() }?.let(::File)?.name.orEmpty()
    val hints = (video.tags + fileName).joinToString(" ").lowercase()
    val marks = mutableSetOf<WatermarkMark>()
    if ("4k" in hints) marks += WatermarkMark.HD_4K
    if ("8k" in hints) marks += WatermarkMark.HD_8K
    if ("字幕" in hints) marks += WatermarkMark.SUB
    if ("无码" in hints) marks += WatermarkMark.UNCENSORED
    if ("破解" in hints) marks += WatermarkMark.UMR
    if ("流出" in hints) marks += WatermarkMark.LEAK
    return marks
}