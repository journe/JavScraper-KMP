"""JavDB scraper using curl_cffi browser impersonation."""

import locale
import logging
import sys
from typing import Optional
from urllib.parse import quote

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry

from .javdb_parsing import find_detail_url, parse_detail_page, parse_search_entries

logger = logging.getLogger(__name__)

try:
    import certifi
    from curl_cffi import CurlOpt
    from curl_cffi import requests as curl_requests

    CURL_CFFI_AVAILABLE = True
    CURL_CFFI_IMPORT_ERROR: Optional[BaseException] = None
except ImportError as error:
    certifi = None
    CurlOpt = None
    curl_requests = None
    CURL_CFFI_AVAILABLE = False
    CURL_CFFI_IMPORT_ERROR = error

_warned = False
_UNSET = object()
_cainfo_override = _UNSET
_ca_warned = False


def _cainfo_override_bytes():
    """Return CAINFO bytes when certifi's Windows path needs ANSI encoding."""
    global _cainfo_override, _ca_warned
    if _cainfo_override is not _UNSET:
        return _cainfo_override

    result = None
    if certifi is not None:
        ca_path = certifi.where()
        if sys.platform == "win32" and not ca_path.isascii():
            try:
                result = ca_path.encode(locale.getencoding(), errors="strict")
            except UnicodeEncodeError as error:
                if not _ca_warned:
                    _ca_warned = True
                    logger.warning(
                        "javdb: CA certificate path is not representable by the "
                        "current code page; TLS may fail: %s",
                        error,
                    )
    _cainfo_override = result
    return result


class JavDBScraper(BaseScraper):
    BASE_URL = "https://javdb.com"
    MIRROR_URLS = ("https://javdb580.com",)

    @property
    def site_id(self) -> str:
        return "javdb"

    @property
    def site_name(self) -> str:
        return "JavDB"

    def __init__(self):
        if CURL_CFFI_AVAILABLE and curl_requests is not None:
            self._session = curl_requests.Session()
        else:
            self._session = requests.Session()
        self._session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36"
                ),
                "Accept": (
                    "text/html,application/xhtml+xml,application/xml;q=0.9,"
                    "*/*;q=0.8"
                ),
                "Accept-Language": "zh-TW,zh;q=0.9,ja;q=0.8,en;q=0.7",
            }
        )

    def _get_html(self, url: str) -> Optional[str]:
        global _warned
        if not CURL_CFFI_AVAILABLE:
            if not _warned:
                _warned = True
                logger.warning(
                    "JavDB is using requests without TLS impersonation: %s",
                    CURL_CFFI_IMPORT_ERROR,
                )

        kwargs = {
            "timeout": 30,
            "headers": {"Referer": f"{self.BASE_URL}/"},
        }
        if CURL_CFFI_AVAILABLE:
            kwargs["impersonate"] = "chrome120"
            cainfo = _cainfo_override_bytes()
            if cainfo is not None and CurlOpt is not None:
                kwargs["curl_options"] = {CurlOpt.CAINFO: cainfo}

        try:
            response = self._session.get(url, **kwargs)
        except Exception as error:
            logger.debug("JavDB request failed for %s: %s", url, error)
            return None

        if response.status_code != 200:
            logger.debug("JavDB non-200 for %s: %s", url, response.status_code)
            return None
        return str(response.text)

    def _fetch_detail(self, number: str, detail_url: str) -> Optional[Video]:
        html = self._get_html(detail_url)
        if not html:
            return None
        return parse_detail_page(
            BeautifulSoup(html, "html.parser"),
            number,
            detail_url,
        )

    def _search_one(self, number: str) -> Optional[Video]:
        normalized = self.normalize_number(number)
        search_url = (
            f"{self.BASE_URL}/search?q={quote(normalized)}&f=all&locale=zh"
        )
        html = self._get_html(search_url)
        if not html:
            return None

        entries = parse_search_entries(
            BeautifulSoup(html, "html.parser"),
            self.BASE_URL,
        )
        detail_url = find_detail_url(entries, normalized)
        if not detail_url:
            return None
        return self._fetch_detail(normalized, detail_url)

    def search_by_keyword(self, keyword: str, limit: int = 20) -> list[Video]:
        query = keyword.strip()
        if not query or limit <= 0:
            return []

        search_url = f"{self.BASE_URL}/search?q={quote(query)}&f=all"
        html = self._get_html(search_url)
        if not html:
            return []

        entries = parse_search_entries(
            BeautifulSoup(html, "html.parser"),
            self.BASE_URL,
        )
        results = []
        for entry in entries[:limit]:
            video = self._fetch_detail(entry.number, entry.detail_url)
            if video is not None:
                results.append(video)
        return results


ScraperRegistry.register(JavDBScraper)
