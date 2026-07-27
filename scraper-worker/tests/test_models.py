import pytest
from scrapers.models import Actress, Video, scrape_success, scrape_error


def test_video_to_dict():
    v = Video(number="SONE-205", title="Test", actresses=[Actress("A")], rating=7.5)
    d = v.to_dict()
    assert d["actresses"] == ["A"]
    assert d["rating"] == 7.5


def test_scrape_success():
    result = scrape_success(Video(number="ABC-123"))
    assert result["success"] is True


def test_scrape_error():
    result = scrape_error("Not found", code=-2)
    assert result["error"]["code"] == -2
