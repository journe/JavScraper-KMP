from typing import Optional, Type
from .base import BaseScraper


class ScraperRegistry:
    _scrapers: dict[str, Type[BaseScraper]] = {}

    @classmethod
    def register(cls, scraper_cls: Type[BaseScraper]) -> None:
        cls._scrapers[scraper_cls().site_id] = scraper_cls

    @classmethod
    def get(cls, site_id: str) -> Optional[Type[BaseScraper]]:
        return cls._scrapers.get(site_id)

    @classmethod
    def list_sites(cls) -> list[dict]:
        return [
            {"id": sid, "name": cls._scrapers[sid]().site_name}
            for sid in sorted(cls._scrapers)
        ]

    @classmethod
    def clear(cls) -> None:
        cls._scrapers.clear()
