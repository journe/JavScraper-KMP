"""Test connectivity of scraper sites (optionally filtered by site id)."""
import time
from collections.abc import Mapping
from concurrent.futures import ThreadPoolExecutor
from typing import Any

import requests

from scrapers.registry import ScraperRegistry

_TIMEOUT = 10
_USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "Chrome/120.0.0.0 Safari/537.36"
)


def check_sites(
    site_ids: list[str] | None = None,
    site_mirrors: Mapping[str, str] | None = None,
    max_workers: int = 8,
) -> list[dict[str, Any]]:
    """Concurrently probe configured site base URLs, optionally filtered by *site_ids*."""
    infos = ScraperRegistry.list_sites()
    if site_ids:
        infos = [info for info in infos if info["id"] in site_ids]
    with ThreadPoolExecutor(max_workers=max_workers) as pool:
        return list(pool.map(lambda info: _check_site(info, site_mirrors), infos))


def _check_site(
    info: dict[str, Any], site_mirrors: Mapping[str, str] | None = None
) -> dict[str, Any]:
    sid = info["id"]
    scraper = ScraperRegistry.create(sid, (site_mirrors or {}).get(sid))
    if scraper is None:
        return {"id": sid, "ok": False, "error": "scraper not registered"}

    base_url = getattr(scraper, "BASE_URL", "") or ""
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
