from unittest.mock import MagicMock, patch

from scrapers.registry import ScraperRegistry
from scrapers.openaver.domestic.mdtv import MdtvScraper

from .conftest import mock_response


SEARCH_HTML = """
<html><body><h4 class="post-title">
  <a title="MDX-0016 标题" href="/post/1">MDX-0016 标题</a>
</h4></body></html>
"""

DETAIL_HTML = """
<html><body>
<div class="blog-single"><div><a title="MDX-0016 标题 麻豆传媒映画">
  <img src="/upload/20240304-1.jpg"></a></div></div>
<div class="category">麻豆系列</div>
<div class="category"><a>国产</a><a>沈娜娜</a></div>
<div class="category"><a>沈娜娜</a><a>未知</a></div>
</body></html>
"""


def test_auto_register():
    if ScraperRegistry.get("mdtv") is None:
        ScraperRegistry.register(MdtvScraper)
    assert ScraperRegistry.get("mdtv") is MdtvScraper


@patch("scrapers.openaver.domestic.mdtv.requests.Session")
def test_search_success(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.post.return_value = mock_response(200, SEARCH_HTML)
    session.get.return_value = mock_response(200, DETAIL_HTML, "https://www.mdpjzip.xyz/post/1")

    result = MdtvScraper().search("MDX0016")

    assert len(result) == 1
    video = result[0]
    assert video.number == "MDX-0016"
    assert video.title == "标题"
    assert video.series == "麻豆系列"
    assert video.maker == "麻豆传媒映画"
    assert [actress.name for actress in video.actresses] == ["沈娜娜"]
    assert video.tags == ["国产"]
    assert video.date == "2024-03-04"
    assert video.cover_url == "https://www.mdpjzip.xyz/upload/20240304-1.jpg"
    assert video.detail_url == "https://www.mdpjzip.xyz/post/1"


@patch("scrapers.openaver.domestic.mdtv.requests.Session")
def test_search_not_found(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.post.return_value = mock_response(200, "没有找到匹配数据")

    assert MdtvScraper().search("ZZZ-999") == []
