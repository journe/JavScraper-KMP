from typing import Optional, Type
from .base import BaseScraper


class ScraperRegistry:
    _scrapers: dict[str, Type[BaseScraper]] = {}
    _categories = {"censored", "uncensored", "mixed", "domestic"}

    @classmethod
    def register(cls, scraper_cls: Type[BaseScraper]) -> None:
        cls._scrapers[scraper_cls().site_id] = scraper_cls

    @classmethod
    def get(cls, site_id: str) -> Optional[Type[BaseScraper]]:
        return cls._scrapers.get(site_id)

    @classmethod
    def create(
        cls, site_id: str, base_url_override: str | None = None
    ) -> Optional[BaseScraper]:
        scraper_cls = cls.get(site_id)
        if scraper_cls is None:
            return None

        scraper = scraper_cls()
        override = (base_url_override or "").strip().rstrip("/")
        if override.startswith(("http://", "https://")):
            scraper.BASE_URL = override
        return scraper

    @classmethod
    def list_sites(cls) -> list[dict]:
        return [
            {
                "id": sid,
                "name": cls._scrapers[sid]().site_name,
                "category": cls._site_category(cls._scrapers[sid]),
                "base_url": getattr(cls._scrapers[sid], "BASE_URL", ""),
                "mirror_urls": list(getattr(cls._scrapers[sid], "MIRROR_URLS", [])),
            }
            for sid in sorted(cls._scrapers)
        ]
    @classmethod
    def _site_category(cls, scraper_cls: Type[BaseScraper]) -> str:
        parts = scraper_cls.__module__.split(".")
        if (
            len(parts) >= 3
            and parts[0:2] == ["scrapers", "openaver"]
            and parts[2] in cls._categories
        ):
            return parts[2]
        return "unknown"

    @classmethod
    def clear(cls) -> None:
        cls._scrapers.clear()
