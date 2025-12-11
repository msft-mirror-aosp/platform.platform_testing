# Copyright 2025, The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
import re
import subprocess
import base64
import uuid
import datetime
import json
from impl.enums import DataSource
import os
from urllib.parse import urlparse

class CodeSearchDownloader:
    def __init__(self, temp_dir):
        self.temp_dir = temp_dir
        os.makedirs(self.temp_dir, exist_ok=True)

    def download_from_codesearch(self, codesearch_url):
      try:
          parsed_url = urlparse(codesearch_url)
      except ValueError:
          print(f"Invalid URL: {codesearch_url}")
          return None
      if parsed_url.netloc != "source.corp.google.com":
          print(f"URL is not a source.corp.google.com URL: {codesearch_url}")
          return None
      pattern = re.compile(
          r"/h/"
          r"(?P<host>[^/]+)/"
          r"(?P<project>.+?)"
          r"/\+/"
          r"(?P<ref>[^:]+):"
          r"(?P<filepath>.*)"
      )
      match = pattern.match(parsed_url.path)
      if not match:
        print(f"Failed to match codesearch URL path pattern: {parsed_url.path}")
        return None
      parts = match.groupdict()
      file_path = parts['filepath']
      repo_marker = "frameworks/base/"
      try:
          repo_marker_index = file_path.index(repo_marker)
          relative_path_start = repo_marker_index + len(repo_marker)
          relative_path_in_repo = file_path[relative_path_start:]
          if not relative_path_in_repo.endswith(".json"):
              print(f"Path does not end with '.json': {relative_path_in_repo}")
              return None
          file_name = os.path.basename(relative_path_in_repo)
          # This base URL is specific to the frameworks/base repository
          base_gob_url = "https://googleplex-android.git.corp.google.com/platform/frameworks/base/+/refs/heads/main/"
          gob_curl_url = f"{base_gob_url}{relative_path_in_repo}?format=TEXT"
      except ValueError:
          print(f"Path does not contain '{repo_marker}': {file_path}")
          return None
      print(f"Constructed gob_curl_url: {gob_curl_url}")
      command = ["gob-curl", gob_curl_url]
      try:
        result = subprocess.run(
            command,
            capture_output=True,
            text=True,
            check=False
        )
        if result.returncode != 0:
          print(f"gob-curl failed with return code {result.returncode}")
          print(f"Stderr:\n{result.stderr}")
          return None

        raw_stdout = result.stdout
        if "NOT_FOUND" in raw_stdout:
          print(f"File not found at {gob_curl_url}")
          return None

        try:
            decoded_content = base64.b64decode(raw_stdout.strip()).decode('utf-8')
        except Exception as decode_e:
            print(f"Error decoding base64 content: {decode_e}")
            return None
        download_id = str(uuid.uuid4())
        local_file_path = os.path.join(self.temp_dir, f"{download_id}.json")
        with open(local_file_path, 'w') as f:
            f.write(decoded_content)
        print(f"Saved content to {local_file_path}")

        return {
            "id": download_id,
            "local_file_path": local_file_path,
            "file_name": file_name,
            "golden_name": file_name[:-5] if file_name.endswith(".json") else file_name,
            "codesearch_url": codesearch_url,
            "dataSource": DataSource.CODESEARCH.value
        }
      except Exception as e:
        print(f"Exception during download or save: {e}")
        return None

    def get_downloaded_content(self, codesearch_url):
        download_info = self.download_from_codesearch(codesearch_url)
        if download_info and 'local_file_path' in download_info:
            try:
                with open(download_info['local_file_path'], 'r') as f:
                    return f.read()
            except Exception as e:
                print(f"Error reading downloaded file: {e}")
                return None
        return None
