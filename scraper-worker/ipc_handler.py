import sys
import traceback
from typing import Any, Callable

from core.file_prober import extract_number
from core.smart_search import search_candidates, search_multi, smart_search
from core.webpage_archive import extract_images
from scrapers.models import scrape_error, scrape_success
from scrapers.registry import ScraperRegistry

_handlers: dict[str, Callable] = {}


def register_handler(method: str):
    def wrapper(fn: Callable) -> Callable:
        _handlers[method] = fn
        return fn
    return wrapper


def handle_request(request: dict) -> dict:
    req_id = request.get("id")
    method = request.get("method")
    params = request.get("params", {})
    if not method:
        return _error(req_id, -32600, "No method")
    handler = _handlers.get(method)
    if not handler:
        return _error(req_id, -32601, f"Unknown: {method}")
    try:
        result = handler(**params)
        return _success(req_id, result)
    except Exception as e:
        traceback.print_exc(file=sys.stderr)
        return _error(req_id, -1, str(e))


def _success(id: Any, result: Any) -> dict:
    return {"jsonrpc": "2.0", "id": id, "result": result}


def _error(id: Any, code: int, message: str) -> dict:
    return {"jsonrpc": "2.0", "id": id, "error": {"code": code, "message": message}}


@register_handler("list_sites")
def _list_sites() -> list[dict]:
    return ScraperRegistry.list_sites()


@register_handler("get_capabilities")
def _get_capabilities() -> dict:
    return {
        "version": "0.1.0",
        "sites": ScraperRegistry.list_sites(),
        "features": [
            "scrape",
            "search",
            "probe_file",
            "list_sites",
            "check_sites",
            "extract_webpage_images",
            "shutdown",
        ],
    }


@register_handler("shutdown")
def _shutdown() -> None:
    import os
    os._exit(0)


@register_handler("probe_file")
def _probe_file(file_path: str) -> dict:
    return {"file_path": file_path, "number": extract_number(file_path)}


@register_handler("scrape")
def _scrape(
    file_path: str = "",
    number: str = "",
    site: str = None,
    sites: list[str] = None,
    save_webpage: bool = False,
) -> dict:
    if not number and file_path:
        number = extract_number(file_path)
    if not number:
        return scrape_error("Cannot extract number", -10)
    result = smart_search(
        number,
        site=site,
        enabled_sites=sites,
        save_webpage=save_webpage,
    )
    if not result:
        return scrape_error(f"No data for {number}", -11)
    return scrape_success(result)


@register_handler("check_sites")
def _check_sites(sites: list[str] = None) -> list[dict]:
    from scrapers.site_check import check_sites
    return check_sites(site_ids=sites)


@register_handler("search")
def _search(
    number: str,
    sites: list[str] = None,
    site: str = None,
    save_webpage: bool = False,
) -> list[dict]:
    if site is not None:
        candidates = search_candidates(
            number,
            site=site,
            enabled_sites=sites,
            save_webpage=save_webpage,
        )
    elif sites is not None:
        candidates = search_multi(number, sites=sites, save_webpage=save_webpage)
    else:
        candidates = search_candidates(number, save_webpage=save_webpage)
    return [video.to_dict() for video in candidates]


@register_handler("extract_webpage_images")
def _extract_webpage_images(
    mhtml_path: str,
    output_dir: str,
    cover_url: str = "",
    poster_url: str = "",
    sample_images: list[str] | None = None,
) -> dict:
    return extract_images(
        mhtml_path,
        output_dir,
        cover_url=cover_url,
        poster_url=poster_url,
        sample_images=sample_images or [],
    )


from scrapers.openaver.censored import javbus  # noqa: F401
from scrapers.openaver.censored import javdb  # noqa: F401
from scrapers.openaver.censored import jav321  # noqa: F401
from scrapers.openaver.uncensored import fc2  # noqa: F401
from scrapers.openaver.uncensored import heyzo  # noqa: F401
from scrapers.openaver.uncensored import avsox  # noqa: F401
from scrapers.openaver.uncensored import d2pass  # noqa: F401
from scrapers.openaver.censored import dmm  # noqa: F401
from scrapers.openaver.censored import javlibrary  # noqa: F401
from scrapers.openaver.mixed import mmtv  # noqa: F401
