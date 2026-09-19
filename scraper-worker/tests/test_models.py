import pytest
from scrapers.models import Actress, Ranking, Review, Video, scrape_success, scrape_error


def test_video_to_dict():
    v = Video(
        number="SONE-205",
        title="Test",
        actresses=[Actress("A")],
        rating=7.5,
        rankings=[Ranking(rank=212, list_name="JavDB 2022年度TOP250")],
        reviews=[Review(id="1", author="author", content="短评")],
    )
    d = v.to_dict()
    assert d["actresses"] == ["A"]
    assert d["rating"] == 7.5
    assert d["rankings"] == [{"rank": 212, "list_name": "JavDB 2022年度TOP250"}]
    assert d["reviews"][0]["content"] == "短评"


def test_scrape_success():
    result = scrape_success(Video(number="ABC-123"))
    assert result["success"] is True


def test_scrape_error():
    result = scrape_error("Not found", code=-2)
    assert result["error"]["code"] == -2
