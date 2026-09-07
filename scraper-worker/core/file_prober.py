import re
import os
from typing import Optional


def extract_number(filename: str) -> Optional[str]:
    """Extract a JAV video number from a filename.

    Supports formats:
    - FC2-PPV-1234567
    - 041417-413  (6 digits dash 2+ digits)
    - 041417_413  (6 digits underscore 2+ digits)
    - SONE-205    (letters dash digits)
    - ABC12345    (letters immediately followed by digits, no dash)
    - N0762       (single letter + 4 digits, specific prefix set)
    - [ABC-123]   (bracketed)
    - ABC-123_UNCENSORED etc. (suffix stripping)
    """
    basename = os.path.splitext(os.path.basename(filename))[0]
    # Strip common qualifier suffixes
    basename = re.sub(
        r'[-_](UC|UNCEN|UNCENSORED|LEAK|LEAKED)(?=[-_.\s]|$)',
        '',
        basename,
        flags=re.IGNORECASE,
    )
    patterns = [
        (r'(FC2-PPV-\d+)', lambda m: m.group(1)),
        (r'(\d{6}-\d{2,})', lambda m: m.group(1)),
        (r'(\d{6}_\d{2,})', lambda m: m.group(1).replace("_", "-")),
        (r'([A-Za-z]+\d+-\d+)', lambda m: m.group(1).upper()),
        (r'\[([A-Za-z]{1,7}-\d{3,5})\]', lambda m: m.group(1).upper()),
        (r'([A-Za-z]{1,7}-\d{3,5})', lambda m: m.group(1).upper()),
        (r'([A-Za-z]{2,7})(\d{3,5})', lambda m: f"{m.group(1).upper()}-{m.group(2)}"),
        (r'([nkcmsNKCMS]\d{4})(?!\d)', lambda m: m.group(1).upper()),
    ]
    for pattern, formatter in patterns:
        match = re.search(pattern, basename)
        if match:
            return formatter(match)
    return None
