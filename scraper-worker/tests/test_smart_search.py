from scrapers.base import BaseScraper
from scrapers.models import Video
from scrapers.registry import ScraperRegistry
from core.smart_search import smart_search


class FirstScraper(BaseScraper):
    @property
    def site_id(self):
        return "javbus"

    @property
    def site_name(self):
        return "JavBus"

    def search(self, number):
        return Video(number=number)


class SecondScraper(BaseScraper):
    @property
    def site_id(self):
        return "javdb"

    @property
    def site_name(self):
        return "JavDB"

    def search(self, number):
        raise AssertionError("disabled site must not be searched")


def test_smart_search_respects_enabled_sites():
    ScraperRegistry.clear()
    ScraperRegistry.register(FirstScraper)
    ScraperRegistry.register(SecondScraper)

    result = smart_search("SONE-001", site=None, enabled_sites=["javbus"])

    assert result is not None
    assert result.source == ""
