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


class CnmdbScraper(BaseScraper):
    BASE_URL = "https://cnmdb.net"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9",
        })

    @property
    def site_id(self) -> str:
        return "cnmdb"

    @property
    def site_name(self) -> str:
        return "CNMDB"

    def _search_one(self, number: str) -> Optional[Video]:
        number = normalize_domestic_number(number)
        try:
            direct_url = f"{self.BASE_URL}/{quote(number)}"
            response = self._session.get(direct_url, timeout=15)
            if response.status_code == 200:
                video = self._parse_detail(
                    etree.fromstring(response.text, etree.HTMLParser()),
                    direct_url,
                )
                if video is not None:
                    return video

            search_url = f"{self.BASE_URL}/s0?q={quote(number)}"
            response = self._session.get(search_url, timeout=15)
            if response.status_code != 200:
                return None
            return self._parse_search(
                etree.fromstring(response.text, etree.HTMLParser()), number
            )
        except (requests.RequestException, etree.LxmlError, ValueError):
            return None

    def _parse_search(self, page, number: str) -> Optional[Video]:
        target = compact_number(number)
        for item in page.xpath('//div[contains(@class,"post-item")]'):
            title = " ".join(item.xpath(".//h3/a//text()")).strip()
            href = " ".join(item.xpath(".//h3/a/@href")).strip()
            if target in compact_number(title) and href:
                return Video(
                    number=number,
                    title=clean_domestic_title(title, number),
                    tags=unique_tags([]),
                    cover_url=absolute_url(
                        self.BASE_URL,
                        " ".join(item.xpath('.//div[contains(@class,"post-item-image")]//img/@src')),
                    ),
                    source=self.site_id,
                    detail_url=absolute_url(self.BASE_URL, href),
                )
        return None

    def _parse_detail(self, page, detail_url: str) -> Optional[Video]:
        crumbs = [
            text.strip()
            for text in page.xpath('//ol[contains(@class,"breadcrumb")]//text()')
            if text.strip()
        ]
        if not crumbs:
            return None
        combined = crumbs[-1]
        number_match = re.search(r"([A-Z0-9]+(?:-[A-Z]+)*-\d{2,}(?:-\d+)?)", combined.upper())
        number = normalize_domestic_number(number_match.group(1)) if number_match else ""
        if not number:
            return None
        remainder = re.sub(re.escape(number_match.group(1)), "", combined, count=1, flags=re.IGNORECASE)
        parts = [part.strip() for part in re.split(r"[.,，]", remainder) if part.strip()]
        title = parts[0] if parts else ""
        actors = parts[1:] if len(parts) > 1 else []
        series = next((crumb for crumb in crumbs[:-1] if "系列" in crumb), "")
        studio = next((crumb for crumb in crumbs[1:-1] if "麻豆" in crumb or "传媒" in crumb), "")
        cover = " ".join(
            page.xpath('//div[contains(@class,"post-image-inner")]//img/@src')
        ).strip()
        if not title:
            return None
        return Video(
            number=number,
            title=clean_domestic_title(title, number, studio, series),
            actresses=clean_names(actors),
            maker=studio,
            label=studio,
            series=series,
            tags=unique_tags([]),
            cover_url=absolute_url(self.BASE_URL, cover),
            source=self.site_id,
            detail_url=detail_url,
        )


ScraperRegistry.register(CnmdbScraper)
