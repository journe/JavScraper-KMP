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
    from core.scrape_errors import WebpageRequestTimeoutError
    mock_session.get.side_effect = Timeout()

    with pytest.raises(WebpageRequestTimeoutError) as error:
        MmtvScraper().search("ABC-123")

    assert error.value.stage == "webpage_request"
    assert error.value.site_id == "mmtv"

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


@patch("scrapers.openaver.mixed.mmtv.requests.Session")
def test_search_save_webpage_archives_each_candidate(mock_session_cls):
    import base64
    import email
    from email.policy import default as email_policy

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
</body></html>"""
    second_url = "https://www.7mmtv.sx/zh/amateur_content/456/content.html"
    second_html = DETAIL_HTML.replace("素人サンプルタイトル", "第二结果")

    def page_response(text, url):
        response = MagicMock()
        response.status_code = 200
        response.text = text
        response.url = url
        response.content = text.encode("utf-8")
        response.headers = {"Content-Type": "text/html"}
        return response

    pages = iter([
        page_response(search_html, "https://www.7mmtv.sx/zh/searchform_search/all/index.html"),
        page_response(DETAIL_HTML, DETAIL_URL),
        page_response(second_html, second_url),
    ])

    def get_side_effect(url, *args, **kwargs):
        try:
            return next(pages)
        except StopIteration:
            resource = MagicMock()
            resource.status_code = 200
            resource.url = url
            resource.content = b"resource-bytes"
            resource.headers = {"Content-Type": "image/jpeg"}
            return resource

    mock_session.get.side_effect = get_side_effect

    results = MmtvScraper().search("ABC-123", save_webpage=True)

    assert [result.detail_url for result in results] == [DETAIL_URL, second_url]
    for result in results:
        assert result.webpage
        archive = email.message_from_bytes(base64.b64decode(result.webpage), policy=email_policy)
        locations = {part["Content-Location"] for part in archive.walk() if part.get("Content-Location")}
        assert result.detail_url in locations

def test_normalize_number():
    scraper = MmtvScraper()
    assert scraper.normalize_number("ABC123") == "ABC-123"
    assert scraper.normalize_number("abc-123") == "ABC-123"
