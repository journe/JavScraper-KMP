package javscraper.io.metadata

internal object NfoTagMerger {
    fun merge(existing: List<String>, incoming: List<String>): List<String> =
        (existing + incoming)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
}
