import pytest
from scrapers.base import BaseScraper


class Concrete(BaseScraper):
    @property
    def site_id(self):
        return "test"

    @property
    def site_name(self):
        return "Test"

    def search(self, n):
        return None


def test_concrete():
    s = Concrete()
    assert s.site_id == "test"


def test_normalize():
    s = Concrete()
    assert s.normalize_number("sone205") == "SONE-205"
