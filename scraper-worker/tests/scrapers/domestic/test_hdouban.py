from unittest.mock import MagicMock, patch

from scrapers.registry import ScraperRegistry
from scrapers.openaver.domestic.hdouban import HdoubanScraper

from .conftest import mock_response


SEARCH_DATA = {
    "data": {
        "list": [
            {"id": 2202, "number": "MDX-0016", "name": "MDX-0016 标题"},
        ]
    }
}

DETAIL_DATA = {
    "data": {
        "number": "MDX-0016",
        "name": "MDX-0016 标题",
        "big_cove": "https://cdn.example/cover.jpg",
        "small_cover": "https://cdn.example/poster.jpg",
        "actors": [{"name": "沈娜娜", "sex": "♀"}, {"name": "男性演员", "sex": "♂"}],
        "labels": [{"name": "国产"}, {"name": "高清"}],
        "director": [{"name": "导演"}],
        "company": [{"name": "麻豆传媒"}],
        "series": [{"name": "麻豆系列"}],
        "release_time": "2024-03-04 00:00:00",
        "time": 5400,
        "score": 8.2,
        "map": [{"big_img": "https://cdn.example/s1.jpg"}],
    }
}


def test_auto_register():
    if ScraperRegistry.get("hdouban") is None:
        ScraperRegistry.register(HdoubanScraper)
    assert ScraperRegistry.get("hdouban") is HdoubanScraper


@patch("scrapers.openaver.domestic.hdouban.requests.Session")
def test_search_success(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(200, json_data=SEARCH_DATA)
    session.post.return_value = mock_response(200, json_data=DETAIL_DATA)

    result = HdoubanScraper().search("MDX0016")

    assert len(result) == 1
    video = result[0]
    assert video.number == "MDX-0016"
    assert video.title == "标题"
    assert [actress.name for actress in video.actresses] == ["沈娜娜"]
    assert video.date == "2024-03-04"
    assert video.duration == 90
    assert video.rating == 8.2
    assert video.maker == "麻豆传媒"
    assert video.series == "麻豆系列"
    assert video.director == "导演"
    assert video.tags == ["国产", "高清"]
    assert video.cover_url == "https://cdn.example/cover.jpg"
    assert video.poster_url == "https://cdn.example/poster.jpg"
    assert video.sample_images == ["https://cdn.example/s1.jpg"]
    assert video.detail_url == "https://ormtgu.com/moviedetail/2202"


@patch("scrapers.openaver.domestic.hdouban.requests.Session")
def test_search_not_found(mock_session_cls):
    session = MagicMock()
    mock_session_cls.return_value = session
    session.get.return_value = mock_response(200, json_data={"data": {"list": []}})

    assert HdoubanScraper().search("ZZZ-999") == []
