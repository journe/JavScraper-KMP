import re
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class JavBusScraper(BaseScraper):
    BASE_URL = "https://www.javbus.com"

    @property
    def site_id(self) -> str:
        return "javbus"

    @property
    def site_name(self) -> str:
        return "JavBus"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def _search_one(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            resp = self._session.get(f"{self.BASE_URL}/{number}", timeout=15)
        except (requests.Timeout, requests.ConnectionError):
            return None
        if resp.status_code != 200:
            return None
        return self._parse(BeautifulSoup(resp.text, "html.parser"), number, resp.url)

    def _parse(self, soup: BeautifulSoup, number: str, url: str) -> Optional[Video]:
        info = soup.find(class_="col-md-3 info")
        if not info:
            return None

        title = (soup.find("h3") or soup).get_text(strip=True)
        cover_tag = soup.find("a", class_="bigImage")
        cover = cover_tag.get("href", "") if cover_tag else ""

        date = ""
        duration = None
        tags: list[str] = []
        maker = label = series = director = ""

        for row in info.find_all("p"):
            t = row.get_text(strip=True)
            m = re.search(r"(\d{4}-\d{2}-\d{2})", t)
            if m:
                date = m.group(1)
            m = re.search(r"(\d+)\s*分钟", t)
            if m:
                duration = int(m.group(1))

            for a in row.find_all("a"):
                href = a.get("href", "")
                if "/genre/" in href:
                    tags.append(a.get_text(strip=True))

        hdr = soup.find("div", class_="header")
        if hdr:
            for a in hdr.find_all("a"):
                h = a.get("href", "")
                t = a.get_text(strip=True)
                if "/studio/" in h:
                    maker = t
                elif "/label/" in h:
                    label = t
                elif "/series/" in h:
                    series = t
                elif "/director/" in h:
                    director = t

        actresses: list[Actress] = []
        waterfall = soup.find(id="waterfall")
        if waterfall:
            for s in waterfall.find_all("a", class_="avatar-box"):
                n = s.get("title", "")
                if n:
                    actresses.append(Actress(name=n))

        return Video(
            number=number,
            title=title,
            actresses=actresses,
            date=date,
            maker=maker,
            label=label,
            series=series,
            director=director,
            duration=duration,
            tags=tags,
            cover_url=cover,
            source="javbus",
            detail_url=url,
        )


ScraperRegistry.register(JavBusScraper)
