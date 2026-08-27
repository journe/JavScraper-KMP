import re
from typing import Optional

import requests
from bs4 import BeautifulSoup

from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry


class FC2Scraper(BaseScraper):
    BASE_URL = "https://adult.contents.fc2.com"

    @property
    def site_id(self) -> str:
        return "fc2"

    @property
    def site_name(self) -> str:
        return "FC2"

    def __init__(self):
        self._session = requests.Session()
        self._session.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36",
            "Accept-Language": "ja,en;q=0.9",
        })

    def _extract_fc2_id(self, number: str) -> Optional[str]:
        m = re.search(r"(?:FC2[-\s]*PPV[-\s]*)?(\d+)", number, re.IGNORECASE)
        if m:
            return m.group(1)
        return None

    def _search_one(self, number: str) -> Optional[Video]:
        fc2_id = self._extract_fc2_id(number)
        if not fc2_id:
            return None

        try:
            url = f"{self.BASE_URL}/article/{fc2_id}/"
            resp = self._session.get(url, timeout=15)
            if resp.status_code != 200:
                return None

            soup = BeautifulSoup(resp.text, "html.parser")
            video = self._parse(soup, url, fc2_id)
            if video is not None:
                full_summary = self._fetch_summary(soup)
                if len(full_summary) > len(video.summary):
                    video.summary = full_summary
            return video

        except (requests.Timeout, requests.ConnectionError):
            return None

    def _fetch_summary(self, soup: BeautifulSoup) -> str:
        iframe = soup.select_one("section.items_article_Contents iframe[data-iframe=description]")
        src = (iframe.get("src") or "").strip() if iframe else ""
        if not src:
            return ""
        if src.startswith("//"):
            src = "https:" + src
        elif src.startswith("/"):
            src = self.BASE_URL + src

        try:
            resp = self._session.get(src, timeout=15)
            if resp.status_code != 200:
                return ""
            widget = BeautifulSoup(resp.text, "html.parser")
            container = widget.body.find("div", recursive=False) if widget.body else None
            if container is None:
                return ""
            for el in container.find_all(["script", "style", "div"]):
                el.decompose()
            lines = []
            for text in container.find_all(string=True):
                line = text.strip()
                if not line or re.fullmatch(r"[A-Za-z0-9+/=]{16,}", line):
                    continue
                if line not in lines:
                    lines.append(line)
            return "\n".join(lines)
        except (requests.Timeout, requests.ConnectionError):
            return ""

    def _parse(self, soup: BeautifulSoup, url: str, fc2_id: str) -> Optional[Video]:
        og_title = soup.select_one('meta[property="og:title"]')
        raw_title = og_title.get("content", "") if og_title else ""
        if not raw_title:
            return None

        title = re.sub(r"^FC2[-\s]*PPV[-\s]*\d+\s*", "", raw_title, flags=re.IGNORECASE).strip()
        number = f"FC2-PPV-{fc2_id}"

        cover_elem = soup.select_one('meta[property="og:image"]')
        cover_url = (cover_elem.get("content", "") if cover_elem else "").strip()

        date = ""
        header_info = soup.select_one(".items_article_headerInfo")
        if header_info:
            m = re.search(r"販売日\s*[:：]\s*(\d{4}/\d{2}/\d{2})", header_info.get_text())
            if m:
                date = m.group(1).replace("/", "-")

        duration = None
        info_elem = soup.select_one("p.items_article_info")
        if info_elem:
            m = re.search(r"(\d{1,3}):(\d{2}):(\d{2})", info_elem.get_text())
            if m:
                duration = int(m.group(1)) * 60 + int(m.group(2))

        rating = None
        review = soup.select_one("section.items_article_reviewComp")
        if review:
            m = re.search(r"平均評価\s*([\d.]+)", review.get_text())
            if m:
                rating = float(m.group(1))

        tags = []
        for a in soup.select("a.tag.tagTag"):
            t = a.get("data-tag") or a.get_text(strip=True)
            if t and t not in tags:
                tags.append(t)

        sample_images = []
        for img in soup.select("section.items_article_SampleImages img"):
            src = (img.get("src") or img.get("data-src") or "").strip()
            if not src:
                continue
            if src.startswith("//"):
                src = "https:" + src
            if src not in sample_images:
                sample_images.append(src)

        summary = ""
        contents = soup.select_one("section.items_article_Contents")
        if contents:
            for heading in contents.select("h3"):
                heading.decompose()
            summary = contents.get_text("\n", strip=True)
        if not summary:
            og_desc = soup.select_one('meta[property="og:description"]')
            summary = (og_desc.get("content", "") if og_desc else "").strip()

        return Video(
            number=number,
            title=title,
            date=date,
            duration=duration,
            rating=rating,
            tags=tags,
            cover_url=cover_url,
            sample_images=sample_images,
            summary=summary,
            source="fc2",
            detail_url=url,
        )


ScraperRegistry.register(FC2Scraper)