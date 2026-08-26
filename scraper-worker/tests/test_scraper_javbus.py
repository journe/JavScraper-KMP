import pytest
from unittest.mock import patch, MagicMock
from scrapers.models import Video
from scrapers.openaver.javbus import JavBusScraper


SAMPLE_HTML = """
<html><body>
<div class="container">
<h3>SONE-205 Sample Title</h3>
<div class="row">
<div class="col-md-3 info">
<p>发行日期: 2025-01-15</p>
<p>长度: 120 分钟</p>
<p>
<a href="/genre/1">HD</a>
<a href="/genre/2">独家</a>
<a href="/other">Not Tag</a>
</p>
</div>
<div class="col-md-9">
<div class="header">
<a href="/studio/abc">ABC Studio</a>
<a href="/label/def">DEF Label</a>
<a href="/series/ghi">GHI Series</a>
<a href="/director/jkl">JKL Director</a>
</div>
<a class="bigImage" href="https://pics.javbus.com/cover.jpg">
<img src="https://pics.javbus.com/cover_s.jpg">
</a>
<div id="waterfall">
<a class="avatar-box" title="Hitomi Tanaka" href="/star/1">
<img src="https://pics.javbus.com/actress1.jpg">
</a>
<a class="avatar-box" title="Yui Hatano" href="/star/2">
<img src="https://pics.javbus.com/actress2.jpg">
</a>
</div>
</div>
</div>
</div>
</body></html>
"""


@pytest.fixture(autouse=True)
def clear_registry_teardown():
    from scrapers.registry import ScraperRegistry
    yield
    ScraperRegistry.clear()


def test_auto_register():
    from scrapers.registry import ScraperRegistry
    # Re-register in case prior tests cleared the registry
    if ScraperRegistry.get("javbus") is None:
        ScraperRegistry.register(JavBusScraper)
    cls = ScraperRegistry.get("javbus")
    assert cls is not None
    assert cls is JavBusScraper


def test_site_properties():
    scraper = JavBusScraper()
    assert scraper.site_id == "javbus"
    assert scraper.site_name == "JavBus"


@patch("scrapers.openaver.javbus.requests.Session")
def test_search_success(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = SAMPLE_HTML
    mock_resp.url = "https://www.javbus.com/SONE-205"
    mock_session.get.return_value = mock_resp

    scraper = JavBusScraper()
    result = scraper.search("sone205")

    assert len(result) == 1
    result = result[0]
    assert isinstance(result, Video)
    assert result.number == "SONE-205"
    assert result.title == "SONE-205 Sample Title"
    assert result.date == "2025-01-15"
    assert result.duration == 120
    assert result.maker == "ABC Studio"
    assert result.label == "DEF Label"
    assert result.series == "GHI Series"
    assert result.director == "JKL Director"
    assert result.tags == ["HD", "独家"]
    assert result.cover_url == "https://pics.javbus.com/cover.jpg"
    assert result.source == "javbus"
    assert result.detail_url == "https://www.javbus.com/SONE-205"
    assert result.actresses is not None
    assert len(result.actresses) == 2
    assert result.actresses[0].name == "Hitomi Tanaka"
    assert result.actresses[1].name == "Yui Hatano"


@patch("scrapers.openaver.javbus.requests.Session")
def test_search_not_found(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    mock_resp = MagicMock()
    mock_resp.status_code = 404
    mock_session.get.return_value = mock_resp

    scraper = JavBusScraper()
    result = scraper.search("ABCD-999")
    assert result == []


@patch("scrapers.openaver.javbus.requests.Session")
def test_search_connection_error(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    from requests.exceptions import ConnectionError
    mock_session.get.side_effect = ConnectionError()

    scraper = JavBusScraper()
    result = scraper.search("SONE-205")
    assert result == []


@patch("scrapers.openaver.javbus.requests.Session")
def test_search_timeout(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    from requests.exceptions import Timeout
    mock_session.get.side_effect = Timeout()

    scraper = JavBusScraper()
    result = scraper.search("SONE-205")
    assert result == []


@patch("scrapers.openaver.javbus.requests.Session")
def test_search_no_info_block(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = "<html><body><p>No info block</p></body></html>"
    mock_session.get.return_value = mock_resp

    scraper = JavBusScraper()
    result = scraper.search("SONE-205")
    assert result == []


@patch("scrapers.openaver.javbus.requests.Session")
def test_search_normalizes_number(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = SAMPLE_HTML
    mock_resp.url = "https://www.javbus.com/SONE-205"
    mock_session.get.return_value = mock_resp

    scraper = JavBusScraper()
    result = scraper.search("SONE205")

    mock_session.get.assert_called_once_with("https://www.javbus.com/SONE-205", timeout=15)
    assert len(result) == 1
    result = result[0]
    assert result.number == "SONE-205"


def test_normalize_number():
    scraper = JavBusScraper()
    assert scraper.normalize_number("SONE205") == "SONE-205"
    assert scraper.normalize_number("sone205") == "SONE-205"
    assert scraper.normalize_number("abc-123") == "ABC-123"
    assert scraper.normalize_number("ABC-123") == "ABC-123"
