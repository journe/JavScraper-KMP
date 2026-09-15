import re
from collections.abc import Iterable, Mapping
from typing import Optional

from scrapers.registry import ScraperRegistry
from scrapers.models import Video

CENSORED = ["javbus", "javdb", "jav321", "javlibrary", "dmm", "mmtv"]
UNCENSORED = ["fc2", "fc2mirror", "heyzo", "avsox", "d2pass", "mmtv"]


def _is_uncensored(number: str) -> bool:
    upper = number.strip().upper()
    if upper.startswith("FC2") or upper.startswith("HEYZO"):
        return True
    return re.match(r"^\d{6}-\d{2,}$", upper) is not None


def _priority_chain(number: str) -> list[str]:
    return UNCENSORED if _is_uncensored(number) else CENSORED


def _enabled_chain(number: str, enabled_sites: Iterable[str] | None) -> list[str]:
    chain = _priority_chain(number)
    if enabled_sites is None:
        return chain
    enabled = set(enabled_sites)
    return [site_id for site_id in chain if site_id in enabled]


def _is_site_enabled(site: str, enabled_sites: Iterable[str] | None) -> bool:
    return enabled_sites is None or site in set(enabled_sites)


def _search_class(
    site_id: str,
    number: str,
    save_webpage: bool,
    site_mirrors: Mapping[str, str] | None = None,
) -> list[Video]:
    searcher = ScraperRegistry.create(site_id, (site_mirrors or {}).get(site_id))
    if searcher is None:
        return []
    if save_webpage:
        return searcher.search(number, save_webpage=True)
    return searcher.search(number)


def search_candidates(
    number: str,
    site: str | None = None,
    enabled_sites: Iterable[str] | None = None,
    site_mirrors: Mapping[str, str] | None = None,
    first_only: bool = False,
    save_webpage: bool = False,
) -> list[Video]:
    site_ids = [site] if site is not None else _enabled_chain(number, enabled_sites)
    results: list[Video] = []
    for site_id in site_ids:
        if not _is_site_enabled(site_id, enabled_sites):
            continue
        site_results = _search_class(site_id, number, save_webpage, site_mirrors)
        results.extend(site_results)
        if first_only and results:
            return results
    return results


def smart_search(
    number: str,
    site: str | None = None,
    enabled_sites: Iterable[str] | None = None,
    site_mirrors: Mapping[str, str] | None = None,
    save_webpage: bool = False,
) -> Optional[Video]:
    return next(
        iter(search_candidates(
            number,
            site=site,
            enabled_sites=enabled_sites,
            site_mirrors=site_mirrors,
            first_only=True,
            save_webpage=save_webpage,
        )),
        None,
    )


def search_multi(
    number: str,
    sites: list[str] | None = None,
    site_mirrors: Mapping[str, str] | None = None,
    save_webpage: bool = False,
) -> list[Video]:
    site_ids = sites
    if site_ids is None:
        site_ids = [item["id"] for item in ScraperRegistry.list_sites()]
    results: list[Video] = []
    for site_id in site_ids:
        results.extend(_search_class(site_id, number, save_webpage, site_mirrors))
    return results
