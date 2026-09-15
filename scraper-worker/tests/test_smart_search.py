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

class ArchivingScraper(BaseScraper):
    last_save_webpage = None

    @property
    def site_id(self):
        return "fc2"

    @property
    def site_name(self):
        return "FC2"

    def _search_one(self, number):
        return None

    def search(self, number, save_webpage=False):
        ArchivingScraper.last_save_webpage = save_webpage
        return [Video(number=number, title="Archive", source="fc2", webpage="archive")]


def test_smart_search_passes_webpage_option_to_scraper():
    ScraperRegistry.clear()
    ScraperRegistry.register(ArchivingScraper)

    result = smart_search("FC2-PPV-1723984", site="fc2", save_webpage=True)

    assert result is not None
    assert result.webpage


class MirrorScraper(BaseScraper):
    @property
    def site_id(self):
        return "fc2mirror"

    @property
    def site_name(self):
        return "FC2 Mirror"

    def _search_one(self, number):
        return None

    def search(self, number):
        return [Video(number=number, title="Mirror", source="fc2mirror")]


def test_smart_search_includes_enabled_fc2_mirror():
    ScraperRegistry.clear()
    ScraperRegistry.register(MirrorScraper)

    results = search_candidates("FC2-PPV-1723984", enabled_sites=["fc2mirror"])

    assert [video.source for video in results] == ["fc2mirror"]

class MirrorAwareScraper(BaseScraper):
    searched_base_urls: list[str] = []

    BASE_URL = "https://example.test"

    @property
    def site_id(self):
        return "mirror-aware"

    @property
    def site_name(self):
        return "Mirror Aware"

    def _search_one(self, number):
        return None

    def search(self, number):
        MirrorAwareScraper.searched_base_urls.append(self.BASE_URL)
        return [Video(number=number, title="Mirrored", source="mirror-aware")]


def test_smart_search_applies_site_mirror_to_scraper_instance():
    ScraperRegistry.clear()
    ScraperRegistry.register(MirrorAwareScraper)

    result = smart_search(
        "SONE-001",
        site="mirror-aware",
        site_mirrors={"mirror-aware": "https://mirror.example"},
    )

    assert result is not None
    assert MirrorAwareScraper.searched_base_urls == ["https://mirror.example"]