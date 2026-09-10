import pytest
from unittest.mock import MagicMock, patch

from bs4 import BeautifulSoup

from scrapers.models import Video
from scrapers.openaver.censored.javbus import JavBusScraper

SAMPLE_HTML = """
<html><body>
<h3>SONE-205 Sample Title</h3>
<div class="col-md-3 info">
<p>发行日期: 2025-01-15</p>
<p>长度: 120 分钟</p>
<p>导演: <a href="/director/jkl">JKL Director</a></p>
<p>制作商: <a href="/studio/abc">ABC Studio</a></p>
<p>发行商: <a href="/label/def">DEF Label</a></p>
<p>系列: <a href="/series/ghi">GHI Series</a></p>
<p><a href="/genre/1">HD</a><a href="/genre/2">独家</a></p>
<p>演員 :</p>
<p><a href="/star/1">Hitomi Tanaka</a><a href="/star/2">Yui Hatano</a></p>
</div>
<a class="bigImage" href="https://pics.javbus.com/cover.jpg"><img src="cover_s.jpg"></a>
<div id="sample-waterfall">
<a class="sample-box" href="/pics/sample/1.jpg"><img src="/pics/sample/1s.jpg"></a>
<a class="sample-box" href="/pics/sample/2.jpg"><img src="/pics/sample/2s.jpg"></a>
</div>
</body></html>
"""

JP_HTML = """
<html><body>
<h3>ABC-123 日本語タイトル</h3>
<div class="col-md-3 info">
<p>品番: ABC-123</p>
<p>発売日: 2025-02-03</p>
<p>収録時間: 90分</p>
<p>監督: <a href="/director/x">監督氏</a></p>
<p>メーカー: <a href="/studio/x">メーカー</a></p>
<p>レーベル: <a href="/label/x">レーベル</a></p>
<p>シリーズ: <a href="/series/x">シリーズ</a></p>
<p>ジャンル: <a href="/genre/x">タグ</a></p>
<p>出演者:</p>
<p><a href="/star/x">出演者</a></p>
</div>
<a class="bigImage" href="/cover.jpg"><img title="ABC-123 日本語タイトル"></a>
<div id="sample-waterfall"><a href="/sample.jpg"><img src="/sample_s.jpg"></a></div>
</body></html>
"""

SEARCH_HTML = """
<html><body><div id="waterfall">
<a class="movie-box" href="https://www.javbus.com/ABC-456"><date>ABC-456</date></a>
<a class="movie-box" href="https://www.javbus.com/DEF-789"></a>
</div></body></html>
"""


@pytest.fixture(autouse=True)
def clear_registry():
    from scrapers.registry import ScraperRegistry

    if ScraperRegistry.get("javbus") is None:
        ScraperRegistry.register(JavBusScraper)

    yield
    ScraperRegistry.clear()


def response(html: str, url: str, status_code: int = 200) -> MagicMock:
    item = MagicMock()
    item.status_code = status_code
    item.text = html
    item.content = html.encode("utf-8")
    item.url = url
    return item


def test_auto_register_and_site_properties():
    from scrapers.registry import ScraperRegistry

    assert ScraperRegistry.get("javbus") is JavBusScraper
    scraper = JavBusScraper()
    assert scraper.site_id == "javbus"
    assert scraper.site_name == "JavBus"


