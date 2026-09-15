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

    def _search_one(self, number):
        return None


class AnotherScraper(BaseScraper):
    @property
    def site_id(self):
        return "another"

    @property
    def site_name(self):
        return "Another Site"

    def _search_one(self, number):
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
    assert sites[0]["category"] == "unknown"
    assert sites[1]["id"] == "mock"
    assert sites[1]["name"] == "Mock"
    assert sites[1]["category"] == "unknown"


@pytest.mark.parametrize(
    ("module", "category"),
    [
        ("scrapers.openaver.censored.mock", "censored"),
        ("scrapers.openaver.uncensored.mock", "uncensored"),
        ("scrapers.openaver.mixed.mock", "mixed"),
    ],
)
def test_list_sites_category_follows_openaver_package(monkeypatch, module, category):
    monkeypatch.setattr(MockScraper, "__module__", module)
    ScraperRegistry.register(MockScraper)

    assert ScraperRegistry.list_sites() == [
        {"id": "mock", "name": "Mock", "category": category}
    ]


def test_list_sites_empty():
    assert ScraperRegistry.list_sites() == []


def test_clear():
    ScraperRegistry.register(MockScraper)
    assert ScraperRegistry.get("mock") is MockScraper
    ScraperRegistry.clear()
    assert ScraperRegistry.get("mock") is None
