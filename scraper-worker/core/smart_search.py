import re
from typing import Optional

from scrapers.registry import ScraperRegistry
from scrapers.models import Video

CENSORED = ["javbus", "javdb", "jav321", "javlibrary", "dmm", "mmtv"]
UNCENSORED = ["fc2", "heyzo", "avsox", "d2pass", "mmtv"]


def _is_uncensored(number: str) -> bool:
    """Check whether *number* belongs to an uncensored category.

    Uncensored categories are FC2 / HEYZO releases and date-pattern
    IDs such as ``041417-413``.
    """
    upper = number.strip().upper()
    if upper.startswith("FC2") or upper.startswith("HEYZO"):
        return True
    if re.match(r"^d{6}-d{2,}$", upper):
        return True
    return False


def _priority_chain(number: str) -> list[str]:
    """Return the site priority chain for *number*."""
    return UNCENSORED if _is_uncensored(number) else CENSORED


def smart_search(number: str, site: str | None = None) -> Video | None:
    """Search for *number* on a specific *site* or through the priority chain.

    When *site* is ``None`` the function automatically detects whether the
    number is censored or uncensored and iterates the corresponding list of
    sites until a result is found.
    """
    if site is not None:
        cls = ScraperRegistry.get(site)
        if cls is None:
            return None
        return cls().search(number)

    for sid in _priority_chain(number):
        cls = ScraperRegistry.get(sid)
        if cls is None:
            continue
        result = cls().search(number)
        if result is not None:
            return result
    return None


def search_multi(
    number: str, sites: list[str] | None = None
) -> list[Video | None]:
    """Search *number* across all specified *sites* (or all registered sites).

    Returns a list of results in the same order as the *sites* parameter.
    """
    if sites is None:
        sites = [s["id"] for s in ScraperRegistry.list_sites()]

    results: list[Video | None] = []
    for sid in sites:
        cls = ScraperRegistry.get(sid)
        if cls is None:
            results.append(None)
        else:
            results.append(cls().search(number))
    return results
