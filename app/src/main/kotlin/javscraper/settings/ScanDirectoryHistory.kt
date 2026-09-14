package javscraper.settings

/** Maintains the recently selected scan directories shown by the directory field. */
object ScanDirectoryHistory {
    const val MAX_ENTRIES: Int = 10

    fun remember(history: List<String>, directory: String): List<String> {
        val normalized = directory.trim()
        val deduplicated = if (normalized.isBlank()) {
            history
        } else {
            listOf(normalized) + history.filter { it.trim() != normalized }
        }
        return deduplicated.distinct().take(MAX_ENTRIES)
    }
}