import base64
from abc import ABC, abstractmethod
from typing import Optional

import requests

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
        if not save_webpage:
            result = self._search_one(number)
            return [result] if result is not None else []

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
            result = self._search_one(number)
            if result is not None:
                self._save_webpage(result)
            return [result] if result is not None else []
        finally:
            if session is not None and original_get is not None:
                if had_instance_get:
                    session.get = original_get
                else:
                    session.__dict__.pop("get", None)

    @abstractmethod
    def _search_one(self, number: str) -> Optional[Video]:
        ...

    def _save_webpage(self, video: Video) -> None:
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
        related = [
            item
            for item in responses[root_index + 1:]
            if "html" in item.content_type.lower() and normalize_url(item.url) != detail_url
        ]
        extra_urls = [video.cover_url, video.poster_url, *video.sample_images]
        try:
            archive = build_mhtml(root, related, self._fetch_resource, extra_urls)
            video.webpage = base64.b64encode(archive).decode("ascii")
        except (requests.RequestException, OSError, ValueError):
            return

    def _fetch_resource(self, url: str) -> CapturedResponse | None:
        response = self._session.get(url, timeout=15)
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