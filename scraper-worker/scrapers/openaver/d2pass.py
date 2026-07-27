import re
from typing import Optional

import requests
from bs4 import BeautifulSoup
from urllib.parse import quote

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class D2PassScraper(BaseScraper):
    BASE_URL = "https://www.d2pass.com"

    @property
    def site_id(self) -> str:
        return "d2pass"

    @property
    def site_name(self) -> str:
        return "D2Pass"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def search(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            # Search
            search_url = f"{self.BASE_URL}/search?q={quote(number)}"
            resp = self._session.get(search_url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Find first result link
            detail_link = None
            number_norm = number.upper().replace("-", "")
            for item in soup.select(".search-result a, a[href*='/movie/']"):
                href = item.get("href", "")
                text = item.get_text(strip=True).upper().replace("-", "")
                if number_norm in text and "/movie/" in href:
                    detail_link = href
                    break

            if not detail_link:
                # Fallback: pick first movie link
                first = soup.select_one('a[href*="/movie/"]')
                if not first:
                    return None
                detail_link = first.get("href")

            if detail_link.startswith("/"):
                detail_link = f"{self.BASE_URL}{detail_link}"

            # Detail page
            resp = self._session.get(detail_link, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            title_elem = soup.select_one("h1, h2, h3")
            title = title_elem.get_text(strip=True) if title_elem else ""
            title = re.sub(rf"^{re.escape(number)}\s*", "", title, flags=re.IGNORECASE)

            cover_elem = soup.select_one('meta[property="og:image"], .cover img, img.poster')
            cover_url = ""
            if cover_elem:
                if cover_elem.name == "meta":
                    cover_url = cover_elem.get("content", "")
                else:
                    cover_url = cover_elem.get("src", "")

            date = ""
            maker = ""
            tags = []
            actresses = []

            # Parse detail info rows
            for row in soup.select(".info-row, .detail-row, .meta-row, tr"):
                text = row.get_text(strip=True)
                m = re.search(r"(\d{4}-\d{2}-\d{2})", text)
                if m:
                    date = m.group(1)

                for a in row.find_all("a"):
                    href = a.get("href", "")
                    t = a.get_text(strip=True)
                    if t:
                        if "/tag/" in href:
                            tags.append(t)
                        elif "/actress/" in href or "/star/" in href:
                            actresses.append(Actress(name=t))

            actresses = list({a.name: a for a in actresses}.values())

            return Video(
                number=number,
                title=title,
                actresses=actresses,
                date=date,
                maker=maker,
                tags=tags,
                cover_url=cover_url,
                source="d2pass",
                detail_url=detail_link,
            )

        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(D2PassScraper)
