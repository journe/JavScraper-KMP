from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry

from .javbus_parsing import (
    build_search_url,
    get_lang_prefix,
    parse_detail_page,
    parse_search_ids,
)


class JavBusScraper(BaseScraper):
    BASE_URL = "https://www.javbus.com"

    def __init__(self, lang: str = "zh-tw"):
        self.lang = lang
        self._session = requests.Session()
        self._session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) "
                    "AppleWebKit/605.1.15 (KHTML, like Gecko) "
                    "Version/17.5 Safari/605.1.15"
                ),
                "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                "Accept-Language": "zh-CN,zh-Hans;q=0.9",
                "Accept-Encoding": "gzip, deflate",
                "Connection": "keep-alive",
            }
        )

    @property
    def site_id(self) -> str:
        return "javbus"

    @property
    def site_name(self) -> str:
        return "JavBus"

    def _search_one(self, number: str) -> Optional[Video]:
        normalized = self.normalize_number(number)
        url = f"{self.BASE_URL}{get_lang_prefix(self.lang)}/{normalized}"
        try:
            response = self._session.get(url, timeout=15)
        except (requests.Timeout, requests.ConnectionError):
            return None
        if response.status_code != 200:
            return None
        return self._parse(
            BeautifulSoup(response.text, "html.parser"),
            normalized,
            response.url or url,
        )

    def _parse(self, soup: BeautifulSoup, number: str, url: str) -> Optional[Video]:
        return parse_detail_page(soup, number, url, self.BASE_URL, self.lang)

    def _build_search_url(self, keyword: str, page: int = 1, search_type: int = 0) -> str:
        return build_search_url(self.BASE_URL, keyword, page, search_type)

    def _parse_search_ids(self, soup: BeautifulSoup) -> list[str]:
        return parse_search_ids(soup)

    def get_ids_from_search(self, keyword: str, page: int = 1, search_type: int = 0) -> list[str]:
        url = self._build_search_url(keyword, page, search_type)
        try:
            response = self._session.get(url, timeout=15)
        except (requests.Timeout, requests.ConnectionError):
            return []
        if response.status_code != 200:
            return []
        return self._parse_search_ids(BeautifulSoup(response.text, "html.parser"))

    def _fetch_by_id(self, movie_id: str) -> Optional[Video]:
        url = f"{self.BASE_URL}{get_lang_prefix(self.lang)}/{movie_id}"
        try:
            response = self._session.get(url, timeout=15)
        except (requests.Timeout, requests.ConnectionError):
            return None
        if response.status_code != 200:
            return None
        base_number = movie_id.split("_")[0]
        return self._parse(
            BeautifulSoup(response.text, "html.parser"),
            base_number,
            response.url or url,
        )

    def search_by_keyword(self, keyword: str, limit: int = 20, page: int = 1) -> list[Video]:
        ids = self.get_ids_from_search(keyword, page=page)
        results = []
        for movie_id in ids[:limit]:
            video = self._fetch_by_id(movie_id)
            if video is not None:
                results.append(video)
        return results


ScraperRegistry.register(JavBusScraper)