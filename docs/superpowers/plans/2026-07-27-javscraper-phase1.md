# JavScraper Phase 1 — Worker IPC & Scrapers

**Status:** ✅ COMPLETED

**Goal:** Build the Python Worker IPC layer and port 9 OpenAver scrapers.

**Architecture:** Python child process communicates with Kotlin via JSON-RPC 2.0 over stdin/stdout. Worker exposes 6 methods.

## File Structure

```
scraper-worker/
├── main.py                     # Entry: stdio JSON-RPC loop
├── ipc_handler.py              # Request routing, error handling
├── requirements.txt            # Python dependencies
├── pyproject.toml              # Project config
│
├── scrapers/
│   ├── __init__.py
│   ├── registry.py             # ScraperRegistry (pluggable)
│   ├── base.py                 # Abstract BaseScraper
│   ├── models.py               # Video, Actress, ScrapeResult
│   └── openaver/               # 9 ported scrapers
│       ├── javbus.py / javdb.py / jav321.py
│       ├── fc2.py / heyzo.py / avsox.py
│       ├── d2pass.py / dmm.py / javlibrary.py
│
├── core/
│   ├── file_prober.py          # Number extraction from filename
│   └── smart_search.py         # Multi-site chain search
│
└── tests/                      # 47 tests
    ├── test_base.py / test_models.py / test_registry.py
    ├── test_file_prober.py / test_ipc_handler.py
    ├── test_scraper_javbus.py / test_integration.py
```

## IPC Protocol

JSON-RPC 2.0 over \n-delimited JSON lines (stdin/stdout).

### Methods
| Method | Params | Returns |
|--------|--------|---------|
| list_sites | {} | [{id, name}] |
| get_capabilities | {} | {version, sites, features} |
| scrape | {file_path?, number, site?} | ScrapeResult |
| search | {keyword, sites?} | [Video] |
| probe_file | {file_path} | {number} |
| shutdown | {} | — |

## 9 Ported Scrapers

1. **JavBus** — javbus.com, requests + BeautifulSoup
2. **JavDB** — javdb.com, search + detail
3. **Jav321** — jav321.com/dn/{number}
4. **FC2** — adult.contents.fc2.com/article/{id}
5. **HEYZO** — heyzo.com/moviepages/{id}
6. **AVSox** — avsox.click/{number}
7. **D2Pass** — d2pass.com search + detail
8. **DMM** — dmm.co.jp (requires proxy)
9. **JavLibrary** — javlibrary.com search + detail

## Worker IPC Handler Fix

- `_scrape()` accepts both `file_path` (for backward compat) and `number` (for Kotlin SidecarManager)

## Tests

All 47 tests pass.