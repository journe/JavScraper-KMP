# -*- coding: utf-8 -*-
import pytest
from unittest.mock import patch, MagicMock

from scrapers.models import Video
from scrapers.openaver.jav321 import Jav321Scraper


JP_HTML = """
<html><body>
<h3>STARS-804 サンプルタイトル</h3>
<div class="col-md-3"><img src="https://example.com/cover.jpg"/></div>
<div class="col-md-9">
<b>メーカー</b> <a href="/studio/1">ABCスタジオ</a><br/>
<b>発売日</b> 2025-01-15<br/>
<b>出演時間</b> 120<br/>
<b>シリーズ</b> <a href="/series/1">DEFシリーズ</a><br/>
<b>平均評価</b> 4.5<br/>
<b>監督</b> <a href="/director/1">JKL監督</a><br/>
<a href="/star/1">女優A</a>
<a href="/genre/1">ジャンルA</a>
</div>
<div class="panel-body"><div class="row"><div class="col-md-12">あらすじ</div></div></div>
</body></html>
"""

EN_HTML = """
<html><body>
<h3>STARS-804 English Title</h3>
<div class="col-md-3"><img src="https://example.com/cover.jpg"/></div>
<div class="col-md-9">
<b>Maker</b> <a href="/studio/1">ABC Studio</a><br/>
<b>Release Date</b> 2025-01-15<br/>
<b>Length</b> 120<br/>
<b>Series</b> <a href="/series/1">DEF Series</a><br/>
<b>Rating</b> 4.5<br/>
<b>Director</b> <a href="/director/1">JKL Director</a><br/>
<a href="/star/1">Actress A</a>
<a href="/genre/1">Genre A</a>
</div>
<div class="panel-body"><div class="row"><div class="col-md-12">Synopsis</div></div></div>
</body></html>
"""


@pytest.fixture(autouse=True)
def clear_registry_teardown():
    from scrapers.registry import ScraperRegistry
    yield
    ScraperRegistry.clear()


def test_site_properties():
    scraper = Jav321Scraper()
    assert scraper.site_id == "jav321"
    assert scraper.site_name == "Jav321"


def _assert_parsed(video):
    assert isinstance(video, Video)
    assert video.number == "STARS-804"
    assert video.title == "サンプルタイトル"
    assert video.date == "2025-01-15"
    assert video.duration == 120
    assert video.rating == 4.5
    assert video.maker == "ABCスタジオ"
    assert video.series == "DEFシリーズ"
    assert video.director == "JKL監督"
    assert video.tags == ["ジャンルA"]
    assert video.actresses[0].name == "女優A"
    assert video.summary == "あらすじ"
    assert video.detail_url == "https://www.jav321.com/dn/stars-804"


@patch("scrapers.openaver.jav321.requests.Session")
def test_search_jp_labels(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = JP_HTML
    mock_session.get.return_value = mock_resp

    scraper = Jav321Scraper()
    result = scraper.search("STARS-804")

    mock_session.get.assert_called_once_with(
        "https://www.jav321.com/dn/STARS-804", timeout=15
    )
    assert len(result) == 1
    _assert_parsed(result[0])


@patch("scrapers.openaver.jav321.requests.Session")
def test_search_en_labels(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = EN_HTML
    mock_session.get.return_value = mock_resp

    scraper = Jav321Scraper()
    result = scraper.search("STARS-804")

    assert len(result) == 1
    video = result[0]
    assert video.title == "English Title"
    assert video.date == "2025-01-15"
    assert video.duration == 120
    assert video.rating == 4.5
    assert video.maker == "ABC Studio"
    assert video.series == "DEF Series"
    assert video.director == "JKL Director"
    assert video.tags == ["Genre A"]
    assert video.actresses[0].name == "Actress A"
    assert video.summary == "Synopsis"


@patch("scrapers.openaver.jav321.requests.Session")
def test_search_not_found(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 404
    mock_session.get.return_value = mock_resp

    scraper = Jav321Scraper()
    assert scraper.search("ABCD-999") == []


@patch("scrapers.openaver.jav321.requests.Session")
def test_search_timeout(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import Timeout
    mock_session.get.side_effect = Timeout()

    scraper = Jav321Scraper()
    assert scraper.search("STARS-804") == []