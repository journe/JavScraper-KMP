import re
from typing import Optional

import requests
from bs4 import BeautifulSoup
from urllib.parse import quote

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class JavDBScraper(BaseScraper):
    BASE_URL = "https://javdb.com"

    @property
    def site_id(self) -> str:
        return "javdb"

    @property
    def site_name(self) -> str:
        return "JavDB"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def _search_one(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            search_url = f"{self.BASE_URL}/search?q={quote(number)}&f=all"
            resp = self._session.get(search_url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            detail_path = None
            number_norm = number.upper().replace("-", "")
            for item in soup.select(".movie-list .item"):
                uid_elem = item.select_one(".video-title strong")
                if not uid_elem:
                    continue
                uid = uid_elem.get_text(strip=True).upper().replace("-", "")
                if uid == number_norm:
                    link = item.select_one('a[href^="/v/"]')
                    if link:
                        detail_path = link.get("href")
                        break

            if not detail_path:
                return None

            detail_url = f"{self.BASE_URL}{detail_path}"
            resp = self._session.get(detail_url, timeout=15)
            if resp.status_code != 200:
                return None

            return self._parse(BeautifulSoup(resp.text, "html.parser"), number, detail_url)

        except (requests.Timeout, requests.ConnectionError):
            return None

    def _parse(self, soup: BeautifulSoup, number: str, detail_url: str) -> Optional[Video]:
        title_elem = soup.select_one(".video-detail h2, .title.is-4")
        title = title_elem.get_text(strip=True) if title_elem else ""
        title = re.sub(rf"^{re.escape(number)}\s*", "", title, flags=re.IGNORECASE)

        cover_elem = soup.select_one(".video-cover img, .column-video-cover img")
        cover_url = str(cover_elem.get("src", "")) if cover_elem else ""

        date = ""
        maker = ""
        director = ""
        series = ""
        duration = None
        tags = []
        actresses = []
        rating = None

        for panel in soup.select(".panel-block"):
            label_elem = panel.select_one("strong")
            if not label_elem:
                continue
            label_text = label_elem.get_text(strip=True)
            value_elem = panel.select_one(".value")

            if not value_elem:
                continue

            if "日期" in label_text:
                date = value_elem.get_text(strip=True)
            elif "片商" in label_text or "製作" in label_text:
                maker = value_elem.get_text(strip=True)
            elif "導演" in label_text:
                director = value_elem.get_text(strip=True)
            elif "系列" in label_text:
                series = value_elem.get_text(strip=True)
            elif "評分" in label_text:
                m = re.search(r"([0-9.]+)", value_elem.get_text(strip=True))
                if m:
                    rating = float(m.group(1))

            for a in panel.select('.value a[href*="/tags/"]'):
                t = a.get_text(strip=True)
                if t:
                    tags.append(t)

            for a in panel.select('.value a[href*="/actors/"]'):
                name = a.get_text(strip=True)
                if name:
                    actresses.append(Actress(name=name))

        actresses = list({a.name: a for a in actresses}.values())

        return Video(
            number=number,
            title=title,
            actresses=actresses,
            date=date,
            maker=maker,
            director=director,
            series=series,
            duration=duration,
            rating=rating,
            tags=tags,
            cover_url=cover_url,
            source="javdb",
            detail_url=detail_url,
        )


ScraperRegistry.register(JavDBScraper)
