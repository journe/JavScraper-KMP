import re
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class HEYZOScraper(BaseScraper):
    BASE_URL = "https://www.heyzo.com"
    EN_URL = "https://www.heyzo.com/en"

    @property
    def site_id(self) -> str:
        return "heyzo"

    @property
    def site_name(self) -> str:
        return "HEYZO"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "en,ja;q=0.9",
        })

    def _extract_heyzo_num(self, number: str) -> Optional[str]:
        m = re.search(r"HEYZO[-\s]*(\d+)", number, re.IGNORECASE)
        if m:
            return m.group(1)
        return None

    def _search_one(self, number: str) -> Optional[Video]:
        heyzo_num = self._extract_heyzo_num(number)
        if not heyzo_num:
            return None

        try:
            en_url = f"{self.EN_URL}/moviepages/{heyzo_num}/index.html"
            resp = self._session.get(en_url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Title from og:title
            title_elem = soup.select_one('meta[property="og:title"]')
            title = title_elem.get("content", "") if title_elem else ""

            # Cover from og:image
            cover_elem = soup.select_one('meta[property="og:image"]')
            cover_url = cover_elem.get("content", "") if cover_elem else ""

            # Date
            date = ""
            date_elem = soup.select_one('td:contains("Release Date") + td, th:contains("Release date") + td')
            if date_elem:
                date = date_elem.get_text(strip=True)
            else:
                m = re.search(r"(\d{4}/\d{2}/\d{2})", resp.text)
                if m:
                    date = m.group(1).replace("/", "-")

            # Actresses
            actresses = []
            for a in soup.select('a[href*="/actor/"]'):
                name = a.get_text(strip=True)
                if name:
                    actresses.append(Actress(name=name))
            actresses = list({a.name: a for a in actresses}.values())

            # Tags
            tags = []
            for a in soup.select('a[href*="/genre/"]'):
                t = a.get_text(strip=True)
                if t:
                    tags.append(t)
            # Also try category links
            if not tags:
                for a in soup.select('a[href*="/category/"]'):
                    t = a.get_text(strip=True)
                    if t:
                        tags.append(t)

            number = f"HEYZO-{heyzo_num}"

            return Video(
                number=number,
                title=title,
                actresses=actresses,
                date=date,
                tags=tags,
                cover_url=cover_url,
                source="heyzo",
                detail_url=en_url,
            )

        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(HEYZOScraper)
