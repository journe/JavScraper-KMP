from typing import Optional

import requests
from lxml import etree

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry

from .common import (
    absolute_url,
    clean_domestic_title,
    clean_names,
    normalize_domestic_number,
    unique_tags,
)


class JavdayScraper(BaseScraper):
    BASE_URL = "https://javday.tv"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9",
        })

    @property
    def site_id(self) -> str:
        return "javday"

    @property
    def site_name(self) -> str:
        return "Javday"

    def _search_one(self, number: str) -> Optional[Video]:
        number = normalize_domestic_number(number)
        detail_url = f"{self.BASE_URL}/videos/{number}/"
        try:
            response = self._session.get(detail_url, timeout=15)
            if response.status_code != 200 or "你似乎來到了沒有視頻存在的荒原" in response.text:
                return None
            page = etree.fromstring(response.text, etree.HTMLParser())
            return self._parse_detail(page, number, detail_url)
        except (requests.RequestException, etree.LxmlError, ValueError):
            return None

    def _parse_detail(self, page, number: str, detail_url: str) -> Optional[Video]:
        title = " ".join(page.xpath('//*[@id="videoInfo"]/div/h1//text()')).strip()
        if not title:
            return None
        series = " ".join(
            page.xpath('//*[@id="videoInfo"]//p[3]/span[2]//text()')
        ).strip()
        category_nodes = page.xpath('//*[@id="videoInfo"]//p[1]/span[2]/a//text()')
        category_values = [text.strip() for text in category_nodes if text.strip()]
        tags = category_values
        actors = []
        if category_values and category_values[0] in {"国产", "國產"}:
            tags = category_values[:1]
            actors = category_values[1:]
        publisher = " ".join(
            page.xpath('//*[@id="videoInfo"]//p[2]/span[2]//text()')
        ).strip()
        cover_values = page.xpath(
            '//meta[@property="og:image"]/@content | //meta[contains(@content,".jpg")]/@content'
        )
        cover = absolute_url(self.BASE_URL, cover_values[0] if cover_values else "")
        return Video(
            number=number,
            title=clean_domestic_title(title, number, series, publisher, *tags),
            actresses=clean_names(actors),
            maker=publisher,
            label=publisher,
            series=series,
            tags=unique_tags(tags),
            cover_url=cover,
            source=self.site_id,
            detail_url=detail_url,
        )


ScraperRegistry.register(JavdayScraper)
