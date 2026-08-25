import re
from collections.abc import Iterable
from typing import Optional

from scrapers.registry import ScraperRegistry
from scrapers.models import Video

CENSORED = ["javbus", "javdb", "jav321", "javlibrary", "dmm", "mmtv"]
UNCENSORED = ["fc2", "heyzo", "avsox", "d2pass", "mmtv"]


def _is_uncensored(number: str) -> bool:
    """Check whether *number* belongs to an uncensored category."""
    upper = number.strip().upper()
    if upper.startswith("FC2") or upper.startswith("HEYZO"):
        return True
    return re.match(r"^\d{6}-\d{2,}$", upper) is not None


def _priority_chain(number: str) -> list[str]:
    """Return the site priority chain for *number*."""
    return UNCENSORED if _is_uncensored(number) else CENSORED


def _enabled_chain(number: str, enabled_sites: Iterable[str] | None) -> list[str]:
    chain = _priority_chain(number)
    if enabled_sites is None:
        return chain
    enabled = set(enabled_sites)
    return [site_id for site_id in chain if site_id in enabled]


def smart_search(
    number: str,
    site: str | None = None,
    enabled_sites: Iterable[str] | None = None,
) -> Optional[Video]:
    """Search *number* on a specific enabled site or through the priority chain."""
    if site is not None:
        cls = ScraperRegistry.get(site)
        if cls is None or (enabled_sites is not None and site not in set(enabled_sites)):
            return None
        return cls().search(number)

    for sid in _enabled_chain(number, enabled_sites):
        cls = ScraperRegistry.get(sid)
        if cls is None:
            continue
        result = cls().search(number)
        if result is not None:
            return result
    return None


def search_multi(
    number: str, sites: list[str] | None = None
) -> list[Optional[Video]]:
    """Search *number* across all specified sites (or all registered sites)."""
    if sites is None:
        sites = [s["id"] for s in ScraperRegistry.list_sites()]

    results: list[Optional[Video]] = []
    for sid in sites:
        cls = ScraperRegistry.get(sid)
        results.append(cls().search(number) if cls is not None else None)
    return results
