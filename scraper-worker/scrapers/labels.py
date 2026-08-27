"""Shared multilingual label keywords for scraper field parsing."""

MAKER_LABELS = ("メーカー", "Maker", "片商", "製作", "制作商")
DATE_LABELS = ("発売日", "配信開始日", "Release Date", "發售日", "发行日期", "日期", "發行日期")
DURATION_LABELS = ("収録時間", "出演時間", "Length", "片長", "片长")
LABEL_LABELS = ("レーベル", "Label")
SERIES_LABELS = ("シリーズ", "Series", "系列")
RATING_LABELS = ("平均評価", "Rating", "評分", "评分")
DIRECTOR_LABELS = ("監督", "Director", "導演", "导演")


def label_matches(text: str, keywords: tuple[str, ...]) -> bool:
    return any(k in text for k in keywords)