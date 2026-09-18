import re
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
    compact_number,
    find_domestic_studio,
    normalize_domestic_number,
    unique_tags,
)


class MdtvScraper(BaseScraper):
    BASE_URL = "https://www.mdpjzip.xyz"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9",
        })

    @property
    def site_id(self) -> str:
        return "mdtv"

    @property
    def site_name(self) -> str:
        return "MDTV"

    def _search_one(self, number: str) -> Optional[Video]:
        number = normalize_domestic_number(number)
        try:
            response = self._session.post(
                f"{self.BASE_URL}/index.php/vodsearch/-------------.html",
                data={"wd": number},
                timeout=15,
            )
            if response.status_code != 200 or "没有找到匹配数据" in response.text:
                return None
            search_page = etree.fromstring(response.text, etree.HTMLParser())
            detail_url = self._find_detail_url(search_page, number)
            if not detail_url:
                return None
            response = self._session.get(detail_url, timeout=15)
            if response.status_code != 200:
                return None
            return self._parse_detail(etree.fromstring(response.text, etree.HTMLParser()), number, detail_url)
        except (requests.RequestException, etree.LxmlError, ValueError):
            return None

    def _find_detail_url(self, page, number: str) -> str:
        target = compact_number(number)
        for link in page.xpath('//h4[contains(@class,"post-title")]/a[@href]'):
            title = str(link.get("title") or "".join(link.itertext()))
            if target in compact_number(title):
                return absolute_url(self.BASE_URL, link.get("href", ""))
        return ""

    def _parse_detail(self, page, number: str, detail_url: str) -> Optional[Video]:
        title = " ".join(page.xpath('//div[contains(@class,"blog-single")]/div/a/@title')).strip()
        if not title:
            return None
        categories = page.xpath('//div[contains(@class,"category")]')
        series = self._node_text(categories[0]) if categories else ""
        raw_tags = categories[1].xpath(".//a/text()") if len(categories) > 1 else []
        raw_actors = categories[2].xpath(".//a/text()") if len(categories) > 2 else []
        actors = clean_names(raw_actors)
        tags = [tag for tag in raw_tags if tag not in {actor.name for actor in actors}]
        cover = " ".join(
            page.xpath('//div[contains(@class,"blog-single")]/div/a/img/@src')
        ).strip()
        release_match = re.search(r"/(\d{4})(\d{2})(\d{2})-", cover)
        date = "-".join(release_match.groups()) if release_match else ""
        studio = find_domestic_studio(title, series, *tags)
        parsed_title = clean_domestic_title(title, number, series, studio, *tags)
        return Video(
            number=number,
            title=parsed_title,
            actresses=actors,
            date=date,
            maker=studio,
            label=studio,
            series=series,
            tags=unique_tags(tags),
            cover_url=absolute_url(self.BASE_URL, cover),
            source=self.site_id,
            detail_url=detail_url,
        )

    @staticmethod
    def _node_text(node) -> str:
        return " ".join(part.strip() for part in node.xpath(".//text()") if part.strip())


ScraperRegistry.register(MdtvScraper)
