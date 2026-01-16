# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import dataclasses
import enum
import json
import logging
from typing import Any, Dict, Optional
import urllib.error
import urllib.parse
import urllib.request


class HttpMethod(enum.Enum):
    """Supported HTTP methods."""

    GET = "GET"
    POST = "POST"


class ApiClientError(Exception):
    """Base exception for all API Client errors."""

    pass


class NetworkError(ApiClientError):
    """Raised when a network connection failure occurs."""

    pass


class HttpError(ApiClientError):
    """Raised when the server returns a 4xx or 5xx status code."""

    def __init__(self, status_code: int, content: str):
        self.status_code = status_code
        self.content = content
        super().__init__(f"HTTP {status_code}: {content}")


@dataclasses.dataclass(frozen=True)
class ApiRequest:
    """Encapsulates the specifics of an API request.

    Attributes:
        path: The endpoint path (relative to the client's base_url).
        method: The HTTP method to use. Defaults to GET.
        payload: Optional JSON-serializable dictionary to send as the body.
        query_params: Optional dictionary of query parameters to append to the
          URL.
        headers: Optional dictionary of HTTP headers to include in the request.
    """

    path: str
    method: HttpMethod = HttpMethod.GET
    payload: Optional[Dict[str, Any]] = None
    query_params: Optional[Dict[str, Any]] = None
    headers: Optional[Dict[str, str]] = None

    def __post_init__(self):
        """Sanitizes the path to ensure it is relative."""
        # Remove leading slashes to prevent double slashes when joining with base_url
        if self.path.startswith("/"):
            object.__setattr__(self, "path", self.path.lstrip("/"))


class ApiClient:
    """Handles generic HTTP communication using the standard library."""

    def __init__(self, base_url: str):
        """Initializes the client with a base URL.

        Args:
            base_url: The root URL for all requests. Trailing slashes are
              removed.
        """
        self._base_url = base_url.rstrip("/")

    def execute(self, request: ApiRequest):
        """Executes the given API request.

        Args:
            request: The ApiRequest object containing request details.

        Returns:
            The parsed JSON response (as a dict or list), or None if response is
            empty.

        Raises:
            NetworkError: If the connection fails.
            HttpError: If the server returns a 4xx/5xx error.
            ApiClientError: For JSON parsing errors or other unexpected issues.
        """
        # Construct the full URL
        # The base URL is guaranteed to have no trailing slash (via __init__)
        # The request path is guaranteed to have no leading slash (via __post_init__)
        url = f"{self._base_url}/{request.path}"

        # Append query parameters
        if request.query_params:
            query_string = urllib.parse.urlencode(request.query_params)
            url = f"{url}?{query_string}"

        # Prepare headers and body
        headers = (request.headers or {}).copy()
        body = None

        if request.payload is not None:
            try:
                body = json.dumps(request.payload).encode("utf-8")
                headers["Content-Type"] = "application/json"
            except (TypeError, ValueError) as e:
                raise ApiClientError(f"Failed to serialize payload: {e}") from e

        # Perform request and parse JSON response
        req_obj = urllib.request.Request(
            url=url, data=body, headers=headers, method=request.method.value
        )

        try:
            with urllib.request.urlopen(req_obj) as response:
                response_body = response.read().decode("utf-8")
                if not response_body:
                    return None
                return json.loads(response_body)

        except urllib.error.HTTPError as e:
            # e.read() might contain helpful error details from the server
            error_msg = e.read().decode("utf-8") or e.reason
            logging.error(
                "HTTP Request failed: %s %s - %s",
                request.method.value,
                url,
                error_msg,
            )
            raise HttpError(e.code, str(error_msg)) from e

        except urllib.error.URLError as e:
            logging.error("Network error accessing %s: %s", url, e.reason)
            raise NetworkError(f"Connection failed: {e.reason}") from e

        except json.JSONDecodeError as e:
            logging.error("Failed to parse JSON response from %s", url)
            raise ApiClientError("Invalid JSON response received") from e
