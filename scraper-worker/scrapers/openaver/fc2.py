import re
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry


class FC2Scraper(BaseScraper):
    BASE_URL = "https://adult.contents.fc2.com"

    @property
    def site_id(self) -> str:
        return "fc2"

    @property
    def site_name(self) -> str:
        return "FC2"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "ja,en;q=0.9",
        })

    def _extract_fc2_id(self, number: str) -> Optional[str]:
        m = re.search(r"(?:FC2[-\s]*PPV[-\s]*)?(\d+)", number, re.IGNORECASE)
        if m:
            return m.group(1)
        return None

    def search(self, number: str) -> Optional[Video]:
        fc2_id = self._extract_fc2_id(number)
        if not fc2_id:
            return None

        try:
            url = f"{self.BASE_URL}/article/{fc2_id}/"
            resp = self._session.get(url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            title_elem = soup.select_one("h2")
            title = title_elem.get_text(strip=True) if title_elem else ""

            cover_elem = soup.select_one('meta[property="og:image"]')
            cover_url = cover_elem.get("content", "") if cover_elem else ""

            date = ""
            tags = []

            date_elem = soup.select_one('span[class*="date"]')
            if date_elem:
                m = re.search(r"(\d{4}/\d{2}/\d{2})", date_elem.get_text())
                if m:
                    date = m.group(1).replace("/", "-")

            for a in soup.select('a[href*="/tag/"]'):
                t = a.get_text(strip=True)
                if t:
                    tags.append(t)

            number = f"FC2-PPV-{fc2_id}"

            return Video(
                number=number,
                title=title,
                date=date,
                tags=tags,
                cover_url=cover_url,
                source="fc2",
                detail_url=url,
            )

        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(FC2Scraper)
