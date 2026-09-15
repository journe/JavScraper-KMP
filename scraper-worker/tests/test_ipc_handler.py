import json
import pytest
from ipc_handler import handle_request
from scrapers.registry import ScraperRegistry
from scrapers.base import BaseScraper
from scrapers.models import Video


class MockScraper(BaseScraper):
    @property
    def site_id(self):
        return "mock"

    @property
    def site_name(self):
        return "Mock Site"

    def _search_one(self, number):
        return None

    def search(self, number):
        return []


class SecondSearchScraper(MockScraper):
    def search(self, number):
        return [
            Video(number=number, title="First", source="mock"),
            Video(number=number, title="Second", source="mock"),
        ]

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
    assert result["result"] == [
        {"id": "mock", "name": "Mock Site", "category": "unknown"}
    ]


def test_get_capabilities():
    ScraperRegistry.register(MockScraper)
    result = handle_request({"id": "1", "method": "get_capabilities", "params": {}})
    assert result["result"]["version"] == "0.1.0"
    assert "probe_file" in result["result"]["features"]
    assert result["result"]["sites"] == [
        {"id": "mock", "name": "Mock Site", "category": "unknown"}
    ]


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



def test_search_returns_all_candidates():
    ScraperRegistry.register(MockScraper)
    ScraperRegistry.register(SecondSearchScraper)

    result = handle_request({
        "id": "search-1",
        "method": "search",
        "params": {"number": "ABC-123", "sites": ["mock"]},
    })

    assert result["result"] == [
        {"number": "ABC-123", "title": "First", "actresses": [], "date": "", "maker": "", "label": "", "series": "", "director": "", "duration": None, "rating": None, "tags": [], "cover_url": "", "poster_url": "", "sample_images": [], "summary": "", "source": "mock", "detail_url": ""},
        {"number": "ABC-123", "title": "Second", "actresses": [], "date": "", "maker": "", "label": "", "series": "", "director": "", "duration": None, "rating": None, "tags": [], "cover_url": "", "poster_url": "", "sample_images": [], "summary": "", "source": "mock", "detail_url": ""},
    ]


def test_search_with_explicit_site():
    ScraperRegistry.register(SecondSearchScraper)

    result = handle_request({
        "id": "search-site",
        "method": "search",
        "params": {"number": "ABC-123", "site": "mock"},
    })

    assert [video["title"] for video in result["result"]] == ["First", "Second"]
def test_jsonrpc_protocol():
    result = handle_request({"id": "99", "method": "list_sites", "params": {}})
    assert result["jsonrpc"] == "2.0"
    assert result["id"] == "99"


class OptionScraper(MockScraper):
    last_save_webpage = None

    def search(self, number, save_webpage=False):
        OptionScraper.last_save_webpage = save_webpage
        return [Video(number=number, title="Saved", source="mock")]


def test_scrape_passes_save_webpage_option():
    scraper_cls = type("ConfiguredOptionScraper", (OptionScraper,), {})
    ScraperRegistry.register(scraper_cls)
    result = handle_request({
        "id": "scrape-webpage",
        "method": "scrape",
        "params": {"number": "ABC-123", "site": "mock", "save_webpage": True},
    })
    assert result["result"]["success"] is True
    assert OptionScraper.last_save_webpage is True


def test_search_omits_webpage_when_disabled():
    ScraperRegistry.register(SecondSearchScraper)
    result = handle_request({
        "id": "search-webpage",
        "method": "search",
        "params": {"number": "ABC-123", "site": "mock", "save_webpage": False},
    })
    assert all("webpage" not in video for video in result["result"])


def test_extract_webpage_images():
    from unittest.mock import patch

    with patch("ipc_handler.extract_images") as extract:
        extract.return_value = {"success": True, "message": "", "saved": {}}
        result = handle_request({
            "id": "extract-1",
            "method": "extract_webpage_images",
            "params": {
                "mhtml_path": "C:/tmp/page.mhtml",
                "output_dir": "C:/tmp/output",
                "cover_url": "https://example.test/a.jpg",
                "poster_url": "https://example.test/b.jpg",
                "sample_images": ["https://example.test/c.jpg"],
            },
        })
    assert result["result"]["success"] is True
    extract.assert_called_once_with(
        "C:/tmp/page.mhtml",
        "C:/tmp/output",
        cover_url="https://example.test/a.jpg",
        poster_url="https://example.test/b.jpg",
        sample_images=["https://example.test/c.jpg"],
    )

def test_search_webpage_archive_timeout_returns_stage_error():
    from core.scrape_errors import WebpageArchiveTimeoutError

    class ArchiveTimeoutScraper(MockScraper):
        def search(self, number, save_webpage=False):
            raise WebpageArchiveTimeoutError(
                "Saving webpage timed out",
                site_id="mock",
                number=number,
                detail_url="https://example.test/video.html",
            )

    ScraperRegistry.register(ArchiveTimeoutScraper)
    result = handle_request({
        "id": "stage-timeout",
        "method": "search",
        "params": {"number": "ABC-123", "site": "mock", "save_webpage": True},
    })

    assert result["error"]["code"] == -31
    assert result["error"]["message"] == "Saving webpage timed out"
    assert result["error"]["data"]["stage"] == "webpage_archive"
    assert result["error"]["data"]["site_id"] == "mock"
    assert result["error"]["data"]["detail_url"] == "https://example.test/video.html"


def test_search_emits_request_and_archive_progress_notifications(capsys):
    class ProgressScraper(BaseScraper):
        @property
        def site_id(self):
            return "mock"

        @property
        def site_name(self):
            return "Mock Site"

        def _search_one(self, number):
            return None

        def _search_all(self, number):
            return [Video(number=number, title="Result", source="mock")]

    ScraperRegistry.register(ProgressScraper)
    result = handle_request({
        "id": "progress-1",
        "method": "search",
        "params": {"number": "ABC-123", "site": "mock", "save_webpage": True},
    })

    assert result["result"][0]["title"] == "Result"
    notifications = [
        json.loads(line)
        for line in capsys.readouterr().out.splitlines()
        if line.strip()
    ]
    progress = [item["params"] for item in notifications if item.get("method") == "scrape.progress"]
    assert [(item["stage"], item["status"]) for item in progress] == [
        ("webpage_request", "start"),
        ("webpage_request", "success"),
        ("webpage_archive", "start"),
        ("webpage_archive", "success"),
    ]
    assert all(item["request_id"] == "progress-1" for item in progress)
