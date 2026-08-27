# -*- coding: utf-8 -*-
import pytest
from unittest.mock import patch, MagicMock

from scrapers.models import Video
from scrapers.openaver.dmm import DMMScraper


JP_HTML = """
<html><body>
<h1 id="title">STARS-804 サンプルタイトル</h1>
<a href="https://pics.dmm.co.jp/1000/cover.jpg"><img src="https://pics.dmm.co.jp/cover.jpg"/></a>
<div class="product-info">
<table>
<tr><th>発売日</th><td>2025/01/15</td></tr>
<tr><th>収録時間</th><td>120</td></tr>
<tr><th>メーカー</th><td>ABCスタジオ</td></tr>
<tr><th>レーベル</th><td>DEFレーベル</td></tr>
<tr><th>シリーズ</th><td>GHIシリーズ</td></tr>
<tr><th>監督</th><td>JKL監督</td></tr>
<tr><th>ジャンル</th><td><a href="/genre/1">ジャンルA</a></td></tr>
</table>
</div>
</body></html>
"""

EN_HTML = """
<html><body>
<h1 id="title">STARS-804 English Title</h1>
<a href="https://pics.dmm.co.jp/1000/cover.jpg"><img src="https://pics.dmm.co.jp/cover.jpg"/></a>
<div class="product-info">
<table>
<tr><th>Release Date</th><td>2025/01/15</td></tr>
<tr><th>Length</th><td>120</td></tr>
<tr><th>Maker</th><td>ABC Studio</td></tr>
<tr><th>Label</th><td>DEF Label</td></tr>
<tr><th>Series</th><td>GHI Series</td></tr>
<tr><th>Director</th><td>JKL Director</td></tr>
<tr><th>Genre</th><td><a href="/genre/1">Genre A</a></td></tr>
</table>
</div>
</body></html>
"""


@pytest.fixture(autouse=True)
def clear_registry_teardown():
    from scrapers.registry import ScraperRegistry
    yield
    ScraperRegistry.clear()


def test_site_properties():
    scraper = DMMScraper()
    assert scraper.site_id == "dmm"
    assert scraper.site_name == "DMM"


def _assert_parsed(video):
    assert isinstance(video, Video)
    assert video.number == "STARS-804"
    assert video.title == "サンプルタイトル"
    assert video.date == "2025-01-15"
    assert video.duration == 120
    assert video.maker == "ABCスタジオ"
    assert video.label == "DEFレーベル"
    assert video.series == "GHIシリーズ"
    assert video.director == "JKL監督"
    assert video.tags == ["ジャンルA"]
    assert video.cover_url == "https://pics.dmm.co.jp/cover.jpg"


@patch("scrapers.openaver.dmm.requests.Session")
def test_search_jp_labels(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    search_resp = MagicMock()
    search_resp.status_code = 200
    search_resp.text = '<a href="/mono/dvd/-/detail/=/cid=abc001/">STARS804</a>'
    detail_resp = MagicMock()
    detail_resp.status_code = 200
    detail_resp.text = JP_HTML
    mock_session.get.side_effect = [search_resp, detail_resp]

    scraper = DMMScraper()
    result = scraper.search("STARS-804")

    assert len(result) == 1
    _assert_parsed(result[0])


@patch("scrapers.openaver.dmm.requests.Session")
def test_search_en_labels(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    search_resp = MagicMock()
    search_resp.status_code = 200
    search_resp.text = '<a href="/mono/dvd/-/detail/=/cid=abc001/">STARS804</a>'
    detail_resp = MagicMock()
    detail_resp.status_code = 200
    detail_resp.text = EN_HTML
    mock_session.get.side_effect = [search_resp, detail_resp]

    scraper = DMMScraper()
    result = scraper.search("STARS-804")

    assert len(result) == 1
    video = result[0]
    assert video.title == "English Title"
    assert video.date == "2025-01-15"
    assert video.duration == 120
    assert video.maker == "ABC Studio"
    assert video.label == "DEF Label"
    assert video.series == "GHI Series"
    assert video.director == "JKL Director"
    assert video.tags == ["Genre A"]


@patch("scrapers.openaver.dmm.requests.Session")
def test_search_not_found(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = "<html><body>no results</body></html>"
    mock_session.get.return_value = mock_resp

    scraper = DMMScraper()
    assert scraper.search("ABCD-999") == []


@patch("scrapers.openaver.dmm.requests.Session")
def test_search_timeout(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import Timeout
    mock_session.get.side_effect = Timeout()

    scraper = DMMScraper()
    assert scraper.search("STARS-804") == []