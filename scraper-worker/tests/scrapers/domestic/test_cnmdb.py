from unittest.mock import MagicMock, patch

from scrapers.registry import ScraperRegistry
from scrapers.openaver.domestic.cnmdb import CnmdbScraper

from .conftest import mock_response


DETAIL_HTML = """
<html><body>
<ol class="breadcrumb"><li>首页</li><li>麻豆传媒</li><li>麻豆系列</li><li>MDX-0016.标题.沈娜娜</li></ol>
<div class="post-image-inner"><img src="https://cdn.example/cover.jpg"></div>
</body></html>
"""


def test_auto_register():
    if ScraperRegistry.get("cnmdb") is None:
        ScraperRegistry.register(CnmdbScraper)
    assert ScraperRegistry.get("cnmdb") is CnmdbScraper


@patch("scrapers.openaver.domestic.cnmdb.requests.Session")
def test_search_success(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(
        200, DETAIL_HTML, "https://cnmdb.net/MDX-0016"
    )

    result = CnmdbScraper().search("MDX0016")

    assert len(result) == 1
    video = result[0]
    assert video.number == "MDX-0016"
    assert video.title == "标题"
    assert [actress.name for actress in video.actresses] == ["沈娜娜"]
    assert video.series == "麻豆系列"
    assert video.maker == "麻豆传媒"
    assert video.tags == ["国产"]
    assert video.cover_url == "https://cdn.example/cover.jpg"
    assert video.detail_url == "https://cnmdb.net/MDX-0016"


@patch("scrapers.openaver.domestic.cnmdb.requests.Session")
def test_search_not_found(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(404, "<html><body></body></html>")

    assert CnmdbScraper().search("ZZZ-999") == []
