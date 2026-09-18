import re
from typing import Optional
from urllib.parse import quote

import requests
from lxml import etree

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry

from .common import (
    absolute_url,
    clean_domestic_title,
    clean_names,
    compact_number,
    normalize_domestic_number,
    unique_tags,
)


class MadouquScraper(BaseScraper):
    BASE_URL = "https://madouqu.com"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9",
        })

    @property
    def site_id(self) -> str:
        return "madouqu"

    @property
    def site_name(self) -> str:
        return "Madouqu"

    def _search_one(self, number: str) -> Optional[Video]:
        number = normalize_domestic_number(number)
        try:
            response = self._session.get(
                f"{self.BASE_URL}/?s={quote(number)}", timeout=15
            )
            if response.status_code != 200:
                return None
            search_page = etree.fromstring(response.text, etree.HTMLParser())
            detail_url = self._find_detail_url(search_page, number)
            if not detail_url:
                return None
            response = self._session.get(detail_url, timeout=15)
            if response.status_code != 200:
                return None
            detail_page = etree.fromstring(response.text, etree.HTMLParser())
            return self._parse_detail(detail_page, number, detail_url)
        except (requests.RequestException, etree.LxmlError, ValueError):
            return None

    def _find_detail_url(self, page, number: str) -> str:
        target = compact_number(number)
        for link in page.xpath('//div[contains(@class,"entry-media")]//a[@href]'):
            alt = " ".join(link.xpath(".//img/@alt"))
            if target in compact_number(alt):
                return absolute_url(self.BASE_URL, link.get("href", ""))
        return ""

    def _parse_detail(self, page, fallback_number: str, detail_url: str) -> Optional[Video]:
        title = " ".join(page.xpath('//div[contains(@class,"cao_entry_header")]//h1//text()')).strip()
        paragraphs = [
            re.sub(r"\s+", " ", text).strip()
            for text in page.xpath('//div[contains(@class,"entry-content")]//p//text()')
            if str(text).strip()
        ]
        number = self._field_value(paragraphs, ("番号", "番號")) or fallback_number
        field_title = self._field_value(paragraphs, ("片名",))
        title = field_title or clean_domestic_title(title, number)
        actors = self._actor_value(paragraphs)
        studio = " ".join(page.xpath('//span[contains(@class,"meta-category")]//text()')).strip()
        cover = " ".join(
            page.xpath('//div[contains(@class,"entry-content")]//p//img/@src')
        ).strip()
        dates = page.xpath("//time[@datetime]/@datetime")
        date = dates[0][:10] if dates else ""
        if not title:
            return None
        return Video(
            number=normalize_domestic_number(number),
            title=clean_domestic_title(title, number, studio),
            actresses=clean_names(actors),
            date=date,
            maker=studio,
            label=studio,
            tags=unique_tags([]),
            cover_url=absolute_url(self.BASE_URL, cover),
            source=self.site_id,
            detail_url=detail_url,
        )

    @staticmethod
    def _field_value(paragraphs: list[str], labels: tuple[str, ...]) -> str:
        for text in paragraphs:
            for label in labels:
                match = re.search(rf"{label}\s*[：:]\s*(.+)", text)
                if match:
                    return match.group(1).strip()
        return ""

    @staticmethod
    def _actor_value(paragraphs: list[str]) -> str:
        for index, text in enumerate(paragraphs):
            if text.endswith(("女郎", "女優", "女优")):
                match = re.search(r"[：:]\s*(.+)", text)
                if match:
                    return match.group(1)
                if index + 1 < len(paragraphs):
                    return paragraphs[index + 1].lstrip("：:")
        return ""


ScraperRegistry.register(MadouquScraper)
