import re
from typing import Optional
from urllib.parse import urljoin

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
        cover = urljoin(url, cover) if cover else ""

        date = ""
        duration = None
        actresses: list[Actress] = []
        tags: list[str] = []
        maker = label = series = director = ""

        for row in info.find_all("p"):
            text = row.get_text(strip=True)
            date_match = re.search(r"(\d{4}-\d{2}-\d{2})", text)
            if date_match:
                date = date_match.group(1)
            duration_match = re.search(r"(\d+)\s*(?:分钟|分鐘|min)", text)
            if duration_match:
                duration = int(duration_match.group(1))

            for link in row.find_all("a"):
                href = link.get("href", "")
                value = link.get_text(strip=True)
                if "/genre/" in href:
                    tags.append(value)
                elif "/star/" in href:
                    actresses = [a for a in actresses if a.name != value]
                    actresses.append(Actress(name=value))
                elif "/studio/" in href:
                    maker = value
                elif "/label/" in href:
                    label = value
                elif "/series/" in href:
                    series = value
                elif "/director/" in href:
                    director = value

        sample_images: list[str] = []
        sample_waterfall = soup.find(id="sample-waterfall")
        if sample_waterfall:
            for box in sample_waterfall.find_all("a", class_="sample-box"):
                image = box.find("img")
                source = str(image.get("src", "")) if image else ""
                if source:
                    sample_url = urljoin(url, source)
                    if sample_url not in sample_images:
                        sample_images.append(sample_url)


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
            sample_images=sample_images,
            source="javbus",
            detail_url=url,
        )


ScraperRegistry.register(JavBusScraper)
