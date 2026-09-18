import re
from typing import Optional

import requests

from scrapers.base import BaseScraper
from scrapers.models import Actress, Video
from scrapers.registry import ScraperRegistry

from .common import compact_number, normalize_domestic_number, unique_tags


class HdoubanScraper(BaseScraper):
    BASE_URL = "https://ormtgu.com"
    API_BASE_URL = "https://api.6dccbca.com"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept": "application/json,text/plain,*/*",
            "Accept-Language": "zh-CN,zh;q=0.9",
        })

    @property
    def site_id(self) -> str:
        return "hdouban"

    @property
    def site_name(self) -> str:
        return "Hdouban"

    def _search_one(self, number: str) -> Optional[Video]:
        number = normalize_domestic_number(number)
        try:
            response = self._session.get(
                f"{self.API_BASE_URL}/api/search",
                params={"ty": "movie", "search": number, "page": 1, "pageSize": 12},
                timeout=15,
            )
            if response.status_code != 200:
                return None
            candidates = response.json().get("data", {}).get("list", [])
            match = self._match_candidate(candidates, number)
            if not match:
                return None
            detail_id = str(match.get("id", ""))
            if not detail_id:
                return None
            response = self._session.post(
                f"{self.API_BASE_URL}/api/movie/detail",
                data={"id": detail_id},
                timeout=15,
            )
            if response.status_code != 200:
                return None
            data = response.json().get("data", {})
            return self._parse_detail(data, detail_id)
        except (requests.RequestException, ValueError, TypeError, AttributeError):
            return None

    @staticmethod
    def _match_candidate(candidates, number: str) -> Optional[dict]:
        target = compact_number(number)
        for item in candidates:
            candidate_number = compact_number(str(item.get("number", "")))
            candidate_name = compact_number(str(item.get("name", "")))
            if target == candidate_number or target in candidate_name:
                return item
        return None

    def _parse_detail(self, data: dict, detail_id: str) -> Optional[Video]:
        number = str(data.get("number", "")).strip().upper()
        title = str(data.get("name", "")).strip()
        if not number or not title:
            return None
        title = title.replace(number, "").strip()
        if not title:
            return None
        actresses = [
            Actress(name=str(item.get("name", "")).replace("♀", "").strip())
            for item in data.get("actors", [])
            if "♀" in str(item.get("sex", "")) and item.get("name")
        ]
        labels = [item.get("name", "") for item in data.get("labels", [])]
        duration = None
        try:
            duration = int(data.get("time")) // 60 if data.get("time") else None
        except (TypeError, ValueError):
            pass
        rating = None
        try:
            rating = float(data.get("score")) if data.get("score") else None
        except (TypeError, ValueError):
            pass
        director = self._first_name(data.get("director"))
        studio = self._first_name(data.get("company"))
        series = self._first_name(data.get("series"))
        release = str(data.get("release_time", "")).replace(" 00:00:00", "").strip()
        return Video(
            number=number,
            title=title,
            actresses=actresses,
            date=release,
            maker=studio,
            label=studio,
            series=series,
            director=director,
            duration=duration,
            rating=rating,
            tags=unique_tags(labels),
            cover_url=str(data.get("big_cove", "")),
            poster_url=str(data.get("small_cover", "")),
            sample_images=[item.get("big_img", "") for item in data.get("map", []) if item.get("big_img")],
            source=self.site_id,
            detail_url=f"{self.BASE_URL}/moviedetail/{detail_id}",
        )

    @staticmethod
    def _first_name(items) -> str:
        return str(items[0].get("name", "")).strip() if items else ""


ScraperRegistry.register(HdoubanScraper)
