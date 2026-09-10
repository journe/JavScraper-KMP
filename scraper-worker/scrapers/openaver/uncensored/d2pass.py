import re
from typing import Optional

import requests

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry

from .d2pass_parsing import (
    SITES,
    SITE_DETAIL_URL,
    extract_gallery,
    parse_caribbeancom_html,
    parse_json,
)


class D2PassScraper(BaseScraper):
    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                ),
                "Accept": "application/json, text/plain, */*",
                "Accept-Language": "ja-JP,ja;q=0.9",
                "Referer": "https://www.1pondo.tv/",
            }
        )

    @property
    def site_id(self) -> str:
        return "d2pass"

    @property
    def site_name(self) -> str:
        return "D2Pass"

    def normalize_number(self, number: str) -> str:
        return number.strip()

    def _detect_site_order(self, number: str) -> list[str]:
        if re.match(r"^\d{6}-\d{2,3}$", number):
            return ["caribbeancom", "1pondo", "10musume"]
        if re.match(r"^\d{6}_\d{3}$", number):
            return ["1pondo", "caribbeancom", "10musume"]
        if re.match(r"^\d{6}_\d{2}$", number):
            return ["10musume", "1pondo", "caribbeancom"]
        return ["1pondo", "caribbeancom", "10musume"]

    def _fetch_json(self, site: str, movie_id: str) -> Optional[dict]:
        try:
            response = self._session.get(SITES[site].format(id=movie_id), timeout=15)
            if response.status_code != 200:
                return None
            return response.json()
        except (requests.RequestException, ValueError, TypeError):
            return None

    def _fetch_gallery_from_html(self, site: str, movie_id: str) -> list[str]:
        if site != "caribbeancom":
            return []
        try:
            response = self._session.get(SITE_DETAIL_URL[site].format(id=movie_id), timeout=15)
            if response.status_code != 200:
                return []
            return extract_gallery(response.text, movie_id)
        except requests.RequestException:
            return []

    def _parse_caribbeancom_html(self, movie_id: str) -> Optional[Video]:
        try:
            response = self._session.get(
                SITE_DETAIL_URL["caribbeancom"].format(id=movie_id),
                timeout=15,
            )
            if response.status_code != 200:
                return None
            return parse_caribbeancom_html(response.text, movie_id)
        except requests.RequestException:
            return None

    def _search_one(self, number: str) -> Optional[Video]:
        movie_id = self.normalize_number(number)
        for site in self._detect_site_order(movie_id):
            data = self._fetch_json(site, movie_id)
            if data is None:
                if site == "caribbeancom":
                    fallback = self._parse_caribbeancom_html(movie_id)
                    if fallback is not None:
                        return fallback
                continue

            video = parse_json(data, site, movie_id)
            if video is None:
                continue
            if not video.sample_images and site == "caribbeancom":
                video.sample_images = self._fetch_gallery_from_html(site, movie_id)
            return video
        return None

    def search_by_keyword(self, keyword: str, limit: int = 20) -> list[Video]:
        result = self._search_one(keyword)
        return [result] if result is not None else []


ScraperRegistry.register(D2PassScraper)