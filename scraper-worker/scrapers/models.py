from dataclasses import dataclass, field, asdict
from typing import Optional


@dataclass
class Actress:
    name: str


@dataclass
class Ranking:
    rank: int
    list_name: str


@dataclass
class Review:
    id: str = ""
    author: str = ""
    date: str = ""
    score: Optional[float] = None
    likes: Optional[int] = None
    content: str = ""


@dataclass
class Video:
    number: str
    title: str = ""
    actresses: list[Actress] = field(default_factory=list)
    date: str = ""
    maker: str = ""
    label: str = ""
    series: str = ""
    director: str = ""
    duration: Optional[int] = None
    rating: Optional[float] = None
    want_count: Optional[int] = None
    watched_count: Optional[int] = None
    rating_count: Optional[int] = None
    tags: list[str] = field(default_factory=list)
    rankings: list[Ranking] = field(default_factory=list)
    reviews: list[Review] = field(default_factory=list)
    cover_url: str = ""
    poster_url: str = ""
    sample_images: list[str] = field(default_factory=list)
    summary: str = ""
    source: str = ""
    detail_url: str = ""
    webpage: str = ""

    def to_dict(self) -> dict:
        result = asdict(self)
        result.pop("webpage", None)
        result["actresses"] = [a.name for a in self.actresses]
        if self.webpage:
            result["webpage"] = self.webpage
        return result


def scrape_success(video: Video) -> dict:
    return {"success": True, "data": video.to_dict()}


def scrape_error(message: str, code: int = -1) -> dict:
    return {"success": False, "error": {"code": code, "message": message}}
