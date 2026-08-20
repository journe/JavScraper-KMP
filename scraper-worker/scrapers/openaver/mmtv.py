"""7mmtv scraper migrated from the MDCx project (mdcx/mdcx/crawlers/mmtv.py)."""
import re
from typing import Optional

import requests
from lxml import etree

from scrapers.base import BaseScraper
from scrapers.models import Actress, Video
from scrapers.registry import ScraperRegistry


class MmtvScraper(BaseScraper):
    BASE_URL = "https://www.7mmtv.sx"

    @property
    def site_id(self) -> str:
        return "mmtv"

    @property
    def site_name(self) -> str:
        return "7mmtv"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                          "Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "zh-CN,zh;q=0.9,ja;q=0.8",
        })

    def search(self, number: str) -> Optional[Video]:
        number = self.normalize_number(number)
        try:
            real_url = self._find_detail_url(number)
            if not real_url:
                return None
            resp = self._session.get(real_url, timeout=15)
            if resp.status_code != 200:
                return None
            return self._parse(resp.text, resp.url or real_url)
        except (requests.Timeout, requests.ConnectionError):
            return None

    # ------------------------------------------------------------------
    # Search
    # ------------------------------------------------------------------

    def _find_detail_url(self, number: str) -> str:
        keyword = number
        if number.upper().startswith("FC2"):
            digits = re.findall(r"\d{3,}", number)
            keyword = digits[0] if digits else number
        search_url = (
            f"{self.BASE_URL}/zh/searchform_search/all/index.html"
            f"?search_keyword={keyword}&search_type=searchall&op=search"
        )
        resp = self._session.get(search_url, timeout=15)
        if resp.status_code != 200:
            return ""
        html = etree.fromstring(resp.text, etree.HTMLParser())
        return self._match_detail_url(html, number)

    def _match_detail_url(self, html, number: str) -> str:
        cap_number = number.upper()
        for link in html.xpath('//figure[@class="video-preview"]/a'):
            temp_url = link.get("href")
            alts = link.xpath("img/@alt")
            if not temp_url or not alts:
                continue
            temp_title = alts[0]
            temp_number = temp_title.split(" ")[0]
            if cap_number.startswith("FC2"):
                head = cap_number.replace("FC2-", "FC2-PPV ")
                if temp_title.upper().startswith(head):
                    return temp_url
            elif (
                temp_number.upper().startswith(cap_number)
                or temp_number.upper().endswith(cap_number)
                and temp_number.upper().replace(cap_number, "").isdigit()
            ):
                return temp_url
        return ""

    # ------------------------------------------------------------------
    # Detail page parsing
    # ------------------------------------------------------------------

    def _parse(self, html_content: str, detail_url: str) -> Optional[Video]:
        html_info = etree.fromstring(html_content, etree.HTMLParser())
        web_number, release, runtime = self._extract_number_info(html_info)
        title = self._extract_title(html_info, web_number)
        if not title:
            return None

        number = web_number.replace("FC2-PPV ", "FC2-")
        actor_text = self._extract_actor(html_info)
        tags = self._extract_tags(html_info)
        mosaic = self._extract_mosaic(html_info, number)
        if mosaic:
            tags.append(mosaic)

        return Video(
            number=number,
            title=title,
            actresses=[Actress(name=a.strip()) for a in actor_text.split(",") if a.strip()],
            date=release,
            maker=self._extract_studio(html_info),
            label=self._extract_publisher(html_info),
            director=self._extract_director(html_info),
            duration=runtime,
            tags=tags,
            cover_url=self._extract_cover(html_content),
            sample_images=self._extract_sample_images(html_info),
            summary=self._extract_outline(html_info),
            source="mmtv",
            detail_url=detail_url,
        )

    def _extract_number_info(self, html_info):
        result = html_info.xpath('//div[@class="d-flex mb-4"]/span/text()')
        if result:
            web_number = result[0]
        else:
            web_number = ""
        release = _extract_date(result[1]) if len(result) >= 2 else ""
        runtime = _extract_runtime_minutes(result[2]) if len(result) >= 3 else None
        return web_number, release, runtime

    def _extract_title(self, html_info, web_number: str) -> str:
        result = html_info.xpath('//h1[@class="fullvideo-title h5 mb-2"]/text()')
        if not result:
            return ""
        title = result[0]
        if web_number:
            title = title.replace(web_number, "")
        return title.strip()

    def _extract_actor(self, html_info) -> str:
        actor_list = html_info.xpath('//div[@class="fullvideo-idol"]/span/a/text()')
        if not actor_list:
            return ""
        parts = []
        for each in actor_list:
            # 愛澄玲花,日高ゆりあ（青山ひより） 菜津子 32歳 デザイナー
            parts.append(re.sub(r"（.+）", "", each).split(" ")[0])
        return ",".join(parts)

    def _extract_studio(self, html_info) -> str:
        result = html_info.xpath(
            '//div[@class="col-auto flex-shrink-1 flex-grow-1"]/a[contains(@href,"makersr")]/text()'
        )
        return _clean_field(result)

    def _extract_publisher(self, html_info) -> str:
        result = html_info.xpath(
            '//div[@class="col-auto flex-shrink-1 flex-grow-1"]/a[contains(@href,"issuer")]/text()'
        )
        return _clean_field(result)

    def _extract_director(self, html_info) -> str:
        result = html_info.xpath(
            '//div[@class="col-auto flex-shrink-1 flex-grow-1"]/a[contains(@href,"director")]/text()'
        )
        return _clean_field(result)

    def _extract_tags(self, html_info) -> list[str]:
        return [t.strip() for t in html_info.xpath('//div[@class="d-flex flex-wrap categories"]/a/text()') if t.strip()]

    def _extract_cover(self, html_content: str) -> str:
        result = re.findall(r'class="player-cover" ><a><img src="([^"]+)', html_content)
        if not result:
            return ""
        cover = result[0]
        if "http" not in cover:
            cover = self.BASE_URL + cover
        return cover

    def _extract_sample_images(self, html_info) -> list[str]:
        images = html_info.xpath('//span/img[contains(@class, "lazyload")]/@data-src')
        scripts = html_info.xpath(
            '//div[contains(@class, "fullvideo")]/script[@language="javascript"]/text()'
        )
        if scripts:
            images.extend(re.findall(r"https?://.+?\.jpe?g", str(scripts)))
        return images

    def _extract_outline(self, html_info) -> str:
        result = html_info.xpath('//div[@class="video-introduction-images-text"]/p/text()')
        return result[-1] if result else ""

    def _extract_mosaic(self, html_info, number: str) -> str:
        try:
            breadcrumb = html_info.xpath('//ol[@class="breadcrumb"]')[0].xpath("string(.)")
            if "無碼AV" in breadcrumb or "國產影片" in breadcrumb:
                return "无码"
            if "有碼AV" in breadcrumb or "素人AV" in breadcrumb:
                return "有码"
        except Exception:
            pass
        return "无码" if number.upper().startswith("FC2") else "有码"


def _clean_field(result: list[str]) -> str:
    if not result:
        return ""
    value = result[0]
    return "" if value in ("N/A", "----") else value


def _extract_date(text: str) -> str:
    match = re.search(r"\d{4}-\d{2}-\d{2}", text)
    return match[0] if match else ""


def _extract_runtime_minutes(text: str) -> Optional[int]:
    if ":" in text:
        parts = text.split(":")
        if len(parts) == 3:
            return int(parts[0]) * 60 + int(parts[1])
        if len(parts) <= 2:
            return int(parts[0])
    if "分" in text or "min" in text:
        match = re.findall(r"(\d+)(分|min)", text)
        if match:
            return int(match[0][0])
    return None


ScraperRegistry.register(MmtvScraper)