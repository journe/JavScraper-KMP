from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry
from core.smart_search import search_candidates, smart_search


class FirstScraper(BaseScraper):
    @property
    def site_id(self):
        return "javbus"

    @property
    def site_name(self):
        return "JavBus"

    def _search_one(self, number):
        return None

    def search(self, number):
        return [
            Video(number=number, title="First", source="javbus"),
            Video(number=number, title="Second", source="javbus"),
        ]


class SecondScraper(BaseScraper):
    @property
    def site_id(self):
        return "javdb"

    @property
    def site_name(self):
        return "JavDB"

    def _search_one(self, number):
        raise AssertionError("disabled site must not be searched")

    def search(self, number):
        raise AssertionError("disabled site must not be searched")("disabled site must not be searched")


def test_smart_search_keeps_first_result_contract():
    ScraperRegistry.clear()
    ScraperRegistry.register(FirstScraper)
    ScraperRegistry.register(SecondScraper)

    result = smart_search("SONE-001", site=None, enabled_sites=["javbus"])

    assert result == Video(number="SONE-001", title="First", source="javbus")


def test_search_candidates_returns_all_enabled_results():
    ScraperRegistry.clear()
    ScraperRegistry.register(FirstScraper)
    ScraperRegistry.register(SecondScraper)

    results = search_candidates("SONE-001", site=None, enabled_sites=["javbus"])

    assert [video.title for video in results] == ["First", "Second"]