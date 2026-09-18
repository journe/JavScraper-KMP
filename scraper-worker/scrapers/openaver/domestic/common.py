import re
from urllib.parse import urljoin

from scrapers.models import Actress


DOMESTIC_STUDIOS = tuple(sorted({
    "麻豆传媒映画", "麻豆传媒", "91茄子", "Ed Mosaic", "HongKongDoll", "JVID",
    "MINI传媒", "SA国际传媒", "TWAV", "乌鸦传媒", "乐播传媒", "优蜜传媒",
    "偶蜜国际", "叮叮映画", "哔哩传媒", "大象传媒", "天美传媒", "开心鬼传媒",
    "微密圈", "扣扣传媒", "抖阴传媒", "星空无限传媒", "映秀传媒", "杏吧传媒",
    "果冻传媒", "模密传媒", "爱污传媒", "爱神传媒", "爱豆传媒", "狂点映像",
    "猛料原创", "猫爪影像", "皇家华人", "精东影业", "糖心VLOG", "维秘传媒",
    "草莓视频", "萝莉社", "蜜桃影像传媒", "蜜桃传媒", "西瓜影视", "起点传媒",
    "香蕉视频", "PsychoPorn色控", "大番号番啪啪", "REAL野性派", "豚豚创媒",
    "宫美娱乐", "肉肉传媒", "爱妃传媒", "91制片厂", "O-STAR", "兔子先生",
    "杏吧原创", "杏吧独家", "辣椒原创", "红斯灯影像", "绝对领域", "麻麻传媒",
    "渡边传媒", "AV帝王", "桃花源", "蝌蚪传媒", "SWAG", "麻豆", "杏吧",
    "糖心", "涩会",
}, key=len, reverse=True))


def normalize_domestic_number(number: str) -> str:
    value = re.sub(r"\s+", "", number.strip().upper().replace("_", "-"))
    match = re.match(r"^(91[A-Z]{2,})-?(\d{3,})$", value)
    if match:
        return f"{match.group(1)}-{match.group(2)}"
    match = re.match(r"^([A-Z]{2,})(\d{3,})(?:-(\d+))?$", value)
    if match:
        suffix = f"-{match.group(3)}" if match.group(3) else ""
        return f"{match.group(1)}-{match.group(2)}{suffix}"
    return value


def compact_number(number: str) -> str:
    return re.sub(r"[^A-Z0-9]", "", number.upper())


def unique_tags(values, extra: str = "国产") -> list[str]:
    tags: list[str] = []
    for value in [*values, extra]:
        text = str(value or "").strip()
        if text and text not in tags:
            tags.append(text)
    return tags


def clean_names(values) -> list[Actress]:
    if isinstance(values, str):
        values = [values]
    names: list[str] = []
    for value in values:
        for name in re.split(r"[,，、/]", str(value or "")):
            name = name.strip()
            if name and name not in {"未知", "N/A", "国产", "國產"} and name not in names:
                names.append(name)
    return [Actress(name=name) for name in names]


def clean_domestic_title(title: str, *remove_values: str) -> str:
    result = re.sub(r"\s+", " ", title or "").strip()
    for value in remove_values:
        text = str(value or "").strip()
        if text:
            result = result.replace(text, " ")
    result = re.sub(r"\b(?:AV|EP)\d+\b", " ", result, flags=re.IGNORECASE)
    return re.sub(r"\s+", " ", result).strip(" .-_\t")


def find_domestic_studio(*values: str) -> str:
    text = " ".join(str(value or "") for value in values)
    upper = text.upper()
    for studio in DOMESTIC_STUDIOS:
        if studio.upper() in upper:
            return studio
    return ""


def absolute_url(base_url: str, url: str) -> str:
    text = str(url or "").strip()
    if not text:
        return ""
    if text.startswith("//"):
        return f"https:{text}"
    return urljoin(f"{base_url.rstrip('/')}/", text)
