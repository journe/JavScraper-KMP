package javscraper.ui

import javscraper.models.Ranking
import javscraper.models.Review
import javscraper.models.Video

/**
 * 测试/预览专用的公共 Video 构造工具。
 * 仅在 @Preview 与 UI 测试中使用,不要在业务代码中引用。
 */
internal fun previewVideoWithAllFields(): Video = Video(
    number = "IPZ-127",
    title = "包含完整字段的刮削结果标题,预览专用",
    actresses = listOf("演员 A", "演员 B", "演员 c", "演员 D"),
    date = "2026-08-26",
    maker = "片商",
    label = "发行标签",
    series = "系列",
    director = "导演",
    duration = 120,
    rating = 9.2,
    wantCount = 7411,
    watchedCount = 1614,
    ratingCount = 1614,
    tags = listOf(
        "标签 A",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 B",
        "标签 d",
        "标签 c",
        "标签asd",
        "标签zzz"
    ),
    rankings = listOf(Ranking(rank = 212, listName = "JavDB 2022年度TOP250")),
    reviews = listOf(
        Review(
            id = "93142913",
            author = "we***e",
            date = "2023-12-11",
            score = 5.0,
            likes = 1007,
            content = "短评内容预览，用于检查折叠列表和长文本换行效果。"
        )
    ),
    coverUrl = "https://example.com/cover.jpg",
    posterUrl = "https://example.com/poster.jpg",
    sampleImages = listOf(
        "https://example.com/sample-1.jpg",
        "https://example.com/sample-2.jpg"
    ),
    summary = "这是完整刮削结果预览的简介内容，用于检查长文本滚动和换行效果。",
    source = "JavBus",
    detailUrl = "https://example.com/detail"
)
