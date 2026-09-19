from unittest.mock import MagicMock

from scrapers.openaver.censored.javdb import JavDBScraper


SEARCH_HTML = """
<html><body>
<div class="movie-list">
  <div class="item">
    <div class="video-title"><strong>SONE-103</strong></div>
    <a href="/v/Ww9zN8"></a>
  </div>
</div>
</body></html>
"""

DETAIL_HTML = """
<html><body>
<h2 class="title is-4">SONE-103 テストタイトル</h2>
<div class="video-cover">
  <img src="https://c0.jdbstatic.com/covers/ww/ps.jpg">
</div>
<div class="panel-block">
  <strong>日期:</strong><span class="value">2024-03-12</span>
</div>
<div class="panel-block">
  <strong>導演:</strong><span class="value">トレンディ山口</span>
</div>
<div class="panel-block">
  <strong>片商:</strong><span class="value">S1 NO.1 STYLE</span>
</div>
<div class="panel-block">
  <strong>評分:</strong><span class="value">4.46分, 由2105人評價</span>
</div>
<div class="panel-block">
  <strong>類別:</strong>
  <span class="value"><a href="/tags?c7=11">戲劇</a><a href="/tags?c4=17">巨乳</a></span>
</div>
<div class="panel-block">
  <strong>演員:</strong>
  <span class="value">
    <a href="/actors/meJay">つばさ舞</a><strong class="symbol female">♀</strong>
    <a href="/actors/O2Q30">結城結弦</a><strong class="symbol male">♂</strong>
  </span>
</div>
</body></html>
"""


def _response(html: str, url: str) -> MagicMock:
    response = MagicMock()
    response.status_code = 200
    response.text = html
    response.content = html.encode("utf-8")
    response.url = url
    response.headers = {"Content-Type": "text/html; charset=utf-8"}
    return response


def _scraper_with_responses(*responses: MagicMock) -> JavDBScraper:
    scraper = JavDBScraper()
    scraper._session = MagicMock()
    scraper._session.get.side_effect = responses
    return scraper


def test_javdb_exposes_javdb580_as_mirror():
    assert JavDBScraper.BASE_URL == "https://javdb.com"
    assert "https://javdb580.com" in JavDBScraper.MIRROR_URLS


def test_get_html_uses_curl_cffi_chrome_impersonation():
    url = "https://javdb580.com/search?q=SONE-103&f=all"
    scraper = _scraper_with_responses(_response(SEARCH_HTML, url))
    scraper.BASE_URL = "https://javdb580.com"

    assert scraper._get_html(url) == SEARCH_HTML

    kwargs = scraper._session.get.call_args.kwargs
    assert kwargs["impersonate"] == "chrome120"
    assert kwargs["timeout"] == 30


def test_search_uses_mirror_and_parses_full_fields():
    search_url = "https://javdb580.com/search?q=SONE-103&f=all&locale=zh"
    detail_url = "https://javdb580.com/v/Ww9zN8"
    scraper = _scraper_with_responses(
        _response(SEARCH_HTML, search_url),
        _response(DETAIL_HTML, detail_url),
    )
    scraper.BASE_URL = "https://javdb580.com"

    result = scraper.search("sone103")

    assert len(result) == 1
    video = result[0]
    assert video.number == "SONE-103"
    assert video.title == "テストタイトル"
    assert video.date == "2024-03-12"
    assert video.director == "トレンディ山口"
    assert video.maker == "S1 NO.1 STYLE"
    assert video.rating == 4.46
    assert video.tags == ["戲劇", "巨乳"]
    assert [actress.name for actress in video.actresses] == ["つばさ舞"]
    assert video.cover_url == "https://c0.jdbstatic.com/covers/ww/pl.jpg"
    assert video.detail_url == detail_url
    assert scraper._session.get.call_args_list[0].args[0] == search_url


def test_search_by_keyword_returns_matching_videos():
    search_url = "https://javdb.com/search?q=%E3%81%A4%E3%81%B0%E3%81%95%E8%88%9E&f=all"
    detail_url = "https://javdb.com/v/Ww9zN8"
    scraper = _scraper_with_responses(
        _response(SEARCH_HTML, search_url),
        _response(DETAIL_HTML, detail_url),
    )

    result = scraper.search_by_keyword("つばさ舞")

    assert [video.number for video in result] == ["SONE-103"]
    assert result[0].source == "javdb"
    assert scraper._session.get.call_args_list[0].args[0] == search_url


def test_search_filters_unmarked_male_when_female_class_is_present():
    detail_url = "https://javdb580.com/v/Ww9zN8"
    detail_html = """
    <html><body>
    <h2 class="title is-4">SONE-103 English title</h2>
    <div class="panel-block">
      <strong>Actor(s):</strong>
      <span class="value">
        <a class="actor-female" href="/actors/meJay">Actress A</a>,
        <a href="/actors/O2Q30">Actor B</a>
      </span>
    </div>
    </body></html>
    """
    scraper = _scraper_with_responses(
        _response(SEARCH_HTML, "https://javdb580.com/search"),
        _response(detail_html, detail_url),
    )
    scraper.BASE_URL = "https://javdb580.com"

    result = scraper.search("SONE-103")

    assert [actress.name for actress in result[0].actresses] == ["Actress A"]
