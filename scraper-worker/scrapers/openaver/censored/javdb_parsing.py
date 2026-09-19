"""Parser helpers for JavDB search and detail pages."""

import re
from dataclasses import dataclass
from urllib.parse import urljoin

from bs4 import BeautifulSoup

from scrapers.labels import (
    DATE_LABELS,
    DIRECTOR_LABELS,
    DURATION_LABELS,
    LABEL_LABELS,
    MAKER_LABELS,
    RATING_LABELS,
    SERIES_LABELS,
    label_matches,
)
from scrapers.models import Actress, Video

ACTRESS_LABELS = ("演員", "演员", "Actor", "出演")
CATEGORY_LABELS = ("類別", "类别", "Genre", "ジャンル")


@dataclass(frozen=True)
class JavdbSearchEntry:
    number: str
    title: str
    detail_url: str


def _clean_code(value: str) -> str:
    return re.sub(r"[\s._-]+", "", value).upper()


def parse_search_entries(soup: BeautifulSoup, base_url: str) -> list[JavdbSearchEntry]:
    entries = []
    for item in soup.select(".movie-list .item"):
        number_elem = item.select_one(".video-title strong")
        link = item.select_one('a[href*="/v/"]')
        if not number_elem or not link:
            continue
        number = number_elem.get_text(strip=True)
        href = str(link.get("href") or "").strip()
        if not number or not href:
            continue
        title_elem = item.select_one(".video-title")
        entries.append(
            JavdbSearchEntry(
                number=number,
                title=title_elem.get_text(" ", strip=True) if title_elem else number,
                detail_url=urljoin(base_url, href),
            )
        )
    return entries


def find_detail_url(entries: list[JavdbSearchEntry], number: str) -> str:
    wanted = _clean_code(number)
    for entry in entries:
        if _clean_code(entry.number) == wanted:
            return entry.detail_url
    for entry in entries:
        content = _clean_code(f"{entry.number} {entry.title}")
        if wanted and wanted in content:
            return entry.detail_url
    return ""


def _classes(element) -> list[str]:
    if element is None:
        return []
    value = element.get("class") or []
    if isinstance(value, str):
        return [value]
    return [str(item) for item in value]


def _actor_gender(anchor) -> str:
    classes = set(_classes(anchor))
    if {"female", "actor-female"} & classes:
        return "female"
    if {"male", "actor-male"} & classes:
        return "male"

    sibling_classes = set(_classes(anchor.find_next_sibling()))
    if "female" in sibling_classes:
        return "female"
    if "male" in sibling_classes:
        return "male"
    return "unknown"


def _parse_duration(value: str) -> int | None:
    match = re.search(r"(\d+)", value)
    return int(match.group(1)) if match else None


def _parse_rating(value: str) -> float | None:
    match = re.search(r"([0-9.]+)\s*(?:分|/5)", value)
    if not match:
        match = re.search(r"([0-9.]+)", value)
    if not match:
        return None
    try:
        return float(match.group(1))
    except ValueError:
        return None


def _unique(values: list[str]) -> list[str]:
    return list(dict.fromkeys(value for value in values if value))


def parse_detail_page(
    soup: BeautifulSoup,
    number: str,
    detail_url: str,
) -> Video | None:
    title_elem = soup.select_one(".video-detail h2, .title.is-4")
    title = title_elem.get_text(separator=" ", strip=True) if title_elem else ""
    title = re.sub(rf"^{re.escape(number)}\s*", "", title, flags=re.IGNORECASE)

    cover_elem = soup.select_one(".video-cover img, .column-video-cover img")
    cover_url = ""
    if cover_elem:
        cover_url = str(cover_elem.get("src") or cover_elem.get("data-src") or "")
        cover_url = urljoin(detail_url, cover_url)
        cover_url = cover_url.replace("ps.jpg", "pl.jpg").replace("/pt/", "/pl/")

    if not title and not cover_url:
        return None

    date = ""
    maker = ""
    label = ""
    series = ""
    director = ""
    duration = None
    rating = None
    tags: list[str] = []
    actresses: list[Actress] = []

    for panel in soup.select(".panel-block"):
        label_elem = panel.select_one("strong")
        if not label_elem:
            continue
        label_text = label_elem.get_text(" ", strip=True)
        value_elem = panel.select_one(".value")
        value_text = value_elem.get_text(" ", strip=True) if value_elem else ""

        if label_matches(label_text, DATE_LABELS):
            date = value_text
        elif label_matches(label_text, MAKER_LABELS):
            maker = value_text
        elif label_matches(label_text, DIRECTOR_LABELS):
            director = value_text
        elif label_matches(label_text, LABEL_LABELS):
            label = value_text
        elif label_matches(label_text, SERIES_LABELS):
            series = value_text
        elif label_matches(label_text, DURATION_LABELS):
            duration = _parse_duration(value_text)
        elif label_matches(label_text, RATING_LABELS):
            rating = _parse_rating(value_text)

        if label_matches(label_text, CATEGORY_LABELS):
            tags.extend(
                anchor.get_text(" ", strip=True)
                for anchor in panel.select('a[href*="/tags"]')
            )

        if label_matches(label_text, ACTRESS_LABELS):
            anchors = panel.select('a[href*="/actors/"]')
            genders = [_actor_gender(anchor) for anchor in anchors]
            has_explicit_gender = any(gender != "unknown" for gender in genders)
            for anchor, gender in zip(anchors, genders):
                if gender == "male":
                    continue
                if has_explicit_gender and gender == "unknown":
                    continue
                if gender == "female" or not has_explicit_gender:
                    actresses.append(Actress(name=anchor.get_text(" ", strip=True)))

    return Video(
        number=number,
        title=title,
        actresses=list({actress.name: actress for actress in actresses}.values()),
        date=date,
        maker=maker,
        label=label,
        series=series,
        director=director,
        duration=duration,
        rating=rating,
        tags=_unique(tags),
        cover_url=cover_url,
        source="javdb",
        detail_url=detail_url,
    )
