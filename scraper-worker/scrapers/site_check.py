"""Test connectivity of all registered scraper sites."""
import time
from concurrent.futures import ThreadPoolExecutor
from typing import Any

import requests

from scrapers.registry import ScraperRegistry

_TIMEOUT = 10
_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "Chrome/120.0.0.0 Safari/537.36"
)


def check_sites(max_workers: int = 8) -> list[dict[str, Any]]:
    """Concurrently probe each registered site's base URL."""
    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        return list(pool.map(_check_site, ScraperRegistry.list_sites()))


def _check_site(info: dict[str, Any]) -> dict[str, Any]:
    sid = info["id"]
    scraper_cls = ScraperRegistry.get(sid)
    if scraper_cls is None:
        return {"id": sid, "ok": False, "error": "scraper not registered"}

    base_url = getattr(scraper_cls(), "BASE_URL", "") or ""
    if not base_url:
        return {"id": sid, "ok": False, "error": "no base url"}

    session = requests.Session()
    session.headers.update({"User-Agent": _USER_AGENT})

    start = time.monotonic()
    try:
        resp = session.get(base_url, timeout=_TIMEOUT)
        elapsed_ms = int((time.monotonic() - start) * 1000)
        ok = resp.status_code < 400
        return {
            "id": sid,
            "ok": ok,
            "status": resp.status_code,
            "latency_ms": elapsed_ms,
            "url": resp.url or base_url,
            "error": None if ok else f"HTTP {resp.status_code}",
        }
    except requests.Timeout:
        return {
            "id": sid,
            "ok": False,
            "error": "timeout",
            "latency_ms": int((time.monotonic() - start) * 1000),
        }
    except requests.RequestException as e:
        return {
            "id": sid,
            "ok": False,
            "error": str(e),
            "latency_ms": int((time.monotonic() - start) * 1000),
        }