from unittest.mock import MagicMock, patch

from scrapers.models import Video
from scrapers.openaver.uncensored.fc2_mirror import FC2MirrorScraper

SEARCH_HTML = """<html><body><a href="https://javten.com/article/id1723984/">FC2</a></body></html>"""
DETAIL_HTML = """<html><head><meta charset="utf-8">
<script type="application/ld+json">{"aggregateRating":{"ratingValue":"4.5"}}</script>
</head><body>
<h1>FC2-PPV-1723984</h1><h1>テストタイトル</h1>
<div class="col-8">テスト賣家</div>
<a data-fancybox="gallery" href="//pics.example.com/cover.jpg"><img src="//pics.example.com/thumb.jpg"></a>
<div style="padding: 0">
<a href="//pics.example.com/gallery/001.jpg"><img src="//pics.example.com/gallery/001s.jpg"></a>
<a href="//pics.example.com/gallery/002.jpg"><img src="//pics.example.com/gallery/002s.jpg"></a>
</div>
<p class="card-text"><a href="/tag/amateur">アマチュア</a></p>
<div class="col des">これはFC2の説明文です。</div>
</body></html>"""


def response(html: str, url: str, content_type: str = "text/html", status_code: int = 200) -> MagicMock:
    item = MagicMock()
    item.status_code = status_code
    item.url = url
    item.text = html
    item.content = html.encode("utf-8") if isinstance(html, str) else html
    item.headers = {"Content-Type": content_type}
    return item


def test_site_properties_and_number_normalization():
    scraper = FC2MirrorScraper()

    assert scraper.site_id == "fc2mirror"
    assert scraper.site_name == "FC2 Mirror"
    assert scraper.BASE_URL == "https://javten.com"
    assert scraper._normalize_fc2_number("fc2-ppv-1723984") == "1723984"
    assert scraper._normalize_fc2_number("FC2PPV-1723984") == "1723984"
    assert scraper._normalize_fc2_number("1723984") == "1723984"


@patch("scrapers.openaver.uncensored.fc2_mirror.requests.Session")
def test_search_uses_javten_and_parses_full_fields(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        response(SEARCH_HTML, "https://javten.com/search?kw=1723984"),
        response(DETAIL_HTML, "https://javten.com/article/id1723984/"),
    ]

    result = FC2MirrorScraper().search("FC2-PPV-1723984")

    assert mock_session.get.call_args_list[0].args[0] == "https://javten.com/search?kw=1723984"
    assert mock_session.get.call_args_list[1].args[0] == "https://javten.com/article/id1723984/"
    assert len(result) == 1
    video = result[0]
    assert isinstance(video, Video)
    assert video.number == "FC2-1723984"
    assert video.title == "テストタイトル"
    assert video.maker == "テスト賣家"
    assert [actress.name for actress in video.actresses] == ["テスト賣家"]
    assert video.cover_url == "https://pics.example.com/cover.jpg"
    assert video.sample_images == [
        "https://pics.example.com/gallery/001.jpg",
        "https://pics.example.com/gallery/002.jpg",
    ]
    assert video.tags == ["アマチュア"]
    assert video.summary == "これはFC2の説明文です。"
    assert video.rating == 4.5
    assert video.detail_url == "https://javten.com/article/id1723984/"


@patch("scrapers.openaver.uncensored.fc2_mirror.requests.Session")
def test_search_not_found_connection_and_timeout(mock_session_cls):
    from requests import ConnectionError, Timeout

    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        response("", "https://javten.com/search", status_code=404),
        ConnectionError(),
        Timeout(),
    ]

    scraper = FC2MirrorScraper()
    assert scraper.search("FC2-PPV-999999") == []
    assert scraper.search("FC2-PPV-1723984") == []
    assert scraper.search("FC2-PPV-1723984") == []


@patch("scrapers.openaver.uncensored.fc2_mirror.requests.Session")
def test_search_save_webpage_includes_main_and_cover(mock_session_cls):
    import base64
    import email
    from email.policy import default as email_policy

    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        response(SEARCH_HTML, "https://javten.com/search?kw=1723984"),
        response(DETAIL_HTML, "https://javten.com/article/id1723984/"),
        response(b"cover-bytes", "https://pics.example.com/cover.jpg", "image/jpeg"),
    ]

    result = FC2MirrorScraper().search("FC2-PPV-1723984", save_webpage=True)

    assert result[0].webpage
    archive = email.message_from_bytes(base64.b64decode(result[0].webpage), policy=email_policy)
    locations = {part["Content-Location"] for part in archive.walk() if part.get("Content-Location")}
    assert "https://javten.com/article/id1723984/" in locations
    assert "https://pics.example.com/cover.jpg" in locations
