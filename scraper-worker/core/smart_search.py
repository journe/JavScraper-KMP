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


def _is_site_enabled(site: str, enabled_sites: Iterable[str] | None) -> bool:
    return enabled_sites is None or site in set(enabled_sites)


def search_candidates(
    number: str,
    site: str | None = None,
    enabled_sites: Iterable[str] | None = None,
    first_only: bool = False,
) -> list[Video]:
    """Search *number* on one enabled site or through the priority chain."""
    site_ids = [site] if site is not None else _enabled_chain(number, enabled_sites)
    results: list[Video] = []
    for site_id in site_ids:
        if not _is_site_enabled(site_id, enabled_sites):
            continue
        cls = ScraperRegistry.get(site_id)
        if cls is not None:
            site_results = cls().search(number)
            results.extend(site_results)
            if first_only and results:
                return results
    return results


def smart_search(
    number: str,
    site: str | None = None,
    enabled_sites: Iterable[str] | None = None,
) -> Optional[Video]:
    """Search *number* and return only the first available video."""
    return next(
        iter(search_candidates(number, site=site, enabled_sites=enabled_sites, first_only=True)),
        None,
    )


def search_multi(
    number: str,
    sites: list[str] | None = None,
) -> list[Video]:
    """Search *number* across the explicitly specified sites and flatten candidates."""
    site_ids = sites
    if site_ids is None:
        site_ids = [item["id"] for item in ScraperRegistry.list_sites()]

    results: list[Video] = []
    for site_id in site_ids:
        cls = ScraperRegistry.get(site_id)
        if cls is not None:
            results.extend(cls().search(number))
    return results
