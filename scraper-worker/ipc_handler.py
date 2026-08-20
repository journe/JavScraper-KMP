import json
import sys
import traceback
from typing import Any, Callable

from scrapers.registry import ScraperRegistry
from core.file_prober import extract_number
from core.smart_search import smart_search, search_multi
from scrapers.models import scrape_success, scrape_error

_handlers: dict[str, Callable] = {}


def register_handler(method: str):
    """Decorator: register a function as a JSON-RPC method handler."""
    def wrapper(fn: Callable) -> Callable:
        _handlers[method] = fn
        return fn
    return wrapper


def handle_request(request: dict) -> dict:
    """Route a JSON-RPC request dict to the registered handler."""
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


# ---------------------------------------------------------------------------
# Built-in handlers
# ---------------------------------------------------------------------------

@register_handler("list_sites")
def _list_sites() -> list[dict]:
    return ScraperRegistry.list_sites()


@register_handler("get_capabilities")
def _get_capabilities() -> dict:
    return {
        "version": "0.1.0",
        "sites": ScraperRegistry.list_sites(),
        "features": ["scrape", "search", "probe_file", "list_sites", "check_sites", "shutdown"],
    }


@register_handler("shutdown")
def _shutdown() -> None:
    import os
    os._exit(0)


@register_handler("probe_file")
def _probe_file(file_path: str) -> dict:
    return {"file_path": file_path, "number": extract_number(file_path)}


@register_handler("scrape")
def _scrape(file_path: str = "", number: str = "", site: str = None) -> dict:
    if not number and file_path:
        number = extract_number(file_path)
    if not number:
        return scrape_error("Cannot extract number", -10)
    result = smart_search(number, site=site)
    if not result:
        return scrape_error(f"No data for {number}", -11)
    return scrape_success(result)


@register_handler("search")
@register_handler("check_sites")
def _check_sites(sites: list[str] = None) -> list[dict]:
    from scrapers.site_check import check_sites

    return check_sites(site_ids=sites)
def _search(number: str, sites: list[str] = None) -> list:
    return [r.to_dict() for r in search_multi(number, sites=sites) if r]


# ---------------------------------------------------------------------------
# Scraper discovery - import all scraper modules to trigger auto-registration
# ---------------------------------------------------------------------------
from scrapers.openaver import javbus  # noqa: F401
from scrapers.openaver import javdb, jav321, fc2, heyzo, avsox, d2pass, dmm, javlibrary, mmtv  # noqa: F401
