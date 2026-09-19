"""Parsers for JavDB counters, rankings, ratings and reviews."""

import re
from typing import Optional

from bs4 import BeautifulSoup
from bs4.element import NavigableString

from scrapers.models import Ranking, Review


def parse_rating(value: str) -> tuple[Optional[float], Optional[int]]:
    match = re.search(r"([0-9]+(?:\.[0-9]+)?)", value)
    if not match:
        return None, parse_rating_count(value)
    try:
        rating = round(float(match.group(1)) * 2, 2)
    except ValueError:
        rating = None
    return rating, parse_rating_count(value)


def parse_rating_count(value: str) -> Optional[int]:
    patterns = (
        r"(?:由|by)\s*([\d,，]+)\s*(?:人(?:評價|评价)|users?)",
        r"([\d,，]+)\s*(?:人(?:評價|评价)|users?)",
    )
    for pattern in patterns:
        match = re.search(pattern, value, flags=re.IGNORECASE)
        if match:
            return _parse_int(match.group(1))
    return None


def parse_watch_counts(soup: BeautifulSoup) -> tuple[Optional[int], Optional[int]]:
    node = soup.select_one(".panel-block span.is-size-7.has-text-grey")
    text = node.get_text(" ", strip=True) if node else ""
    want = _first_count(
        text,
        r"([\d,，]+)\s*(?:人想看|want to watch(?: it)?)",
    )
    watched = _first_count(
        text,
        r"([\d,，]+)\s*(?:人看過|人看过|have seen(?: it)?)",
    )
    return want, watched


def parse_rankings(soup: BeautifulSoup) -> list[Ranking]:
    rankings = []
    for node in soup.select(".control.ranking-tags"):
        text = re.sub(r"\s+", " ", node.get_text(" ", strip=True)).strip()
        match = re.match(r"^No\.\s*(\d+)\s+(.+)$", text, flags=re.IGNORECASE)
        if not match:
            continue
        rankings.append(
            Ranking(
                rank=int(match.group(1)),
                list_name=match.group(2).strip(),
            )
        )
    return rankings


def parse_reviews(soup: BeautifulSoup) -> list[Review]:
    reviews = []
    for item in soup.select("dl.review-items dt.review-item:not(.more)"):
        title = item.select_one(".review-title")
        author = _direct_text(title)
        date = _text(item.select_one(".time"))
        likes = _parse_int(_text(item.select_one(".likes-count")))
        score = _parse_score(item)
        content = _text(item.select_one(".content"), separator="\n")
        reviews.append(
            Review(
                id=_review_id(item),
                author=author,
                date=date,
                score=score,
                likes=likes,
                content=content,
            )
        )
    return reviews


def _parse_score(item) -> Optional[float]:
    stars = item.select(".score-stars i.icon-star")
    filled = [star for star in stars if "gray" not in (star.get("class") or [])]
    return float(len(filled)) if filled else None


def _review_id(item) -> str:
    value = str(item.get("id") or "")
    return value.removeprefix("review-item-")


def _direct_text(node) -> str:
    if node is None:
        return ""
    for child in node.children:
        if isinstance(child, NavigableString) and str(child).strip():
            return str(child).strip()
    return ""


def _text(node, separator: str = " ") -> str:
    if node is None:
        return ""
    value = node.get_text(separator, strip=True)
    return re.sub(r"[ \t]+", " ", value).strip()


def _first_count(value: str, pattern: str) -> Optional[int]:
    match = re.search(pattern, value, flags=re.IGNORECASE)
    return _parse_int(match.group(1)) if match else None


def _parse_int(value: str) -> Optional[int]:
    digits = re.sub(r"[^\d]", "", value or "")
    return int(digits) if digits else None
