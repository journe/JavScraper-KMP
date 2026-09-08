import re
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.registry import ScraperRegistry


class JavLibraryScraper(BaseScraper):
    BASE_URL = "https://www.javlibrary.com"
    LANG = "en"

    @property
    def site_id(self) -> str:
        return "javlibrary"

    @property
    def site_name(self) -> str:
        return "JavLibrary"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "en,ja;q=0.9,zh;q=0.8",
        })

    def _search_one(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            search_url = f"{self.BASE_URL}/{self.LANG}/vl_searchbyid.php?keyword={number}"
            resp = self._session.get(search_url, timeout=15, allow_redirects=True)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            # Check if we got redirected to a detail page directly (single hit)
            detail_url = resp.url
            if "/vl_searchbyid.php" in detail_url and "keyword" in detail_url:
                # Multi-result page: find the matching detail link
                detail_link = None
                number_norm = number.upper().replace("-", "")
                for a in soup.select('a[href*="/?v="]'):
                    text = a.get_text(strip=True).upper().replace("-", "")
                    if number_norm in text:
                        detail_link = a.get("href")
                        break
                if not detail_link:
                    # Fallback: first video link
                    first = soup.select_one('a[href*="/?v="]')
                    if not first:
                        return None
                    detail_link = first.get("href")
                if detail_link.startswith("/"):
                    detail_url = f"{self.BASE_URL}{detail_link}"
                else:
                    detail_url = detail_link

                resp = self._session.get(detail_url, timeout=15)
                if resp.status_code != 200:
                    return None
                soup = BeautifulSoup(resp.text, "html.parser")

            return self._parse(soup, number, detail_url)

        except (requests.Timeout, requests.ConnectionError):
            return None

    def _parse(self, soup: BeautifulSoup, number: str, detail_url: str) -> Optional[Video]:
        # Title
        title_elem = soup.select_one("#video_title h3, .post-title")
        title = title_elem.get_text(strip=True) if title_elem else ""
        title = re.sub(rf"^{re.escape(number)}\s*", "", title, flags=re.IGNORECASE)

        # Cover
        cover_elem = soup.select_one("#video_jacket img, .bigImage img")
        cover_url = cover_elem.get("src", "") if cover_elem else ""

        date = ""
        maker = ""
        label = ""
        series = ""
        director = ""
        duration = None
        tags = []
        actresses = []
        rating = None

        # Parse video info table
        info = soup.select_one("#video_info, .video-info")
        if info:
            for tr in info.find_all("tr"):
                th = tr.find("td", class_="header")
                td = tr.find("td", class_="text")
                if not th or not td:
                    continue
                key = th.get_text(strip=True)
                if "Release Date" in key:
                    m = re.search(r"(\d{4}-\d{2}-\d{2})", td.get_text())
                    if m:
                        date = m.group(1)
                elif "Length" in key:
                    m = re.search(r"(\d+)", td.get_text())
                    if m:
                        duration = int(m.group(1))
                elif "Maker" in key:
                    maker = td.get_text(strip=True)
                elif "Label" in key:
                    label = td.get_text(strip=True)
                elif "Series" in key:
                    series = td.get_text(strip=True)
                elif "Director" in key:
                    director = td.get_text(strip=True)
                elif "Rating" in key:
                    m = re.search(r"([0-9.]+)", td.get_text())
                    if m:
                        rating = float(m.group(1))

                # Genres/tags
                for a in td.find_all("a"):
                    href = a.get("href", "")
                    t = a.get_text(strip=True)
                    if t:
                        if "/genre/" in href:
                            tags.append(t)
                        elif "/star/" in href:
                            actresses.append(Actress(name=t))

        # Also look for star/actress links outside table
        if not actresses:
            for a in soup.select('a[href*="/star/"]'):
                name = a.get_text(strip=True)
                if name:
                    actresses.append(Actress(name=name))
            actresses = list({a.name: a for a in actresses}.values())

        if not title and not cover_url:
            return None

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
            rating=rating,
            tags=tags,
            cover_url=cover_url,
            source="javlibrary",
            detail_url=detail_url,
        )


ScraperRegistry.register(JavLibraryScraper)
