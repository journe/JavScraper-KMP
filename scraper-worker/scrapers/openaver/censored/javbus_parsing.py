import re
from typing import Optional

from bs4 import BeautifulSoup

from scrapers.models import Actress, Video

FIELD_LABELS = {
    "zh-tw": {
        "number": "識別碼",
        "date": "發行日期",
        "duration": "長度",
        "director": "導演",
        "maker": "製作商",
        "label": "發行商",
        "series": "系列",
        "tags": "類別",
        "actresses": "演員",
    },
    "ja": {
        "number": "品番",
        "date": "発売日",
        "duration": "収録時間",
        "director": "監督",
        "maker": "メーカー",
        "label": "レーベル",
        "series": "シリーズ",
        "tags": "ジャンル",
        "actresses": "出演者",
    },
    "en": {
        "number": "ID",
        "date": "Release Date",
        "duration": "Length",
        "director": "Director",
        "maker": "Studio",
        "label": "Label",
        "series": "Series",
        "tags": "Genre",
        "actresses": "JAV Idols",
    },
}

LANG_PREFIX = {"zh-tw": "", "ja": "/ja", "en": "/en"}


def get_labels(lang: str) -> dict:
    return FIELD_LABELS.get(lang, FIELD_LABELS["zh-tw"])


def get_lang_prefix(lang: str) -> str:
    return LANG_PREFIX.get(lang, "")


def strip_title_prefix(title: str, number: str) -> str:
    if not title or not number:
        return title or ""
    for candidate in (number, number.replace("-", "")):
        pattern = r"^\s*" + re.escape(candidate) + r"(?![A-Za-z0-9])\s*"
        stripped = re.sub(pattern, "", title, flags=re.IGNORECASE)
        if stripped != title:
            return stripped
    return title


def _parse_info_paragraphs(paragraphs, labels: dict) -> dict:
    result = {
        "date": "",
        "duration": "",
        "director": "",
        "maker": "",
        "label": "",
        "series": "",
        "tags": [],
        "actresses": [],
    }

    for index, paragraph in enumerate(paragraphs):
        text = paragraph.get_text(strip=True)
        for field in ("date", "director", "maker", "label", "series"):
            label = labels[field]
            if label not in text:
                continue
            link = paragraph.find("a")
            if link:
                result[field] = link.get_text(strip=True)
            else:
                value = text.replace(label, "").replace(":", "").replace("：", "")
                result[field] = value.strip()
            break

        if labels["duration"] in text:
            result["duration"] = text
        elif not result["duration"]:
            duration_match = re.search(r"(\d+)\s*(?:分钟|分鐘|分|min)", text)
            if duration_match:
                result["duration"] = duration_match.group(0)

        if labels["tags"] in text:
            result["tags"] = _linked_texts(paragraph)
        if labels["actresses"] in text and not result["actresses"]:
            result["actresses"] = _linked_texts(paragraph)

        _parse_links_fallback(paragraph, result)
        date_match = re.search(r"(\d{4}-\d{2}-\d{2})", text)
        if date_match and not result["date"]:
            result["date"] = date_match.group(1)

    return result


def _linked_texts(paragraph) -> list[str]:
    return [link.get_text(strip=True) for link in paragraph.find_all("a") if link.get_text(strip=True)]


def _parse_links_fallback(paragraph, result: dict) -> None:
    tags: list[str] = list(result["tags"])
    actresses: list[str] = list(result["actresses"])
    for link in paragraph.find_all("a"):
        href = link.get("href", "")
        value = link.get_text(strip=True)
        if not value:
            continue
        if "/genre/" in href and value not in tags:
            tags.append(value)
        elif "/star/" in href and value not in actresses:
            actresses.append(value)
        elif "/studio/" in href:
            result["maker"] = value
        elif "/label/" in href:
            result["label"] = value
        elif "/series/" in href:
            result["series"] = value
        elif "/director/" in href:
            result["director"] = value
    result["tags"] = tags
    result["actresses"] = actresses


def parse_detail_page(
    soup: BeautifulSoup,
    number: str,
    detail_url: str,
    base_url: str,
    lang: str,
) -> Optional[Video]:
    info = soup.find(class_="col-md-3 info")
    if not info:
        return None

    labels = get_labels(lang)
    title_elem = soup.find("h3")
    title = title_elem.get_text(strip=True) if title_elem else ""
    if not title:
        big_image = soup.find(class_="bigImage")
        image = big_image.find("img", {"title": True}) if big_image else None
        title = image.get("title", "") if image else ""
    title = strip_title_prefix(title, number)

    big_image = soup.find(class_="bigImage")
    cover_url = big_image.get("href", "") if big_image else ""
    if cover_url and not cover_url.startswith(("http://", "https://")):
        cover_url = base_url.rstrip("/") + "/" + cover_url.lstrip("/")

    parsed = _parse_info_paragraphs(info.find_all("p"), labels)
    duration_match = re.search(r"(\d+)", parsed.get("duration", ""))
    duration = int(duration_match.group(1)) if duration_match else None

    seen_names: set[str] = set()
    actresses = []
    for name in parsed.get("actresses", []):
        if name and name not in seen_names:
            seen_names.add(name)
            actresses.append(Actress(name=name))

    sample_section = soup.find(id="sample-waterfall") or soup.find(class_="sample-waterfall")
    sample_images = []
    if sample_section:
        for anchor in sample_section.find_all("a", href=True):
            image_url = anchor["href"]
            if not image_url.startswith(("http://", "https://")):
                image_url = base_url.rstrip("/") + "/" + image_url.lstrip("/")
            if image_url not in sample_images:
                sample_images.append(image_url)
        if not sample_images:
            for image in sample_section.find_all("img", src=True):
                image_url = base_url.rstrip("/") + "/" + image["src"].lstrip("/")
                if image_url not in sample_images:
                    sample_images.append(image_url)

    return Video(
        number=number,
        title=title,
        actresses=actresses,
        date=parsed.get("date", ""),
        maker=parsed.get("maker", ""),
        label=parsed.get("label", ""),
        series=parsed.get("series", ""),
        director=parsed.get("director", ""),
        duration=duration,
        tags=parsed.get("tags", []),
        cover_url=cover_url,
        sample_images=sample_images,
        source="javbus",
        detail_url=detail_url,
    )


def build_search_url(base_url: str, keyword: str, page: int = 1, search_type: int = 0) -> str:
    url = f"{base_url}/search/{keyword}"
    if page > 1:
        url += f"/{page}"
    if search_type > 0:
        url += f"&type={search_type}"
    return url


def parse_search_ids(soup: BeautifulSoup) -> list[str]:
    waterfall = soup.find(id="waterfall")
    if not waterfall:
        return []

    ids = []
    for card in waterfall.find_all("a", class_="movie-box"):
        dates = card.find_all("date")
        if dates:
            number = dates[0].get_text(strip=True)
        else:
            number = card.get("href", "").rstrip("/").split("/")[-1]
        if number:
            ids.append(number)
    return ids
