from core.file_prober import extract_number


def test_standard_format():
    """ABC-123 → ABC-123"""
    assert extract_number("ABC-123.mp4") == "ABC-123"


def test_fc2_ppv():
    """FC2-PPV-1234567 → FC2-PPV-1234567"""
    assert extract_number("FC2-PPV-1234567.mp4") == "FC2-PPV-1234567"


def test_six_digit_dash():
    """041417-413 → 041417-413"""
    assert extract_number("041417-413.mp4") == "041417-413"


def test_six_digit_underscore():
    """041417_413 → 041417-413 (underscore → dash)"""
    assert extract_number("041417_413.mp4") == "041417-413"


def test_lowercase_uppercased():
    """abc-123 → ABC-123"""
    assert extract_number("abc-123.mp4") == "ABC-123"


def test_no_dash_inserted():
    """ABC12345 → ABC-12345"""
    assert extract_number("ABC12345.mp4") == "ABC-12345"


def test_bracketed():
    """[ABC-123] → ABC-123"""
    assert extract_number("[ABC-123].mp4") == "ABC-123"


def test_single_letter_prefix():
    """N0762 → N0762"""
    assert extract_number("N0762.mp4") == "N0762"


def test_son_series():
    """SONE-205 → SONE-205"""
    assert extract_number("SONE-205.mp4") == "SONE-205"


def test_full_path():
    """Full path with directories still extracts the number."""
    assert extract_number("/some/path/ABC-123.mp4") == "ABC-123"


def test_strip_uncensored_suffix():
    """ABC-123-UC → ABC-123"""
    assert extract_number("ABC-123-UC.mp4") == "ABC-123"


def test_strip_leaked_suffix():
    """ABC-123_LEAKED → ABC-123"""
    assert extract_number("ABC-123_LEAKED.mp4") == "ABC-123"


def test_no_match():
    """Random string returns None."""
    assert extract_number("random_video.mp4") is None


def test_empty_basename():
    """Empty name returns None."""
    assert extract_number(".mp4") is None
