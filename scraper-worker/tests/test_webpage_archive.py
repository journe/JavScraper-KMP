import email
import re
from pathlib import Path
from urllib.parse import quote

from core.webpage_archive import CapturedResponse, build_mhtml, extract_images

ROOT_URL = "https://example.test/movie/FC2-PPV-1723984"
CSS_URL = "https://example.test/assets/page.css"
IMAGE_URL = "https://example.test/images/cover.jpg"
BACKGROUND_URL = "https://example.test/images/background.jpg"
WIDGET_URL = "https://example.test/widget/description"
WIDGET_IMAGE_URL = "https://example.test/images/widget.jpg"
POSTER_URL = "https://example.test/images/poster.jpg"
SAMPLE_URL = "https://example.test/images/sample.jpg"

ROOT_HTML = f'''
<!doctype html><html><head>
<link rel="stylesheet" href="/assets/page.css">
</head><body><img src="{IMAGE_URL}"><iframe src="{WIDGET_URL}"></iframe></body></html>
'''.encode()
CSS_BYTES = f"body{{background:url({quote(BACKGROUND_URL, safe='/:')}) no-repeat}}".encode()
IMAGE_BYTES = b"image-bytes"
BACKGROUND_BYTES = b"background-bytes"
WIDGET_BYTES = b"<html><body><img src=/images/widget.jpg></body></html>"
WIDGET_IMAGE_BYTES = b"widget-bytes"
POSTER_BYTES = b"poster-bytes"
SAMPLE_BYTES = b"sample-bytes"


def response_for(url, content=b"", content_type="text/html"):
    return CapturedResponse(url=url, content=content, content_type=content_type)


def test_build_mhtml_fetches_and_embeds_each_resource_once():
    resources = {
        CSS_URL: response_for(CSS_URL, CSS_BYTES, "text/css"),
        IMAGE_URL: response_for(IMAGE_URL, IMAGE_BYTES, "image/jpeg"),
        BACKGROUND_URL: response_for(BACKGROUND_URL, BACKGROUND_BYTES, "image/jpeg"),
        WIDGET_IMAGE_URL: response_for(WIDGET_IMAGE_URL, WIDGET_IMAGE_BYTES, "image/jpeg"),
    }
    fetch_counts = {url: 0 for url in resources}

    def fetch(url):
        fetch_counts[url] += 1
        return resources[url]

    archive = build_mhtml(
        root=response_for(ROOT_URL, ROOT_HTML),
        related=[response_for(WIDGET_URL, WIDGET_BYTES, "text/html")],
        fetch=fetch,
    )

    message = email.message_from_bytes(archive)
    assert message.get_content_type() == "multipart/related"
    locations = {
        part["Content-Location"]: part.get_payload(decode=True)
        for part in message.walk()
        if part.get("Content-Location")
    }
    assert locations[ROOT_URL] == ROOT_HTML
    assert locations[WIDGET_URL] == WIDGET_BYTES
    assert locations[CSS_URL] == CSS_BYTES
    assert locations[IMAGE_URL] == IMAGE_BYTES
    assert locations[BACKGROUND_URL] == BACKGROUND_BYTES
    assert locations[WIDGET_IMAGE_URL] == WIDGET_IMAGE_BYTES
    assert set(fetch_counts.values()) == {1}
    widget_count = sum(
        1
        for part in message.walk()
        if part.get("Content-Location") == WIDGET_URL
    )
    assert widget_count == 1


def test_extract_images_writes_expected_files(tmp_path: Path):
    archive = build_mhtml(
        root=response_for(ROOT_URL, ROOT_HTML),
        related=[],
        fetch=lambda url: response_for(
            url,
            {
                IMAGE_URL: IMAGE_BYTES,
                POSTER_URL: POSTER_BYTES,
                SAMPLE_URL: SAMPLE_BYTES,
            }[url],
            "image/jpeg",
        ),
        extra_urls=[IMAGE_URL, POSTER_URL, SAMPLE_URL],
    )
    mhtml_path = tmp_path / "page.mhtml"
    mhtml_path.write_bytes(archive)
    output_dir = tmp_path / "output"

    result = extract_images(
        str(mhtml_path),
        str(output_dir),
        cover_url=IMAGE_URL,
        poster_url=POSTER_URL,
        sample_images=[SAMPLE_URL],
    )

    assert result["success"] is True
    assert result["saved"] == {
        "poster": str(output_dir / "poster.jpg"),
        "fanart": str(output_dir / "fanart.jpg"),
        "extrafanart": [str(output_dir / "extrafanart" / "fanart1.jpg")],
    }
    assert (output_dir / "poster.jpg").read_bytes() == IMAGE_BYTES
    assert (output_dir / "fanart.jpg").read_bytes() == POSTER_BYTES
    assert (output_dir / "extrafanart" / "fanart1.jpg").read_bytes() == SAMPLE_BYTES


def test_extract_images_reports_missing_requested_images(tmp_path: Path):
    archive = build_mhtml(
        root=response_for(ROOT_URL, b"<html></html>"),
        related=[],
        fetch=lambda url: None,
        extra_urls=[POSTER_URL],
    )
    mhtml_path = tmp_path / "page.mhtml"
    mhtml_path.write_bytes(archive)

    result = extract_images(str(mhtml_path), str(tmp_path / "output"), poster_url=POSTER_URL)

    assert result["success"] is False
    assert re.search(r"fanart:.*not found", result["message"])


def test_extract_images_resolves_relative_urls_from_mhtml_root(tmp_path: Path):
    archive = build_mhtml(
        root=response_for(ROOT_URL, b"<html><body><img src=/images/cover.jpg></body></html>"),
        related=[],
        fetch=lambda url: response_for(url, b"relative-image", "image/jpeg"),
        extra_urls=["/images/sample.jpg"],
    )
    mhtml_path = tmp_path / "relative-page.mhtml"
    mhtml_path.write_bytes(archive)
    output_dir = tmp_path / "relative-output"

    result = extract_images(
        str(mhtml_path),
        str(output_dir),
        cover_url="/images/cover.jpg",
        sample_images=["/images/sample.jpg"],
    )

    assert result["success"] is True, result
    assert (output_dir / "poster.jpg").read_bytes() == b"relative-image"
    assert (output_dir / "extrafanart" / "fanart1.jpg").read_bytes() == b"relative-image"
