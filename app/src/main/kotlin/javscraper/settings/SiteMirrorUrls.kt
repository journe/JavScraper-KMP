package javscraper.settings

import java.net.URI

internal fun normalizeSiteMirrorUrl(url: String): String =
    url.trim().trimEnd('/')

internal fun isValidSiteMirrorUrl(url: String): Boolean {
    val normalized = normalizeSiteMirrorUrl(url)
    if (normalized.isEmpty()) return false
    val uri = runCatching { URI(normalized) }.getOrNull() ?: return false
    return (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
}