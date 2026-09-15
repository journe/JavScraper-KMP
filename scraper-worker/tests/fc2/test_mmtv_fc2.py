from unittest.mock import MagicMock, patch

from scrapers.openaver.mixed.mmtv import MmtvScraper

DETAIL_URL = 'https://7mmtv.sx/zh/uncensored_content/42419/fc2-ppv-3259498.html'
SEARCH_HTML = '''
<html><body>
<figure class="video-preview topic_contain">
  <a href="https://7mmtv.sx/zh/uncensored_content/42419/fc2-ppv-3259498.html">
    <img alt="fc2-ppv 3259498 测试标题">
  </a>
</figure>
</body></html>
'''
DETAIL_HTML = '''
<html><body>
<h1 class="fullvideo-title h5 mb-2">测试标题</h1>
<div class="d-flex mb-4"><span>fc2-ppv 3259498</span><span>2024-01-02</span><span>61分</span></div>
<ol class="breadcrumb">無碼AV</ol>
</body></html>
'''


def _response(text, url):
    response = MagicMock()
    response.status_code = 200
    response.text = text
    response.url = url
    response.content = text.encode('utf-8')
    response.headers = {'Content-Type': 'text/html'}
    return response


@patch('scrapers.openaver.mixed.mmtv.requests.Session')
def test_mmtv_search_matches_plain_digit_fc2_candidate_with_extra_class(mock_session_cls):
    mock_session = MagicMock()
    mock_session_cls.return_value = mock_session
    mock_session.get.side_effect = [
        _response(SEARCH_HTML, 'https://7mmtv.sx/zh/searchform_search/all/index.html'),
        _response(DETAIL_HTML, DETAIL_URL),
    ]

    results = MmtvScraper().search('3259498')

    assert [video.detail_url for video in results] == [DETAIL_URL]


def test_mmtv_parse_normalizes_lowercase_fc2_number():
    video = MmtvScraper()._parse(DETAIL_HTML, DETAIL_URL)

    assert video is not None
    assert video.number == 'FC2-3259498'
    assert video.tags == ['无码']