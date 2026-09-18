from unittest.mock import MagicMock

import pytest

from scrapers.registry import ScraperRegistry


@pytest.fixture(autouse=True)
def clear_domestic_registry():
    original_scrapers = ScraperRegistry._scrapers.copy()
    ScraperRegistry.clear()
    yield
    ScraperRegistry.clear()
    ScraperRegistry._scrapers.update(original_scrapers)


def mock_response(status_code=200, text="", json_data=None, url=""):
    response = MagicMock()
    response.status_code = status_code
    response.text = text
    response.json.return_value = json_data
    response.url = url
    response.headers = {"Content-Type": "text/html"}
    response.content = text.encode("utf-8")
    return response
