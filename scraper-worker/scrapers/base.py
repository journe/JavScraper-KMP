from abc import ABC, abstractmethod
from typing import Optional
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

    def search(self, number: str) -> list[Video]:
        result = self._search_one(number)
        return [result] if result is not None else []

    @abstractmethod
    def _search_one(self, number: str) -> Optional[Video]:
        ...

    def normalize_number(self, number: str) -> str:
        number = number.strip().upper()
        import re
        match = re.match(r'^([A-Z]+)(\d+)$', number)
        if match:
            return f"{match.group(1)}-{match.group(2)}"
        return number