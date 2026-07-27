import re
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class AVSOXScraper(BaseScraper):
    BASE_URL = "https://avsox.click"

    @property
    def site_id(self) -> str:
        return "avsox"

    @property
    def site_name(self) -> str:
        return "AVSox"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def search(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            url = f"{self.BASE_URL}/{number}"
            resp = self._session.get(url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Title
            title_elem = soup.select_one("h3")
            title = title_elem.get_text(strip=True) if title_elem else ""

            # Cover
            cover_elem = soup.select_one('a.bigImage')
            cover_url = cover_elem.get("href", "") if cover_elem else ""
            if not cover_url:
                cover_elem = soup.select_one(".bigImage img")
                cover_url = cover_elem.get("src", "") if cover_elem else ""

            date = ""
            maker = ""
            director = ""
            series = ""
            duration = None
            tags = []
            actresses = []

            # Parse info panels
            info = soup.select_one(".col-md-3.info")
            if info:
                for p in info.find_all("p"):
                    text = p.get_text(strip=True)
                    m = re.search(r"(\d{4}-\d{2}-\d{2})", text)
                    if m:
                        date = m.group(1)
                    m = re.search(r"(\d+)\s*分鐘", text)
                    if m:
                        duration = int(m.group(1))

                    for a in p.find_all("a"):
                        href = a.get("href", "")
                        t = a.get_text(strip=True)
                        if "/genre/" in href:
                            tags.append(t)

            # Header info
            header = soup.select_one("div.header")
            if header:
                for a in header.find_all("a"):
                    href = a.get("href", "")
                    t = a.get_text(strip=True)
                    if "/studio/" in href:
                        maker = t
                    elif "/series/" in href:
                        series = t
                    elif "/director/" in href:
                        director = t

            # Actresses
            waterfall = soup.select_one("#waterfall")
            if waterfall:
                for s in waterfall.select("a.avatar-box"):
                    name = s.get("title", "")
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
                tags=tags,
                cover_url=cover_url,
                source="avsox",
                detail_url=url,
            )

        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(AVSOXScraper)
