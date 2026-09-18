from unittest.mock import MagicMock, patch

from scrapers.registry import ScraperRegistry
from scrapers.openaver.domestic.madouqu import MadouquScraper

from .conftest import mock_response


SEARCH_HTML = """
<html><body>
<div class="entry-media"><div><a href="/post/1">
  <img class="lazyload" alt="MDX-0016 标题">
</a></div></div>
</body></html>
"""

DETAIL_HTML = """
<html><body>
<div class="cao_entry_header"><header><h1>MDX-0016 标题</h1></header></div>
<time datetime="2024-03-04T08:00:00+08:00">2024-03-04</time>
<span class="meta-category">麻豆传媒</span>
<div class="entry-content u-text-format u-clearfix">
  <p>番号：MDX-0016</p><p>片名：标题</p><p>女郎</p><p>：沈娜娜、李娜</p>
  <p><img src="/cover.jpg"></p>
</div>
</body></html>
"""


def test_auto_register():
    if ScraperRegistry.get("madouqu") is None:
        ScraperRegistry.register(MadouquScraper)
    assert ScraperRegistry.get("madouqu") is MadouquScraper


@patch("scrapers.openaver.domestic.madouqu.requests.Session")
def test_search_success(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.side_effect = [
        mock_response(200, SEARCH_HTML, "https://madouqu.com/?s=MDX-0016"),
        mock_response(200, DETAIL_HTML, "https://madouqu.com/post/1"),
    ]

    result = MadouquScraper().search("MDX0016")

    assert len(result) == 1
    video = result[0]
    assert video.number == "MDX-0016"
    assert video.title == "标题"
    assert [actress.name for actress in video.actresses] == ["沈娜娜", "李娜"]
    assert video.date == "2024-03-04"
    assert video.maker == "麻豆传媒"
    assert video.label == "麻豆传媒"
    assert video.tags == ["国产"]
    assert video.cover_url == "https://madouqu.com/cover.jpg"
    assert video.detail_url == "https://madouqu.com/post/1"


@patch("scrapers.openaver.domestic.madouqu.requests.Session")
def test_search_not_found(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(200, "<html><body></body></html>")

    assert MadouquScraper().search("ZZZ-999") == []
