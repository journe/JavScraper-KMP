import email
import mimetypes
import re
from dataclasses import dataclass
from email import encoders
from email.generator import BytesGenerator
from email.mime.application import MIMEApplication
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.parser import BytesParser
from email.policy import default
from io import BytesIO
from pathlib import Path
from typing import Callable, Iterable
from .scrape_errors import ScrapeStageError
from urllib.parse import urljoin, urlsplit, urlunsplit

from bs4 import BeautifulSoup
from bs4.element import Tag

CSS_URL_PATTERN = re.compile(r"url\(\s*(['\"]?)(.*?)\1\s*\)", re.IGNORECASE)


@dataclass(frozen=True)
class CapturedResponse:
    url: str
    content: bytes
    content_type: str


def normalize_url(url: str, base: str = "") -> str:
    value = url.strip()
    if not value or value.startswith(("data:", "javascript:", "mailto:", "tel:", "#")):
        return ""
    if value.startswith("//"):
        value = "https:" + value
    if base:
        value = urljoin(base, value)
    split = urlsplit(value)
    if split.scheme not in ("http", "https") or not split.netloc:
        return ""
    return urlunsplit((split.scheme.lower(), split.netloc, split.path, split.query, ""))


LINK_RESOURCE_RELS = frozenset(
    {
        "stylesheet",
        "icon",
        "apple-touch-icon",
        "apple-touch-icon-precomposed",
        "mask-icon",
        "manifest",
        "preload",
        "modulepreload",
    }
)


def _is_resource_link(element: Tag) -> bool:
    rel = element.get("rel") or []
    tokens = rel.split() if isinstance(rel, str) else [str(item) for item in rel]
    return bool({token.lower() for token in tokens} & LINK_RESOURCE_RELS)


def _resource_urls(content: bytes, base_url: str) -> list[str]:
    urls: list[str] = []
    soup = BeautifulSoup(content, "html.parser")
    elements = soup.find_all(["img", "script", "link", "source", "iframe", "video"])
    for element in elements:
        if element.name == "link" and not _is_resource_link(element):
            continue
        for attribute in ("src", "data-src", "data-original", "href", "poster"):
            value = element.get(attribute)
            if value:
                urls.append(value)
        srcset = element.get("srcset")
        if srcset:
            urls.extend(item.strip().split(" ")[0] for item in srcset.split(",") if item.strip())
    for meta in soup.find_all("meta"):
        content_attribute = meta.get("content")
        if content_attribute and str(content_attribute).startswith(("http://", "https://", "//")):
            urls.append(str(content_attribute))
    css_urls = CSS_URL_PATTERN.findall(content.decode("utf-8", errors="ignore"))
    urls.extend(match[1] for match in css_urls)
    return [normalize_url(url, base_url) for url in urls]


def _content_type(url: str, captured_type: str) -> tuple[str, str]:
    content_type = captured_type.split(";", 1)[0].strip().lower() or "application/octet-stream"
    if content_type == "application/octet-stream":
        guessed, _ = mimetypes.guess_type(url)
        if guessed:
            content_type = guessed
    if "/" not in content_type:
        return "application", "octet-stream"
    maintype, subtype = content_type.split("/", 1)
    return maintype.strip(), subtype.strip()


def _attachment(captured: CapturedResponse) -> MIMEApplication:
    maintype, subtype = _content_type(captured.url, captured.content_type)
    part = MIMEApplication(captured.content, _subtype=subtype)
    part.replace_header("Content-Type", f"{maintype}/{subtype}")
    encoders.encode_base64(part)
    part.add_header("Content-Location", captured.url)
    part.add_header("Content-Disposition", "inline")
    return part


