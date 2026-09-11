import json
import sys
from contextvars import ContextVar

current_request_id: ContextVar[str | None] = ContextVar("current_request_id", default=None)


def emit_scrape_progress(
    stage: str,
    status: str,
    site_id: str = "",
    number: str = "",
    detail_url: str = "",
    message: str = "",
) -> None:
    request_id = current_request_id.get()
    if request_id is None:
        return
    params = {
        "request_id": request_id,
        "stage": stage,
        "status": status,
        "site_id": site_id,
        "number": number,
    }
    if detail_url:
        params["detail_url"] = detail_url
    if message:
        params["message"] = message
    notification = {"jsonrpc": "2.0", "method": "scrape.progress", "params": params}
    print(json.dumps(notification, ensure_ascii=False, separators=(",", ":")), flush=True, file=sys.stdout)