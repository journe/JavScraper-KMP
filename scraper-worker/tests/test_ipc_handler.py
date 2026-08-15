import pytest
from ipc_handler import handle_request
from scrapers.registry import ScraperRegistry
from scrapers.base import BaseScraper


class MockScraper(BaseScraper):
    @property
    def site_id(self):
        return "mock"

    @property
    def site_name(self):
        return "Mock Site"

    def search(self, number):
        return None


@pytest.fixture(autouse=True)
def clean_registry():
    ScraperRegistry.clear()
    yield
    ScraperRegistry.clear()


def test_unknown_method():
    result = handle_request({"id": "1", "method": "x", "params": {}})
    assert result["error"]["code"] == -32601
    assert "Unknown: x" in result["error"]["message"]


def test_no_method():
    result = handle_request({"id": "1", "params": {}})
    assert result["error"]["code"] == -32600
    assert result["error"]["message"] == "No method"


def test_list_sites_empty():
    result = handle_request({"id": "1", "method": "list_sites", "params": {}})
    assert result["result"] == []


def test_list_sites_with_registered():
    ScraperRegistry.register(MockScraper)
    result = handle_request({"id": "1", "method": "list_sites", "params": {}})
    assert result["result"] == [{"id": "mock", "name": "Mock Site"}]


def test_get_capabilities():
    ScraperRegistry.register(MockScraper)
    result = handle_request({"id": "1", "method": "get_capabilities", "params": {}})
    assert result["result"]["version"] == "0.1.0"
    assert "probe_file" in result["result"]["features"]
    assert result["result"]["sites"] == [{"id": "mock", "name": "Mock Site"}]


def test_probe_file():
    result = handle_request({
        "id": "1",
        "method": "probe_file",
        "params": {"file_path": "some/path/ABC-123.mp4"},
    })
    assert result["result"]["file_path"] == "some/path/ABC-123.mp4"
    assert result["result"]["number"] == "ABC-123"


def test_probe_file_no_match():
    result = handle_request({
        "id": "1",
        "method": "probe_file",
        "params": {"file_path": "some/path/video.mp4"},
    })
    assert result["result"]["number"] is None


def test_handler_error_propagates():
    """Calling probe_file without params should fail."""
    result = handle_request({"id": "1", "method": "probe_file", "params": {}})
    assert result["error"]["code"] == -1
    assert "file_path" in result["error"]["message"]


def test_jsonrpc_protocol():
    result = handle_request({"id": "99", "method": "list_sites", "params": {}})
    assert result["jsonrpc"] == "2.0"
    assert result["id"] == "99"