def build_mhtml(
    root: CapturedResponse,
    related: list[CapturedResponse],
    fetch: Callable[[str], CapturedResponse | None],
    extra_urls: Iterable[str] = (),
) -> bytes:
    root_url = normalize_url(root.url)
    if not root_url:
        raise ValueError("Invalid root URL")

    known: dict[str, CapturedResponse] = {}
    related_map: dict[str, CapturedResponse] = {}
    related_resources: list[CapturedResponse] = []
    documents: list[tuple[str, bytes, str]] = [(root_url, root.content, root.content_type)]
    for item in related:
        item_url = normalize_url(item.url)
        if not item_url or item_url in known:
            continue
        related_map[item_url] = item
        related_resources.append(item)
        documents.append((item_url, item.content, item.content_type))

    processed_urls = {root_url, *known}
    queue: list[str] = []
    for _, content, _ in documents:
        queue.extend(_resource_urls(content, root_url))
    queue.extend(url for url in (normalize_url(url, root_url) for url in extra_urls) if url)

    while queue:
        url = queue.pop(0)
        if not url or url in processed_urls:
            continue
        processed_urls.add(url)
        try:
            captured = fetch(url)
        except ScrapeStageError:
            raise
        except Exception:
            captured = None
        if captured is None:
            continue
        normalized = normalize_url(captured.url or url, root_url)
        captured = CapturedResponse(normalized, captured.content, captured.content_type)
        known[normalized] = captured
        if normalized != url:
            processed_urls.add(normalized)
        if "css" in captured.content_type.lower() or url.endswith(".css"):
            queue.extend(_resource_urls(captured.content, normalized))

    archive = MIMEMultipart("related", type="text/html")
    archive["Subject"] = "Saved webpage"
    archive["Date"] = email.utils.formatdate(localtime=False, usegmt=True)
    archive["From"] = "JavScraper"
    archive["To"] = "JavScraper"
    archive["Snapshot-Content-Location"] = root_url

    root_part = MIMEText(root.content.decode("utf-8", errors="replace"), "html", "utf-8")
    del root_part["MIME-Version"]
    root_part.add_header("Content-Location", root_url)
    root_part.add_header("Content-Disposition", "inline")
    archive.attach(root_part)
    for captured in related_resources:
        archive.attach(_attachment(captured))
    for url, captured in known.items():
        if captured.content:
            archive.attach(_attachment(captured))

    buffer = BytesIO()
    BytesGenerator(buffer, mangle_from_=False, maxheaderlen=0).flatten(archive)
    return buffer.getvalue()


def _mhtml_resources(mhtml_path: str) -> tuple[dict[str, bytes], str]:
    message = BytesParser(policy=default).parsebytes(Path(mhtml_path).read_bytes())
    resources: dict[str, bytes] = {}
    root_url = normalize_url(str(message.get("Snapshot-Content-Location", "")))
    for part in message.walk():
        location = part.get("Content-Location")
        if not location:
            continue
        normalized = normalize_url(str(location))
        if not normalized:
            continue
        resources[normalized] = part.get_payload(decode=True) or b""
        if (
            not root_url
            and part.get_content_type() == "text/html"
            and part.get("Content-Disposition") == "inline"
        ):
            root_url = normalized
    return resources, root_url

def _write_resource(content: bytes, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(content)


def extract_images(
    mhtml_path: str,
    output_dir: str,
    cover_url: str = "",
    poster_url: str = "",
    sample_images: Iterable[str] = (),
    write_poster: bool = True,
) -> dict:
    output = Path(output_dir)
    errors: list[str] = []
    saved: dict[str, str | list[str]] = {}
    try:
        resources, root_url = _mhtml_resources(mhtml_path)
        cover_norm = normalize_url(cover_url, root_url)
        poster_norm = normalize_url(poster_url, root_url)
        # 海报拥有独立地址时单独下载；否则复用封面（fanart）内容复制为 poster。
        # write_poster=False 时不产生任何 poster.jpg（保护已存在的用户编辑版）。
        poster_independent = bool(poster_norm) and poster_norm != cover_norm

        requests = [
            ("fanart", cover_url, output / "fanart.jpg"),
        ]
        if poster_independent and write_poster:
            requests.append(("poster", poster_url, output / "poster.jpg"))
        for index, sample_url in enumerate(sample_images, start=1):
            requests.append((f"extrafanart{index}", sample_url, output / "extrafanart" / f"fanart{index}.jpg"))

        for key, url, target in requests:
            normalized = normalize_url(url, root_url)
            if not normalized:
                continue
            content = resources.get(normalized)
            if content is None:
                errors.append(f"{key}: {url} not found in MHTML")
                continue
            _write_resource(content, target)
            if key == "fanart" and not poster_independent and write_poster:
                # 封面（cover）写为 fanart 后，复制同一份内容为 poster（不做二次下载）。
                poster_target = output / "poster.jpg"
                _write_resource(content, poster_target)
                saved["poster"] = str(poster_target)
            if key.startswith("extrafanart"):
                values = saved.setdefault("extrafanart", [])
                if isinstance(values, list):
                    values.append(str(target))
            else:
                saved[key] = str(target)
    except Exception as exc:
        errors.append(str(exc))

    return {"success": not errors, "message": "; ".join(errors), "saved": saved}
