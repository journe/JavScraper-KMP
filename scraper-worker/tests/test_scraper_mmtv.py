"""Tests for the 7mmtv scraper."""
import pytest
from unittest.mock import patch, MagicMock

from scrapers.models import Video
from scrapers.openaver.mixed.mmtv import MmtvScraper


SEARCH_HTML = """
<html><body>
<figure class="video-preview">
  <a href="/zh/amateur_content/123/content.html">
    <img alt="ABC-123 素人サンプルタイトル"></a>
</figure>
</body></html>
"""

DETAIL_HTML = """
<html><body>
<h1 class="fullvideo-title h5 mb-2">ABC-123 素人サンプルタイトル</h1>
<div class="d-flex mb-4">
  <span>ABC-123</span><span>2024-01-15</span><span>120分</span>
</div>
<div class="fullvideo-idol">
  <span><a>愛澄玲花（青山ひより）</a></span>
  <span><a>日高ゆりあ 32歳 デザイナー</a></span>
</div>
<div class="col-auto flex-shrink-1 flex-grow-1"><a href="/makersr/1">ABC Studio</a></div>
<div class="col-auto flex-shrink-1 flex-grow-1"><a href="/issuer/2">DEF Publisher</a></div>
<div class="col-auto flex-shrink-1 flex-grow-1"><a href="/director/3">GHI Director</a></div>
<div class="d-flex flex-wrap categories"><a>高清</a><a>素人</a></div>
<div class="video-introduction-images-text"><p>第一段</p><p>剧情简介</p></div>
<ol class="breadcrumb">有碼AV</ol>
<div class="player-cover" ><a><img src="/images/cover.jpg"></a></div>
<span><img class="lazyload" data-src="/images/s1.jpg"></span>
<span><img class="lazyload" data-src="/images/s2.jpg"></span>
</body></html>
"""

DETAIL_URL = "https://www.7mmtv.sx/zh/amateur_content/123/content.html"


@pytest.fixture(autouse=True)
def clear_registry_teardown():
    from scrapers.registry import ScraperRegistry
    yield
    ScraperRegistry.clear()


def _mock_resp(status_code=200, text="", url=""):
    resp = MagicMock()
    resp.status_code = status_code
    resp.text = text
    resp.url = url
    return resp


def test_auto_register():
    from scrapers.registry import ScraperRegistry
    if ScraperRegistry.get("mmtv") is None:
        ScraperRegistry.register(MmtvScraper)
    assert ScraperRegistry.get("mmtv") is MmtvScraper


