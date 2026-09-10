# -*- coding: utf-8 -*-

from unittest.mock import patch, MagicMock

from scrapers.models import Video
from scrapers.openaver.uncensored.fc2 import FC2Scraper

from tests.fc2.fixtures import (
    MAIN_HTML_WITH_IFRAME,
    MINIMAL_ARCHIVE_HTML,
    MULTILANG_HTML,
    NO_LD_MULTILANG_HTML,
    SAMPLE_HTML,
    WIDGET_HTML,
)





def test_site_properties():
    scraper = FC2Scraper()
    assert scraper.site_id == "fc2"
    assert scraper.site_name == "FC2"


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_success(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = SAMPLE_HTML
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    result = scraper.search("FC2-PPV-1723984")

    assert len(result) == 1
    video = result[0]
    assert isinstance(video, Video)
    assert video.number == "FC2-PPV-1723984"
    assert video.title == "透け透け体操服にローライズブルマーでハミ尻。"
    assert video.date == "2021-03-12"
    assert video.duration == 61
    assert video.rating == 4.8
    assert video.tags == ["ブルマ", "電マ"]
    assert video.cover_url == "https://storage58000.contents.fc2.com/file/367/36632163/1615552591.1.jpg"
    assert video.sample_images == [
        "https://contents-thumbnail2.fc2.com/w480/storage58000.contents.fc2.com/file/367/36632163/1615552591.2.jpg",
        "https://contents-thumbnail2.fc2.com/w480/storage58000.contents.fc2.com/file/367/36632163/1615552591.43.jpg",
    ]
    assert video.summary == "透け透け体操服からスケル乳首。"
    assert video.source == "fc2"
    assert video.detail_url == "https://adult.contents.fc2.com/article/1723984/"


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_extracts_id_from_plain_digits(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = SAMPLE_HTML
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    result = scraper.search("1723984")

    mock_session.get.assert_called_once_with(
        "https://adult.contents.fc2.com/article/1723984/", timeout=15
    )
    assert len(result) == 1
    assert result[0].number == "FC2-PPV-1723984"




@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_parses_multilanguage_page_via_json_ld(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = MULTILANG_HTML
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    result = scraper.search("FC2-PPV-1723984")

    assert len(result) == 1
    video = result[0]
    assert video.title == "Some English Title"
    assert video.date == "2021-03-12"
    assert video.duration == 61
    assert video.rating == 4.5
    assert video.tags == ["ブルマ"]
    assert video.cover_url == "https://storage58000.contents.fc2.com/file/367/36632163/1615552591.1.jpg"
    assert video.summary == "Full description from JSON-LD."


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_rating_fallback_without_json_ld(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = NO_LD_MULTILANG_HTML
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    result = scraper.search("FC2-PPV-1723984")

    assert len(result) == 1
    video = result[0]
    assert video.title == "Some English Title"
    assert video.date == "2021-03-12"
    assert video.rating == 4.2

@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_fetches_full_summary_from_widget(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    main_resp = MagicMock()
    main_resp.status_code = 200
    main_resp.text = MAIN_HTML_WITH_IFRAME
    widget_resp = MagicMock()
    widget_resp.status_code = 200
    widget_resp.text = WIDGET_HTML
    mock_session.get.side_effect = [main_resp, widget_resp]

    scraper = FC2Scraper()
    result = scraper.search("FC2-PPV-1723984")

    assert len(result) == 1
    summary = result[0].summary
    assert "透け透け体操服からスケル乳首。" in summary
    assert "そんな彼女は" in summary
    assert "MC5kY2Y2NjU5Yi" not in summary
    assert "カートに入れる" not in summary


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_not_found(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 404
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-999999") == []


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_missing_title_returns_none(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = "<html><body><h2>サービス情報</h2></body></html>"
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-1723984") == []


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_connection_error(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import ConnectionError
    mock_session.get.side_effect = ConnectionError()

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-1723984") == []


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_timeout(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import Timeout
    mock_session.get.side_effect = Timeout()

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-1723984") == []


@patch("scrapers.openaver.uncensored.fc2.requests.Session")
def test_search_save_webpage_includes_main_and_widget(mock_session_cls):
    import base64
    import email
    from email.policy import default as email_policy
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session

    def response(url, content, content_type):
        item = MagicMock()
        item.status_code = 200
        item.url = url
        item.text = content.decode("utf-8")
        item.content = content
        item.headers = {"Content-Type": content_type}
        return item

    main = response("https://adult.contents.fc2.com/article/1723984/", MINIMAL_ARCHIVE_HTML.encode(), "text/html")
    widget = response("https://example.test/widget", b"<html><body>Description</body></html>", "text/html")
    cover = response("https://example.test/cover.jpg", b"cover-bytes", "image/jpeg")
    mock_session.get.side_effect = [main, widget, cover]

    scraper = FC2Scraper()
    result = scraper.search("FC2-PPV-1723984", save_webpage=True)

    assert result[0].webpage
    archive = email.message_from_bytes(base64.b64decode(result[0].webpage), policy=email_policy)
    locations = {part["Content-Location"] for part in archive.walk() if part.get("Content-Location")}
    assert "https://adult.contents.fc2.com/article/1723984/" in locations
    assert "https://example.test/widget" in locations
    assert "https://example.test/cover.jpg" in locations
