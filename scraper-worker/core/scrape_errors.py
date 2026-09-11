class ScrapeStageError(Exception):
    stage = "unknown"

    def __init__(
        self,
        message: str,
        *,
        code: int,
        stage: str,
        site_id: str = "",
        number: str = "",
        detail_url: str = "",
    ):
        super().__init__(message)
        self.message = message
        self.code = code
        self.stage = stage
        self.site_id = site_id
        self.number = number
        self.detail_url = detail_url


class WebpageRequestTimeoutError(ScrapeStageError):
    stage = "webpage_request"

    def __init__(self, message: str, **context):
        super().__init__(message, code=-30, stage=self.stage, **context)


class WebpageArchiveTimeoutError(ScrapeStageError):
    stage = "webpage_archive"

    def __init__(self, message: str, **context):
        super().__init__(message, code=-31, stage=self.stage, **context)


class WebpageArchiveError(ScrapeStageError):
    stage = "webpage_archive"

    def __init__(self, message: str, **context):
        super().__init__(message, code=-32, stage=self.stage, **context)