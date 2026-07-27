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

    @abstractmethod
    def search(self, number: str) -> Optional[Video]:
        ...

    def normalize_number(self, number: str) -> str:
        number = number.strip().upper()
        import re
        match = re.match(r'^([A-Z]+)(\d+)$', number)
        if match:
            return f"{match.group(1)}-{match.group(2)}"
        return number
