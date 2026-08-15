import pytest
from scrapers.registry import ScraperRegistry
from scrapers.base import BaseScraper


class MockScraper(BaseScraper):
    @property
    def site_id(self):
        return "mock"

    @property
    def site_name(self):
        return "Mock"

    def search(self, number):
        return None


class AnotherScraper(BaseScraper):
    @property
    def site_id(self):
        return "another"

    @property
    def site_name(self):
        return "Another Site"

    def search(self, number):
        return None


@pytest.fixture(autouse=True)
def clear_registry():
    ScraperRegistry.clear()
    yield
    ScraperRegistry.clear()


def test_register():
    ScraperRegistry.register(MockScraper)
    assert ScraperRegistry.get("mock") is MockScraper


def test_get_unknown_returns_none():
    assert ScraperRegistry.get("nonexistent") is None


def test_register_multiple():
    ScraperRegistry.register(MockScraper)
    ScraperRegistry.register(AnotherScraper)
    assert ScraperRegistry.get("mock") is MockScraper
    assert ScraperRegistry.get("another") is AnotherScraper


def test_list_sites():
    ScraperRegistry.register(MockScraper)
    ScraperRegistry.register(AnotherScraper)
    sites = ScraperRegistry.list_sites()
    assert len(sites) == 2
    assert sites[0]["id"] == "another"
    assert sites[0]["name"] == "Another Site"
    assert sites[1]["id"] == "mock"
    assert sites[1]["name"] == "Mock"


def test_list_sites_empty():
    assert ScraperRegistry.list_sites() == []


def test_clear():
    ScraperRegistry.register(MockScraper)
    assert ScraperRegistry.get("mock") is MockScraper
    ScraperRegistry.clear()
    assert ScraperRegistry.get("mock") is None
