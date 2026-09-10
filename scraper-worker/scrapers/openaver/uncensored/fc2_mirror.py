import json
import re
from typing import Optional
from urllib.parse import urljoin

import requests
from lxml import etree

from scrapers.base import BaseScraper
from scrapers.models import Actress, Video
from scrapers.registry import ScraperRegistry


class FC2MirrorScraper(BaseScraper):
    BASE_URL = "https://javten.com"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update(
            {
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                ),
                "Accept": "text/html,application/xhtml+xml",
                "Accept-Language": "ja,en;q=0.9",
            }
        )

    @property
    def site_id(self) -> str:
        return "fc2mirror"

    @property
    def site_name(self) -> str:
        return "FC2 Mirror"

    def _normalize_fc2_number(self, number: str) -> str:
        normalized = number.upper().strip()
        normalized = re.sub(r"^FC2[-_]?PPV[-_]?", "", normalized)
        normalized = re.sub(r"^FC2[-_]?", "", normalized)
        return normalized.replace("-", "").replace("_", "")

    def _get_title(self, page) -> str:
        values = page.xpath("//h1/text()")
        return values[1].strip() if len(values) > 1 else ""

    def _get_cover(self, page) -> str:
        values = page.xpath('//a[@data-fancybox="gallery"]/@href')
        if not values:
            return ""
        url = values[0]
        return f"https:{url}" if url.startswith("//") else url

    def _get_extrafanart(self, page) -> list[str]:
        values = page.xpath('//div[@style="padding: 0"]/a/@href')
        return [f"https:{url}" if url.startswith("//") else url for url in values]

    def _get_studio(self, page) -> str:
        values = page.xpath('//div[@class="col-8"]/text()')
        return values[0].strip() if values else ""

    def _get_tags(self, page) -> list[str]:
        values = page.xpath('//p[@class="card-text"]/a[contains(@href, "/tag/")]/text()')
        return [tag.strip() for tag in values if tag.strip()]

    def _get_outline(self, page) -> str:
        values = page.xpath('//div[@class="col des"]//text()')
        return "".join(values).replace("\\n", " ").replace("・", "").strip()

    def _get_rating(self, page) -> Optional[float]:
        scripts = page.xpath('//script[@type="application/ld+json"]/text()')
        for script in scripts:
            try:
                data = json.loads(script)
                if isinstance(data, list):
                    nodes = data
                elif isinstance(data, dict):
                    graph = data.get("@graph")
                    nodes = [data] + (graph if isinstance(graph, list) else [])
                else:
                    nodes = []
                for node in nodes:
                    if isinstance(node, dict) and "aggregateRating" in node:
                        return float(node["aggregateRating"]["ratingValue"])
            except (json.JSONDecodeError, KeyError, TypeError, ValueError):
                continue
        return None

    def _search_url(self, fc2_number: str) -> Optional[str]:
        search_url = f"{self.BASE_URL}/search?kw={fc2_number}"
        try:
            response = self._session.get(search_url, timeout=15)
            if response.status_code != 200:
                return None
            page = etree.fromstring(response.content, etree.HTMLParser())
            urls = page.xpath(f"//a[contains(@href, 'id{fc2_number}')]/@href")
            if not urls:
                return None
            non_japanese = ["/tw/", "/ko/", "/en/"]
            selected = next(
                (url for url in urls if all(lang not in url for lang in non_japanese)),
                urls[0],
            )
            return urljoin(self.BASE_URL + "/", selected)
        except (requests.RequestException, etree.LxmlError, ValueError):
            return None

    def _search_one(self, number: str) -> Optional[Video]:
        fc2_number = self._normalize_fc2_number(number)
        if not fc2_number.isdigit():
            return None

        detail_url = self._search_url(fc2_number)
        if not detail_url:
            return None
        try:
            response = self._session.get(detail_url, timeout=15)
            if response.status_code != 200:
                return None
            page = etree.fromstring(response.content, etree.HTMLParser())
            title = self._get_title(page)
            if not title:
                return None

            studio = self._get_studio(page)
            tags = self._get_tags(page)
            sample_images = self._get_extrafanart(page)
            return Video(
                number=f"FC2-{fc2_number}",
                title=title,
                actresses=[Actress(name=studio)] if studio else [],
                date="",
                maker=studio,
                cover_url=self._get_cover(page),
                tags=[tag for tag in tags if tag not in ["無修正", "无修正"]],
                source="fc2mirror",
                detail_url=str(response.url or detail_url),
                sample_images=sample_images,
                summary=self._get_outline(page),
                rating=self._get_rating(page),
            )
        except (requests.RequestException, etree.LxmlError, ValueError, TypeError):
            return None


ScraperRegistry.register(FC2MirrorScraper)