def test_site_properties():
    scraper = MmtvScraper()
    assert scraper.site_id == "mmtv"
    assert scraper.site_name == "7mmtv"


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_success(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        _mock_resp(200, SEARCH_HTML, "https://www.7mmtv.sx/zh/searchform_search/all/index.html"),
        _mock_resp(200, DETAIL_HTML, DETAIL_URL),
    ]

    result = MmtvScraper().search("ABC-123")

    assert len(result) == 1
    result = result[0]
    assert isinstance(result, Video)
    assert result.number == "ABC-123"
    assert result.title == "素人サンプルタイトル"
    assert result.date == "2024-01-15"
    assert result.duration == 120
    assert result.maker == "ABC Studio"
    assert result.label == "DEF Publisher"
    assert result.director == "GHI Director"
    assert result.tags == ["高清", "素人", "有码"]
    assert result.cover_url == "https://www.7mmtv.sx/images/cover.jpg"
    assert result.sample_images == ["/images/s1.jpg", "/images/s2.jpg"]
    assert result.summary == "剧情简介"
    assert result.source == "mmtv"
    assert result.detail_url == DETAIL_URL
    assert [a.name for a in result.actresses] == ["愛澄玲花", "日高ゆりあ"]


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_not_found(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.return_value = _mock_resp(200, "<html><body></body></html>")

    result = MmtvScraper().search("ZZZ-999")
    assert result == []


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_connection_error(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import ConnectionError
    mock_session.get.side_effect = ConnectionError()

    result = MmtvScraper().search("ABC-123")
    assert result == []


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_timeout(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import Timeout
    mock_session.get.side_effect = Timeout()

    result = MmtvScraper().search("ABC-123")
    assert result == []


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_empty_title_returns_none(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        _mock_resp(200, SEARCH_HTML),
        _mock_resp(200, "<html><body><div class=\"d-flex mb-4\"><span>ABC-123</span></div></body></html>", DETAIL_URL),
    ]

    result = MmtvScraper().search("ABC-123")
    assert result == []


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_fc2_match_uses_ppv_title(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    fc2_search = """
<html><body>
<figure class="video-preview">
  <a href="/zh/amateur_content/456/content.html">
    <img alt="FC2-PPV 424646 タイトル"></a>
</figure>
</body></html>
"""
    fc2_detail = """
<html><body>
<h1 class="fullvideo-title h5 mb-2">FC2-PPV 424646 タイトル</h1>
<div class="d-flex mb-4"><span>FC2-PPV 424646</span><span>2024-02-01</span><span>60分</span></div>
<ol class="breadcrumb">無碼AV</ol>
<div class="fullvideo-idol"><span><a>TEST-ACTOR</a></span></div>
<div class="player-cover" ><a><img src="https://cdn.example.com/cover.jpg"></a></div>
</body></html>
"""
    mock_session.get.side_effect = [
        _mock_resp(200, fc2_search),
        _mock_resp(200, fc2_detail, "https://www.7mmtv.sx/zh/amateur_content/456/content.html"),
    ]

    result = MmtvScraper().search("FC2-424646")

    assert len(result) == 1
    result = result[0]
    assert result.number == "FC2-424646"
    assert result.tags == ["无码"]



@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_returns_all_matched_candidates(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    search_html = """
<html><body>
<figure class="video-preview">
  <a href="/zh/amateur_content/123/content.html"><img alt="ABC-123 第一结果"></a>
</figure>
<figure class="video-preview">
  <a href="/zh/amateur_content/456/content.html"><img alt="ABC-123 第二结果"></a>
</figure>
</body></html>
"""
    second_html = DETAIL_HTML.replace("素人サンプルタイトル", "第二结果")
    second_url = "https://www.7mmtv.sx/zh/amateur_content/456/content.html"
    mock_session.get.side_effect = [
        _mock_resp(200, search_html, "https://www.7mmtv.sx/zh/searchform_search/all/index.html"),
        _mock_resp(200, DETAIL_HTML, DETAIL_URL),
        _mock_resp(200, second_html, second_url),
    ]

    results = MmtvScraper().search("ABC-123")

    assert [result.detail_url for result in results] == [DETAIL_URL, second_url]
    assert results[0].title == "素人サンプルタイトル"
    assert results[1].title == "第二结果"


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_skips_failed_detail_candidates(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    search_html = """
<html><body>
<figure class="video-preview">
  <a href="/zh/amateur_content/123/content.html"><img alt="ABC-123 第一结果"></a>
</figure>
<figure class="video-preview">
  <a href="/zh/amateur_content/456/content.html"><img alt="ABC-123 第二结果"></a>
</figure>
</body></html>
"""
    empty_title_html = "<html><body><div class=\"d-flex mb-4\"><span>ABC-123</span></div></body></html>"
    second_url = "https://www.7mmtv.sx/zh/amateur_content/456/content.html"
    mock_session.get.side_effect = [
        _mock_resp(200, search_html, "https://www.7mmtv.sx/zh/searchform_search/all/index.html"),
        _mock_resp(200, empty_title_html, DETAIL_URL),
        _mock_resp(200, DETAIL_HTML.replace("素人サンプルタイトル", "第二结果"), second_url),
    ]

    results = MmtvScraper().search("ABC-123")

    assert [result.detail_url for result in results] == [second_url]

def test_normalize_number():
    scraper = MmtvScraper()
    assert scraper.normalize_number("ABC123") == "ABC-123"
    assert scraper.normalize_number("abc-123") == "ABC-123"