import json
from unittest.mock import MagicMock, patch

from scrapers.models import Video
from scrapers.openaver.uncensored.d2pass import D2PassScraper

MOVIE_JSON = {
    "Status": True,
    "Title": "10musume テスト",
    "ActressesJa": ["テスト女優"],
    "Release": "2025-01-02",
    "ThumbHigh": "https://pics.example/cover.jpg",
    "UCNAME": ["テスト", "1080p"],
    "AvgRating": "4.5",
    "Desc": "テスト説明",
    "Series": "テストシリーズ",
    "Duration": 3900,
    "SampleImages": ["https://pics.example/sample.jpg"],
}

CARIBBEAN_HTML = """
<html><body>
<h1>Caribbean テスト</h1>
<li>
再生時間 01:02:03
シリーズ <a>シリーズ</a>
</li><li>出演 <a>女優</a>
</li><li>タグ <a>タグ</a>
</li>
<span class="meta-rating">★★★★★</span>
<p itemprop="description">説明</p>
<img src="/moviepages/020924-001/images/l/001.jpg">
<img src="/moviepages/020924-001/images/l/001.jpg">
<img src="/moviepages/020924-001/images/l/002.jpg">
</body></html>
"""


def response(body, status_code=200, content_type="application/json"):
    item = MagicMock()
    item.status_code = status_code
    item.text = body if isinstance(body, str) else json.dumps(body)
    item.content = item.text.encode()
    item.headers = {"Content-Type": content_type}
    if isinstance(body, dict):
        item.json.return_value = body
    return item


def test_site_properties_and_site_order():
    scraper = D2PassScraper()

    assert scraper.site_id == "d2pass"
    assert scraper.site_name == "D2Pass"
    assert scraper._detect_site_order("020924-001") == ["caribbeancom", "1pondo", "10musume"]
    assert scraper._detect_site_order("120415_201") == ["1pondo", "caribbeancom", "10musume"]
    assert scraper._detect_site_order("082912_01") == ["10musume", "1pondo", "caribbeancom"]


@patch("scrapers.openaver.uncensored.d2pass.requests.Session")
def test_search_10musume_json(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.return_value = response(MOVIE_JSON)

    result = D2PassScraper().search("082912_01")

    assert len(result) == 1
    video = result[0]
    assert isinstance(video, Video)
    assert video.number == "082912_01"
    assert video.title == "10musume テスト"
    assert [actress.name for actress in video.actresses] == ["テスト女優"]
    assert video.date == "2025-01-02"
    assert video.cover_url == "https://pics.example/cover.jpg"
    assert video.tags == ["テスト"]
    assert video.rating == 4.5
    assert video.summary == "テスト説明"
    assert video.series == "テストシリーズ"
    assert video.duration == 65
    assert video.sample_images == ["https://pics.example/sample.jpg"]
    assert video.detail_url == "https://www.10musume.com/moviepages/082912_01/index.html"


@patch("scrapers.openaver.uncensored.d2pass.requests.Session")
def test_search_caribbeancom_html_fallback_and_gallery(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        response(None, status_code=404),
        response(CARIBBEAN_HTML, content_type="text/html"),
    ]

    result = D2PassScraper().search("020924-001")

    assert len(result) == 1
    video = result[0]
    assert video.title == "Caribbean テスト"
    assert video.duration == 62
    assert video.series == "シリーズ"
    assert [actress.name for actress in video.actresses] == ["女優"]
    assert video.tags == ["タグ"]
    assert video.summary == "説明"
    assert video.rating == 5.0
    assert video.sample_images == [
        "https://www.caribbeancom.com/moviepages/020924-001/images/l/001.jpg",
        "https://www.caribbeancom.com/moviepages/020924-001/images/l/002.jpg",
    ]


@patch("scrapers.openaver.uncensored.d2pass.requests.Session")
def test_not_found_and_timeout(mock_session_cls):
    from requests import Timeout

    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    calls = {"count": 0}

    def get_side_effect(*args, **kwargs):
        calls["count"] += 1
        if calls["count"] <= 4:
            return response(None, status_code=404)
        raise Timeout()

    mock_session.get.side_effect = get_side_effect

    scraper = D2PassScraper()
    assert scraper.search("123456_01") == []
    assert scraper.search("123456_01") == []
