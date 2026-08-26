package javscraper.models

private fun networkPreviewCandidateKey(video: Video): String {
    if (video.detailUrl.isNotBlank()) return video.detailUrl
    return listOf(video.source, video.number, video.title).joinToString("\n")
}

fun mergeNetworkPreviewCandidates(
    existing: List<Video>,
    incoming: List<Video>
): List<Video> {
    val seen = existing.mapTo(mutableSetOf()) { networkPreviewCandidateKey(it) }
    return existing + incoming.filter { seen.add(networkPreviewCandidateKey(it)) }
}