@patch("scrapers.openaver.censored.javbus.requests.Session")
def test_search_success(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.return_value = response(SAMPLE_HTML, "https://www.javbus.com/SONE-205")

    result = JavBusScraper().search("sone205")

    assert len(result) == 1
    video = result[0]
    assert isinstance(video, Video)
    assert video.number == "SONE-205"
    assert video.title == "Sample Title"
    assert video.date == "2025-01-15"
    assert video.duration == 120
    assert video.maker == "ABC Studio"
    assert video.label == "DEF Label"
    assert video.series == "GHI Series"
    assert video.director == "JKL Director"
    assert video.tags == ["HD", "独家"]
    assert video.cover_url == "https://pics.javbus.com/cover.jpg"
    assert video.sample_images == [
        "https://www.javbus.com/pics/sample/1.jpg",
        "https://www.javbus.com/pics/sample/2.jpg",
    ]
    assert [actress.name for actress in video.actresses] == ["Hitomi Tanaka", "Yui Hatano"]


@patch("scrapers.openaver.censored.javbus.requests.Session")
def test_source_japanese_layout_and_title_prefix(mock_session_cls):
    mock_session_cls.return_value = MagicMock()
    scraper = JavBusScraper(lang="ja")

    video = scraper._parse(
        BeautifulSoup(JP_HTML, "html.parser"),
        "ABC-123",
        "https://www.javbus.com/ja/ABC-123",
    )

    assert video.title == "日本語タイトル"
    assert video.date == "2025-02-03"
    assert video.duration == 90
    assert video.director == "監督氏"
    assert video.maker == "メーカー"
    assert video.label == "レーベル"
    assert video.series == "シリーズ"
    assert video.tags == ["タグ"]
    assert [actress.name for actress in video.actresses] == ["出演者"]
    assert video.cover_url == "https://www.javbus.com/cover.jpg"
    assert video.sample_images == ["https://www.javbus.com/sample.jpg"]


def test_source_search_url_and_ids():
    scraper = JavBusScraper()

    assert scraper._build_search_url("ABC", 2, 1) == "https://www.javbus.com/search/ABC/2&type=1"
    assert scraper._parse_search_ids(BeautifulSoup(SEARCH_HTML, "html.parser")) == ["ABC-456", "DEF-789"]


def test_source_browser_fingerprint():
    headers = JavBusScraper()._session.headers

    assert headers["User-Agent"].startswith("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
    assert headers["Accept-Encoding"] == "gzip, deflate"
    assert headers["Connection"] == "keep-alive"


@patch("scrapers.openaver.censored.javbus.requests.Session")
def test_search_not_found_connection_and_timeout(mock_session_cls):
    from requests import ConnectionError, Timeout

    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [MagicMock(status_code=404), ConnectionError(), Timeout()]

    scraper = JavBusScraper()
    assert scraper.search("ABC-123") == []
    assert scraper.search("ABC-123") == []
    assert scraper.search("ABC-123") == []


@patch("scrapers.openaver.censored.javbus.requests.Session")
def test_search_save_webpage_fetches_images_with_referer(mock_session_cls):
    import base64

    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    main = response(SAMPLE_HTML, "https://www.javbus.com/SONE-205")
    main.headers = {"Content-Type": "text/html"}
    resource_calls = []

    def get_side_effect(url, *args, **kwargs):
        if url == "https://www.javbus.com/SONE-205":
            return main
        resource_calls.append(kwargs)
        item = MagicMock()
        item.status_code = 200
        item.url = url
        item.content = b"resource-bytes"
        item.headers = {"Content-Type": "image/jpeg"}
        return item

    mock_session.get.side_effect = get_side_effect

    video = JavBusScraper().search("SONE-205", save_webpage=True)[0]

    assert video.webpage
    assert base64.b64decode(video.webpage)
    assert all(call["headers"]["Referer"] == "https://www.javbus.com/SONE-205" for call in resource_calls)

    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    main = response(SAMPLE_HTML, "https://www.javbus.com/SONE-205")
    main.headers = {"Content-Type": "text/html"}
    cover = MagicMock(status_code=200, url="https://pics.javbus.com/cover.jpg", content=b"cover")
    sample = MagicMock(status_code=200, url="https://www.javbus.com/pics/sample/1.jpg", content=b"sample")
    mock_session.get.side_effect = [main, cover, sample]

    video = JavBusScraper().search("SONE-205", save_webpage=True)[0]

    assert video.webpage
    assert base64.b64decode(video.webpage)
    assert mock_session.get.call_args_list[-2].kwargs["headers"]["Referer"] == "https://www.javbus.com/SONE-205"
