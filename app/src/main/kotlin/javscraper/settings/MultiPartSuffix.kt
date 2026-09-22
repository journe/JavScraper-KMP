package javscraper.settings

import kotlinx.serialization.Serializable

/** Jellyfin-supported labels used when this app writes multi-part files. */
@Serializable
enum class MultiPartSuffix(val value: String, val displayName: String) {
    CD("cd", "CD"),
    DVD("dvd", "DVD"),
    PART("part", "Part"),
    PT("pt", "PT"),
    DISC("disc", "Disc"),
    DISK("disk", "Disk")
}