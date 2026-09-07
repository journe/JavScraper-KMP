"""Tests for scraper site connectivity checks."""
import time

import pytest
import requests

from scrapers.registry import ScraperRegistry
from scrapers.site_check import check_sites


class _DummyResponse:
    def __init__(self, status_code=200, url="https://example.test/"):
        self.status_code = status_code
        self.url = url


class _FakeScraper:
    BASE_URL = "https://example.test"


class _NoUrlScraper:
    pass


def _register(monkeypatch, scraper_cls, site_id="fake"):
    monkeypatch.setattr(ScraperRegistry, "_scrapers", {site_id: scraper_cls})
    monkeypatch.setattr(
        ScraperRegistry,
        "list_sites",
        classmethod(lambda cls: [{"id": site_id, "name": "Fake"}]),
    )


def _patch_get(monkeypatch, result):
    calls = {}

    def fake_get(self, url, timeout):
        calls["url"] = url
        calls["timeout"] = timeout
        if isinstance(result, Exception):
            raise result
        return result

    monkeypatch.setattr(requests.Session, "get", fake_get)
    return calls


def test_check_sites_ok(monkeypatch):
    _register(monkeypatch, _FakeScraper)
    calls = _patch_get(monkeypatch, _DummyResponse(200))
    results = check_sites()
    assert len(results) == 1
    result = results[0]
    assert result["id"] == "fake"
    assert result["ok"] is True
    assert result["status"] == 200
    assert result["latency_ms"] >= 0
    assert calls["url"] == "https://example.test"
    assert calls["timeout"] == 10


def test_check_sites_http_error(monkeypatch):
    _register(monkeypatch, _FakeScraper)
    _patch_get(monkeypatch, _DummyResponse(503))
    results = check_sites()
    assert results[0]["ok"] is False
    assert results[0]["error"] == "HTTP 503"


def test_check_sites_timeout(monkeypatch):
    _register(monkeypatch, _FakeScraper)
    _patch_get(monkeypatch, requests.Timeout("timed out"))
    results = check_sites()
    assert results[0]["ok"] is False
    assert results[0]["error"] == "timeout"


def test_check_sites_connection_error(monkeypatch):
    _register(monkeypatch, _FakeScraper)
    _patch_get(monkeypatch, requests.ConnectionError("refused"))
    results = check_sites()
    assert results[0]["ok"] is False
    assert "refused" in results[0]["error"]


def test_check_sites_no_base_url(monkeypatch):
    _register(monkeypatch, _NoUrlScraper)
    results = check_sites()
    assert results[0]["ok"] is False
    assert results[0]["error"] == "no base url"


def test_check_sites_unregistered(monkeypatch):
    monkeypatch.setattr(ScraperRegistry, "_scrapers", {})
    monkeypatch.setattr(
        ScraperRegistry,
        "list_sites",
        classmethod(lambda cls: [{"id": "ghost", "name": "Ghost"}]),
    )
    results = check_sites()
    assert results[0]["ok"] is False
    assert results[0]["error"] == "scraper not registered"


def test_check_sites_filter_by_site_ids(monkeypatch):
    monkeypatch.setattr(
        ScraperRegistry, "_scrapers", {"fake": _FakeScraper, "other": _FakeScraper}
    )
    called = []

    def fake_get(self, url, timeout):
        called.append(url)
        return _DummyResponse(200)

    monkeypatch.setattr(requests.Session, "get", fake_get)
    monkeypatch.setattr(
        ScraperRegistry,
        "list_sites",
        classmethod(
            lambda cls: [{"id": "fake", "name": "Fake"}, {"id": "other", "name": "Other"}]
        ),
    )
    results = check_sites(site_ids=["other"])
    assert len(results) == 1
    assert results[0]["id"] == "other"
    assert len(called) == 1


def test_check_sites_no_filter_returns_all(monkeypatch):
    _register(monkeypatch, _FakeScraper)
    monkeypatch.setattr(
        ScraperRegistry,
        "list_sites",
        classmethod(
            lambda cls: [{"id": "fake", "name": "Fake"}, {"id": "other", "name": "Other"}]
        ),
    )
    results = check_sites()
    assert len(results) == 2


def test_check_sites_parallel_order(monkeypatch):
    _register(monkeypatch, _FakeScraper)

    def slow_get(self, url, timeout):
        time.sleep(0.05)
        return _DummyResponse(200)

    monkeypatch.setattr(requests.Session, "get", slow_get)
    results = check_sites()
    assert len(results) == 1
    assert results[0]["ok"] is True