import base64
from abc import ABC, abstractmethod
from typing import Optional

import requests

from core.progress import emit_scrape_progress
from core.scrape_errors import (
    ScrapeStageError,
    WebpageArchiveError,
    WebpageArchiveTimeoutError,
    WebpageRequestTimeoutError,
)
from core.webpage_archive import CapturedResponse, build_mhtml, normalize_url
from .models import Video


class BaseScraper(ABC):
    @property
    @abstractmethod
    def site_id(self) -> str:
        ...

    @property
    @abstractmethod
    def site_name(self) -> str:
        ...

    def search(self, number: str, save_webpage: bool = False) -> list[Video]:
        context = {"site_id": self.site_id, "number": number}
        self._captured_responses = []
        session = getattr(self, "_session", None)
        original_get = getattr(session, "get", None) if session is not None else None
        had_instance_get = session is not None and "get" in session.__dict__

        def recorded_get(url, *args, **kwargs):
            response = original_get(url, *args, **kwargs)
            content_type = str(response.headers.get("Content-Type", ""))
            self._captured_responses.append(
                CapturedResponse(
                    url=response.url or url,
                    content=response.content,
                    content_type=content_type,
                )
            )
            return response

        try:
            if session is not None and original_get is not None:
                session.get = recorded_get
            emit_scrape_progress("webpage_request", "start", **context)
            try:
                results = self._search_all(number)
            except requests.Timeout as error:
                message = f"Webpage request timed out: {error.__class__.__name__}"
                emit_scrape_progress("webpage_request", "timeout", message=message, **context)
                raise WebpageRequestTimeoutError(message, **context) from error
            emit_scrape_progress("webpage_request", "success", **context)

            if not save_webpage or not results:
                return results

            emit_scrape_progress("webpage_archive", "start", **context)
            try:
                self._save_webpages(results)
            except ScrapeStageError as error:
                status = "timeout" if isinstance(error, WebpageArchiveTimeoutError) else "error"
                emit_scrape_progress(
                    "webpage_archive",
                    status,
                    message=error.message,
                    site_id=error.site_id or self.site_id,
                    number=error.number or number,
                    detail_url=error.detail_url,
                )
                raise
            except (requests.RequestException, OSError, ValueError) as error:
                message = f"Saving webpage failed: {error.__class__.__name__}"
                emit_scrape_progress("webpage_archive", "error", message=message, **context)
                raise WebpageArchiveError(message, **context) from error
            emit_scrape_progress("webpage_archive", "success", **context)
            return results
        finally:
            if session is not None and original_get is not None:
                if had_instance_get:
                    session.get = original_get
                else:
                    session.__dict__.pop("get", None)

    def _search_all(self, number: str) -> list[Video]:
        result = self._search_one(number)
        return [result] if result is not None else []

    @abstractmethod
    def _search_one(self, number: str) -> Optional[Video]:
        ...

    def _save_webpages(self, videos: list[Video]) -> None:
        detail_urls = {normalize_url(video.detail_url) for video in videos if video.detail_url}
        for video in videos:
            self._save_webpage(video, detail_urls)

    def _save_webpage(self, video: Video, detail_urls: set[str]) -> None:
        detail_url = normalize_url(video.detail_url)
        if not detail_url or not hasattr(self, "_session"):
            return
        responses = self._captured_responses
        root_index = next(
            (index for index, item in enumerate(responses) if normalize_url(item.url) == detail_url),
            None,
        )
        if root_index is None:
            return
        root = responses[root_index]
        next_root_index = next(
            (
                index for index in range(root_index + 1, len(responses))
                if normalize_url(responses[index].url) in detail_urls
            ),
            len(responses),
        )
        related = [
            item
            for item in responses[root_index + 1:next_root_index]
            if "html" in item.content_type.lower() and normalize_url(item.url) != detail_url
        ]
        extra_urls = [video.cover_url, video.poster_url, *video.sample_images]
        try:
            def fetch_resource(url: str):
                return self._fetch_resource(url, detail_url)

            archive = build_mhtml(root, related, fetch_resource, extra_urls)
            video.webpage = base64.b64encode(archive).decode("ascii")
        except WebpageArchiveTimeoutError as error:
            raise WebpageArchiveTimeoutError(
                error.message,
                site_id=self.site_id,
                number=video.number,
                detail_url=detail_url,
            ) from error
        except (requests.RequestException, OSError, ValueError) as error:
            raise WebpageArchiveError(
                f"Saving webpage failed: {error.__class__.__name__}",
                site_id=self.site_id,
                number=video.number,
                detail_url=detail_url,
            ) from error

    def _fetch_resource(self, url: str, referer: str = "") -> CapturedResponse | None:
        headers = {"Referer": referer} if referer else {}
        try:
            response = self._session.get(url, timeout=15, headers=headers)
        except requests.Timeout as error:
            message = f"Webpage resource timed out: {url}"
            emit_scrape_progress(
                "webpage_archive",
                "timeout",
                site_id=self.site_id,
                detail_url=referer,
                message=message,
            )
            raise WebpageArchiveTimeoutError(
                message, site_id=self.site_id, detail_url=referer
            ) from error
        if response.status_code != 200:
            return None
        return CapturedResponse(
            url=response.url or url,
            content=response.content,
            content_type=str(response.headers.get("Content-Type", "")),
        )

    def normalize_number(self, number: str) -> str:
        number = number.strip().upper()
        import re
        match = re.match(r'^([A-Z]+)(\d+)$', number)
        if match:
            return f"{match.group(1)}-{match.group(2)}"
        return number
