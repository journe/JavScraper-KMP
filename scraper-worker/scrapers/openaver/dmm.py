import re
from typing import Optional
from urllib.parse import quote

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class DMMScraper(BaseScraper):
    BASE_URL = "https://www.dmm.co.jp"

    @property
    def site_id(self) -> str:
        return "dmm"

    @property
    def site_name(self) -> str:
        return "DMM"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "ja,en;q=0.9",
        })

    def _search_one(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            search_url = f"{self.BASE_URL}/mono/dvd/-/search/?searchstr={quote(number)}&sort=date"
            resp = self._session.get(search_url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Find first result link
            detail_link = None
            number_norm = number.upper().replace("-", "")
            for a in soup.select('a[href*="/mono/dvd/"]'):
                href = a.get("href", "")
                text = a.get_text(strip=True).upper().replace("-", "")
                if number_norm in text and "/mono/dvd/-/detail/" in href:
                    detail_link = href
                    break

            if not detail_link:
                # Fallback: first detail link
                first = soup.select_one('a[href*="/mono/dvd/-/detail/"]')
                if not first:
                    return None
                detail_link = first.get("href")

            if detail_link.startswith("/"):
                detail_link = f"{self.BASE_URL}{detail_link}"

            resp = self._session.get(detail_link, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Title
            title_elem = soup.select_one("#title, h1")
            title = title_elem.get_text(strip=True) if title_elem else ""
            title = re.sub(rf"^{re.escape(number)}\s*", "", title, flags=re.IGNORECASE)

            # Cover
            cover_elem = soup.select_one('a[href*="pics.dmm.co.jp"] img, .product-picture img')
            cover_url = cover_elem.get("src", "") if cover_elem else ""
            if not cover_url:
                cover_elem = soup.select_one('meta[property="og:image"]')
                cover_url = cover_elem.get("content", "") if cover_elem else ""

            date = ""
            maker = ""
            label = ""
            series = ""
            director = ""
            duration = None
            tags = []
            actresses = []

            # Parse table data
            table = soup.select_one(".product-info table, table.mg-b20")
            if table:
                for tr in table.find_all("tr"):
                    th = tr.find("th")
                    td = tr.find("td")
                    if not th or not td:
                        continue
                    key = th.get_text(strip=True)
                    if "配信開始日" in key or "発売日" in key:
                        m = re.search(r"(\d{4}/\d{2}/\d{2})", td.get_text())
                        if m:
                            date = m.group(1).replace("/", "-")
                    elif "収録時間" in key:
                        m = re.search(r"(\d+)", td.get_text())
                        if m:
                            duration = int(m.group(1))
                    elif "メーカー" in key:
                        maker = td.get_text(strip=True)
                    elif "レーベル" in key:
                        label = td.get_text(strip=True)
                    elif "シリーズ" in key:
                        series = td.get_text(strip=True)
                    elif "監督" in key:
                        director = td.get_text(strip=True)

                    for a in td.find_all("a"):
                        href = a.get("href", "")
                        t = a.get_text(strip=True)
                        if t:
                            if "/genre/" in href:
                                tags.append(t)
                            elif "/actress/" in href:
                                actresses.append(Actress(name=t))

            actresses = list({a.name: a for a in actresses}.values())

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
                cover_url=cover_url,
                source="dmm",
                detail_url=detail_link,
            )

        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(DMMScraper)
