# -*- coding: utf-8 -*-
import pytest
from unittest.mock import patch, MagicMock

from scrapers.models import Video
from scrapers.openaver.fc2 import FC2Scraper


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


@patch("scrapers.openaver.fc2.requests.Session")
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


@patch("scrapers.openaver.fc2.requests.Session")
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


@patch("scrapers.openaver.fc2.requests.Session")
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


@patch("scrapers.openaver.fc2.requests.Session")
def test_search_not_found(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 404
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-999999") == []


@patch("scrapers.openaver.fc2.requests.Session")
def test_search_missing_title_returns_none(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_resp = MagicMock()
    mock_resp.status_code = 200
    mock_resp.text = "<html><body><h2>サービス情報</h2></body></html>"
    mock_session.get.return_value = mock_resp

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-1723984") == []


@patch("scrapers.openaver.fc2.requests.Session")
def test_search_connection_error(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import ConnectionError
    mock_session.get.side_effect = ConnectionError()

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-1723984") == []


@patch("scrapers.openaver.fc2.requests.Session")
def test_search_timeout(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    from requests.exceptions import Timeout
    mock_session.get.side_effect = Timeout()

    scraper = FC2Scraper()
    assert scraper.search("FC2-PPV-1723984") == []