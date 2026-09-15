"""Integration tests for the full JSON-RPC handler pipeline."""

import pytest
from ipc_handler import handle_request


def test_get_capabilities():
    """The get_capabilities endpoint reports 9+ registered sites."""
    result = handle_request({"id": "1", "method": "get_capabilities", "params": {}})
    sites = result["result"]["sites"]
    assert len(sites) >= 9
    assert "scrape" in result["result"]["features"]
    assert "search" in result["result"]["features"]


def test_list_sites():
    """list_sites includes javbus and follows openaver package categories."""
    result = handle_request({"id": "1", "method": "list_sites", "params": {}})
    categories = {site["id"]: site["category"] for site in result["result"]}
    assert categories["javbus"] == "censored"
    assert categories["fc2"] == "uncensored"
    assert categories["mmtv"] == "mixed"
    javbus = next(site for site in result["result"] if site["id"] == "javbus")
    assert javbus["mirror_urls"] == [
        "https://www.dmmsee.casa",
        "https://www.buscdn.casa",
        "https://www.seedmm.casa",
    ]


def test_probe_file():
    """probe_file extracts the JAV number from a file path."""
    result = handle_request({
        "id": "1",
        "method": "probe_file",
        "params": {"file_path": "some/path/ABC-123.mp4"},
    })
    assert result["result"]["number"] == "ABC-123"
    assert result["result"]["file_path"] == "some/path/ABC-123.mp4"


def test_scrape_no_number():
    """scrape with a file that has no extractable number returns error -10."""
    result = handle_request({
        "id": "1",
        "method": "scrape",
        "params": {"file_path": "random_video.mp4"},
    })
    assert result["result"]["success"] is False
    assert result["result"]["error"]["code"] == -10
    assert "Cannot extract number" in result["result"]["error"]["message"]
