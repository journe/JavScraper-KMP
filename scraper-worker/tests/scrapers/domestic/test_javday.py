from unittest.mock import MagicMock, patch

from scrapers.registry import ScraperRegistry
from scrapers.openaver.domestic.javday import JavdayScraper

from .conftest import mock_response


DETAIL_HTML = """
<html><head><meta content="/cover.jpg"></head><body>
<div id="videoInfo"><div><h1>MDX-0016 标题</h1>
<div><div>
  <p><span>标签</span><span><a>国产</a><a>沈娜娜</a></span></p>
  <p><span>发行商</span><span>麻豆传媒</span></p>
  <p><span>系列</span><span><a>麻豆系列</a></span></p>
</div></div></div></div>
</body></html>
"""


def test_auto_register():
    if ScraperRegistry.get("javday") is None:
        ScraperRegistry.register(JavdayScraper)
    assert ScraperRegistry.get("javday") is JavdayScraper


@patch("scrapers.openaver.domestic.javday.requests.Session")
def test_search_success(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(
        200, DETAIL_HTML, "https://javday.tv/videos/MDX-0016/"
    )

    result = JavdayScraper().search("MDX0016")

    assert len(result) == 1
    video = result[0]
    assert video.number == "MDX-0016"
    assert video.title == "标题"
    assert video.series == "麻豆系列"
    assert [actress.name for actress in video.actresses] == ["沈娜娜"]
    assert video.tags == ["国产"]
    assert video.maker == "麻豆传媒"
    assert video.cover_url == "https://javday.tv/cover.jpg"
    assert video.detail_url == "https://javday.tv/videos/MDX-0016/"


@patch("scrapers.openaver.domestic.javday.requests.Session")
def test_search_not_found(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(200, "你似乎來到了沒有視頻存在的荒原")

    assert JavdayScraper().search("ZZZ-999") == []
