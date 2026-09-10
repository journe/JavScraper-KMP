import html
import re
from typing import Optional

from scrapers.models import Actress, Video

SITES = {
    "1pondo": "https://www.1pondo.tv/dyn/phpauto/movie_details/movie_id/{id}.json",
    "caribbeancom": "https://b.caribbeancom.com/dyn/phpauto/movie_details/movie_id/{id}.json",
    "10musume": "https://www.10musume.com/dyn/phpauto/movie_details/movie_id/{id}.json",
}

SITE_DETAIL_URL = {
    "1pondo": "https://www.1pondo.tv/movies/{id}/",
    "caribbeancom": "https://www.caribbeancom.com/moviepages/{id}/index.html",
    "10musume": "https://www.10musume.com/moviepages/{id}/index.html",
}


def parse_json(data: dict, site: str, movie_id: str) -> Optional[Video]:
    if not data.get("Status", False):
        return None

    title = data.get("Title") or data.get("TitleEn") or ""
    if not title:
        return None

    actress_names = data.get("ActressesJa") or data.get("ActressesEn") or []
    if not actress_names:
        actress_list = data.get("ActressesList") or {}
        actress_names = [
            item.get("NameJa") or item.get("NameEn", "")
            for item in actress_list.values()
            if item.get("NameJa") or item.get("NameEn")
        ]
    actresses = [Actress(name=name) for name in actress_names if name]

    cover_url = data.get("ThumbHigh") or data.get("MovieThumb") or ""
    if not cover_url and site == "caribbeancom":
        cover_url = f"https://www.caribbeancom.com/moviepages/{movie_id}/images/l_l.jpg"
    if not cover_url and site == "1pondo":
        cover_url = f"https://www.1pondo.tv/assets/sample/{movie_id}/str.jpg"

    tags = data.get("UCNAME") or data.get("UCNAMEEn") or []
    tags = [tag for tag in tags if not re.match(r"^\d+p$", str(tag))]

    rating = None
    if data.get("AvgRating") is not None:
        try:
            average = float(data["AvgRating"])
            rating = average if average <= 5 else None
        except (TypeError, ValueError):
            rating = None

    duration = None
    if data.get("Duration") is not None:
        try:
            duration = int(data["Duration"]) // 60
        except (TypeError, ValueError):
            duration = None

    return Video(
        number=movie_id,
        title=title,
        actresses=actresses,
        date=data.get("Release", ""),
        maker="",
        cover_url=cover_url,
        tags=tags,
        source="d2pass",
        detail_url=SITE_DETAIL_URL[site].format(id=movie_id),
        rating=rating,
        series=data.get("Series") or data.get("SeriesJa") or data.get("SeriesEn") or "",
        duration=duration,
        sample_images=data.get("SampleImages") or [],
        summary=data.get("Desc") or "",
    )


def extract_gallery(html_text: str, movie_id: str) -> list[str]:
    numbers = re.findall(r"images/l/(\d{3})\.jpg", html_text)
    base = f"https://www.caribbeancom.com/moviepages/{movie_id}/images/l"
    images = []
    for number in numbers:
        image_url = f"{base}/{number}.jpg"
        if image_url not in images:
            images.append(image_url)
    return images


def parse_caribbeancom_html(html_text: str, movie_id: str) -> Optional[Video]:
    title_match = re.search(r"<h1[^>]*>([^<]+)</h1>", html_text)
    if not title_match:
        return None

    duration = None
    duration_match = re.search(r"再生時間.*?(\d{2}):(\d{2}):(\d{2})", html_text, re.DOTALL)
    if duration_match:
        hours, minutes, _ = map(int, duration_match.groups())
        duration = hours * 60 + minutes

    series = ""
    series_match = re.search(r"シリーズ.*?<a[^>]*>([^<]+)</a>", html_text, re.DOTALL)
    if series_match:
        series = series_match.group(1).strip()

    actresses = []
    actress_match = re.search(r"出演(.*?)</li>", html_text, re.DOTALL)
    if actress_match:
        names = re.findall(r"<a[^>]*>([^<]+)</a>", actress_match.group(1))
        actresses = [Actress(name=name.strip()) for name in names if name.strip()]

    tags = []
    tag_match = re.search(r"タグ(.*?)</li>", html_text, re.DOTALL)
    if tag_match:
        values = re.findall(r"<a[^>]*>([^<]+)</a>", tag_match.group(1))
        tags = [tag.strip() for tag in values if tag.strip() and not re.match(r"^\d+p$", tag.strip())]

    summary = ""
    summary_match = re.search(
        r'<p[^>]*itemprop="description"[^>]*>(.*?)</p>',
        html_text,
        re.DOTALL,
    )
    if summary_match:
        summary = html.unescape(re.sub(r"<[^>]+>", "", summary_match.group(1))).strip()

    rating = None
    rating_match = re.search(r"meta-rating[^>]*>([^<]*)</span>", html_text)
    if rating_match:
        stars = rating_match.group(1).count("★")
        rating = float(stars) if 0 < stars <= 5 else None

    return Video(
        number=movie_id,
        title=title_match.group(1).strip(),
        actresses=actresses,
        date="",
        maker="",
        cover_url=f"https://www.caribbeancom.com/moviepages/{movie_id}/images/l_l.jpg",
        tags=tags,
        source="d2pass",
        detail_url=SITE_DETAIL_URL["caribbeancom"].format(id=movie_id),
        rating=rating,
        series=series,
        duration=duration,
        sample_images=extract_gallery(html_text, movie_id),
        summary=summary,
    )