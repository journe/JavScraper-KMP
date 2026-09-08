# -*- coding: utf-8 -*-
import json

import pytest
from unittest.mock import patch, MagicMock

from scrapers.models import Video
from scrapers.openaver.uncensored.fc2 import FC2Scraper


SAMPLE_HTML = """
<html><head>
<meta property="og:title" content="FC2-PPV-1723984 透け透け体操服にローライズブルマーでハミ尻。"/>
<meta property="og:image" content="https://storage58000.contents.fc2.com/file/367/36632163/1615552591.1.jpg"/>
<meta property="og:description" content="og fallback summary"/>
</head><body>
<section class="items_article_wrapper">
  <section class="items_article_header">
    <section class="items_article_headerTitleInArea">
      <div class="items_article_headerInfo">
        <div class="items_article_softDevice"><p>販売日 : 2021/03/12</p></div>
        <p>商品ID : FC2 PPV 1723984</p>
      </div>
      <div class="items_article_MainitemThumb">
        <span><p class="items_article_info">01:01:02</p></span>
      </div>
      <section class="items_article_TagArea">
        <h3>商品タグ</h3>
        <div data-feed-container="">
          <a class="tag tagTag" data-tag="ブルマ" href="/search/?tag=%E3%83%96%E3%83%AB%E3%83%9E">ブルマ</a>
          <a class="tag tagTag" data-tag="電マ" href="/search/?tag=%E9%9B%BB%E3%83%9E">電マ</a>
        </div>
      </section>
    </section>
  </section>
  <section class="items_article_SampleImages">
    <h3>サンプル画像</h3>
    <ul class="items_article_SampleImagesArea">
      <li><a><img src="//contents-thumbnail2.fc2.com/w480/storage58000.contents.fc2.com/file/367/36632163/1615552591.2.jpg"/></a></li>
      <li><a><img src="//contents-thumbnail2.fc2.com/w480/storage58000.contents.fc2.com/file/367/36632163/1615552591.43.jpg"/></a></li>
    </ul>
  </section>
  <section class="items_article_Contents">
    <h3>商品説明</h3>
    <div>透け透け体操服からスケル乳首。</div>
  </section>
  <section class="items_article_Review">
    <section class="items_article_reviewComp"><section>平均評価 4.8</section></section>
  </section>
</section>
</body></html>
"""


@pytest.fixture(autouse=True)
def clear_registry_teardown():
    from scrapers.registry import ScraperRegistry
    yield
    ScraperRegistry.clear()


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



MAIN_HTML_WITH_IFRAME = SAMPLE_HTML.replace(
    '<div>透け透け体操服からスケル乳首。</div>',
    '<div><iframe data-iframe="description" src="/widget/article/1723984/description?ac=demo"></iframe></div>',
)

LD_PRODUCT = {
    "@type": "Product",
    "name": "Some English Title",
    "image": {"url": "http://storage58000.contents.fc2.com/file/367/36632163/1615552591.1.jpg"},
    "aggregateRating": {"ratingValue": 4.5, "bestRating": 5, "worstRating": 1, "reviewCount": 12},
    "description": "Full description from JSON-LD.",
}

MULTILANG_HTML = f"""
<html><head>
<meta property="og:title" content="FC2-PPV-1723984 Some English Title"/>
<script type="application/ld+json">{json.dumps(LD_PRODUCT)}</script>
</head><body>
<section class="items_article_wrapper">
  <section class="items_article_header">
    <section class="items_article_headerTitleInArea">
      <div class="items_article_headerInfo">
        <div class="items_article_softDevice"><p>Release Date : 2021/03/12</p></div>
        <p>Product ID : FC2 PPV 1723984</p>
      </div>
      <div class="items_article_MainitemThumb">
        <span><p class="items_article_info">01:01:02</p></span>
      </div>
      <section class="items_article_TagArea">
        <h3>Tags</h3>
        <a class="tag tagTag" data-tag="ブルマ" href="/search/?tag=x">ブルマ</a>
      </section>
    </section>
  </section>
</section>
</body></html>
"""


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

NO_LD_MULTILANG_HTML = """
<html><head>
<meta property="og:title" content="FC2-PPV-1723984 Some English Title"/>
</head><body>
<section class="items_article_wrapper">
  <section class="items_article_header">
    <section class="items_article_headerTitleInArea">
      <div class="items_article_headerInfo">
        <div class="items_article_softDevice"><p>Release Date : 2021/03/12</p></div>
        <p>Product ID : FC2 PPV 1723984</p>
      </div>
      <div class="items_article_MainitemThumb">
        <span><p class="items_article_info">01:01:02</p></span>
      </div>
      <section class="items_article_Review">
        <section class="items_article_reviewComp">
          <section>Average Rating: 4.2</section>
        </section>
      </section>
    </section>
  </section>
</section>
</body></html>
"""


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
WIDGET_HTML = """
<html><body>
<div>
透け透け体操服からスケル乳首。<br/>
<span>MC5kY2Y2NjU5Yi4xYTRlNTAuNmE5MDI0ZGU=</span><br/>
そんな彼女は、私、あんまらない可愛くないけど。<br/>
<div>パンファイ① 極上の美脚、至極のパンスト！ 800 pt カートに入れる</div>
</div>
</body></html>
"""


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

MINIMAL_ARCHIVE_HTML = """
<html><head><meta property="og:title" content="FC2-PPV-1723984 Archive">
<meta property="og:image" content="https://example.test/cover.jpg"></head>
<body><section class="items_article_Contents">
<iframe data-iframe="description" src="https://example.test/widget"></iframe>
</section></body></html>
"""


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