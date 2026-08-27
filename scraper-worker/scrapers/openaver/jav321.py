import re
from typing import Optional
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video, Actress
from scrapers.labels import (
    DATE_LABELS, DIRECTOR_LABELS, DURATION_LABELS, MAKER_LABELS, RATING_LABELS,
    SERIES_LABELS, label_matches,
)
from scrapers.registry import ScraperRegistry



class Jav321Scraper(BaseScraper):
    BASE_URL = "https://www.jav321.com"

    @property
    def site_id(self) -> str:
        return "jav321"

    @property
    def site_name(self) -> str:
        return "Jav321"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def _search_one(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            detail_url = f"{self.BASE_URL}/dn/{number}"
            resp = self._session.get(detail_url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")

            title_elem = soup.select_one("h3")
            title = title_elem.get_text(strip=True) if title_elem else ""
            title = re.sub(rf"^{re.escape(number)}\s*", "", title, flags=re.IGNORECASE)

            img_elem = soup.select_one(".col-md-3 img")
            cover_url = img_elem.get("src", "") if img_elem else ""
            if cover_url and not cover_url.startswith("http"):
                cover_url = urljoin(self.BASE_URL, cover_url)

            date = ""
            duration = None
            maker = ""
            director = ""
            tags = []
            actresses = []
            series = ""
            rating = None
            summary = ""

            # Main info panel
            col9 = soup.select_one(".col-md-9")
            if col9:
                for b in col9.find_all("b"):
                    label = b.get_text(strip=True)
                    if label_matches(label, MAKER_LABELS):
                        a_tag = b.find_next("a")
                        if a_tag:
                            maker = a_tag.get_text(strip=True)
                    elif label_matches(label, DATE_LABELS):
                        sibling = b.next_sibling
                        if sibling:
                            m = re.search(r"(\d{4}-\d{2}-\d{2})", str(sibling))
                            if m:
                                date = m.group(1)
                    elif label_matches(label, DURATION_LABELS):
                        sibling = b.next_sibling
                        if sibling:
                            m = re.search(r"(\d+)", str(sibling))
                            if m:
                                duration = int(m.group(1))
                    elif label_matches(label, SERIES_LABELS):
                        a_tag = b.find_next("a")
                        if a_tag:
                            series = a_tag.get_text(strip=True)
                    elif label_matches(label, RATING_LABELS):
                        sibling = b.next_sibling
                        if sibling:
                            m = re.search(r"([0-9.]+)", str(sibling))
                            if m:
                                rating = float(m.group(1))
                    elif label_matches(label, DIRECTOR_LABELS):
                        a_tag = b.find_next("a")
                        if a_tag:
                            director = a_tag.get_text(strip=True)

            # Actress links
            for a in soup.select('a[href*="/star/"]'):
                name = a.get_text(strip=True)
                if name:
                    actresses.append(Actress(name=name))
            actresses = list({a.name: a for a in actresses}.values())

            # Tags
            for a in soup.select('a[href*="/genre/"]'):
                t = a.get_text(strip=True)
                if t:
                    tags.append(t)

            # Summary
            main_panel = soup.select_one(".panel-body")
            if main_panel:
                for cand in main_panel.select(".row .col-md-12"):
                    text = cand.get_text(strip=True)
                    if text:
                        summary = text
                        break

            if not title and not cover_url:
                return None

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
                cover_url=str(cover_url) if cover_url else "",
                source="jav321",
                detail_url=f"{self.BASE_URL}/dn/{number.lower()}",
                summary=summary,
            )

        except (requests.Timeout, requests.ConnectionError):
            return None


ScraperRegistry.register(Jav321Scraper)
