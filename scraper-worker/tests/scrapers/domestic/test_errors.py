from unittest.mock import MagicMock, patch

import pytest
from requests.exceptions import ConnectionError, Timeout

from scrapers.openaver.domestic.cnmdb import CnmdbScraper
from scrapers.openaver.domestic.hdouban import HdoubanScraper
from scrapers.openaver.domestic.javday import JavdayScraper
from scrapers.openaver.domestic.madouqu import MadouquScraper
from scrapers.openaver.domestic.mdtv import MdtvScraper


@pytest.mark.parametrize("scraper_cls", [
    MadouquScraper,
    MdtvScraper,
    HdoubanScraper,
    CnmdbScraper,
    JavdayScraper,
])
def test_search_connection_error_returns_empty(scraper_cls):
    with patch(f"{scraper_cls.__module__}.requests.Session") as mock_session_cls:
        session = MagicMock()
        mock_session_cls.return_value = session
        session.get.side_effect = ConnectionError()
        session.post.side_effect = ConnectionError()

        assert scraper_cls().search("MDX-0016") == []


@pytest.mark.parametrize("scraper_cls", [
    MadouquScraper,
    MdtvScraper,
    HdoubanScraper,
    CnmdbScraper,
    JavdayScraper,
])
def test_search_timeout_returns_empty(scraper_cls):
    with patch(f"{scraper_cls.__module__}.requests.Session") as mock_session_cls:
        session = MagicMock()
        mock_session_cls.return_value = session
        session.get.side_effect = Timeout()
        session.post.side_effect = Timeout()

        assert scraper_cls().search("MDX-0016") == []
