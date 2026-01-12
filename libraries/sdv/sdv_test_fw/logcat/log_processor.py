# Copyright (C) 2026 The Android Open Source Project
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

import re


class LogcatProcessor:

  def __init__(self, log):
    self.log = log

  def _parse_log_entry(self, log_entry):
    """Parses a single log entry to extract the timestamp and message.

    Args:
        log_entry (str): A single log entry.

    Returns:
        tuple: (timestamp, message) if the entry is valid, otherwise (None,
        None).
    """
    match = re.match(
        r"^(\d{2}-\d{2}"
        r" \d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+\w\s+[^:]+:\s*(.*)$",
        log_entry,
    )
    if match:
      timestamp = match.group(1)
      message = match.group(2).strip()
      return timestamp, message
    return None, None

  def _find_message(self, message, n=1, timestamp=None, after_message=None, regex=False):
    """Generalized helper to find messages in the log.

    Args:
        message (str): The message (partial or full) to search for.
        n (int): The occurrence number to find. Defaults to 1 (first
          occurrence).
        timestamp (str, optional): A specific timestamp to match or filter
          entries after. Defaults to None.
        after_message (str, optional): Find the message after this reference
          message. Defaults to None.
        regex (bool, optional): If true, the message is a regular expression. Defaults to False
    Returns:
        tuple: (timestamp, message) of the found entry, or (None, None) if not
        found.
    """
    count = 0
    found_after_message = after_message is None
    for log_entry in self._iterate_log_entries():
      log_timestamp, log_message = self._parse_log_entry(log_entry)
      if not log_timestamp:
        continue

      if timestamp and log_timestamp <= timestamp:
        continue

      if not found_after_message and after_message in log_message:
        found_after_message = True
        continue

      if found_after_message:
        if regex:
          if re.search(message, log_message):
            count += 1
            if count == n:
                return log_timestamp, log_message
        elif message in log_message:
            count += 1
            if count == n:
                return log_timestamp, log_message

    return None, None

  def _iterate_log_entries(self):
    """Yields complete log entries (including multiline logs) from the log."""
    buffer = []
    for line in self.log.splitlines():
      if self._is_new_entry(line):
        if buffer:
          yield "\n".join(buffer)
          buffer = []
      buffer.append(line)
    if buffer:
      yield "\n".join(buffer)

  def _is_new_entry(self, line):
    """Determines if a line starts a new log entry.

    Args:
        line (str): A line from the log.

    Returns:
        bool: True if the line starts a new log entry, False otherwise.
    """
    return bool(re.match(r"^\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}", line))

  def parse_crash_dump_lines(self, crash_dump):
    """Parses the captured crash dump lines and extracts relevant details.

    Args:
        crash_dump (list): Lines from the captured crash dump.

    Returns:
        dict: Parsed crash dump details.
    """
    result = {
        "timestamp": None,
        "pid": None,
        "level": None,
        "binary": None,
        "msg": [],
    }

    for line in crash_dump:
      match = re.match(
          r"^\s*(\d{2}-\d{2}"
          r" \d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+(\w)\s+([\w_]+)\s*:\s*(.*)",
          line,
      )
      if match:
        if not result["timestamp"]:
          result["timestamp"] = match.group(1)
          result["pid"] = match.group(2)
          result["level"] = match.group(4)
        result["binary"] = match.group(5)
        result["msg"].append(match.group(6))
      else:
        result["msg"].append(line.strip())

    return result

  def extract_tokens(self, log_entries):
    """Extracts crash dump details from log entries.

    Args:
        log_entries (str): The complete log as a single string.

    Returns:
        dict or None: Parsed crash dump details or None if no crash dump found.
    """
    crash_start_pattern = (
        r"^\s*(\d{2}-\d{2}"
        r" \d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+(\w)\s+([\w_]+)\s*:\s*(.*\S)\s*$"
    )
    crash_dump = []
    capture = False

    for line in log_entries.splitlines():
      if re.match(crash_start_pattern, line):
        capture = True
      if capture:
        crash_dump.append(line)
        if (
            "Tombstone written to" in line
            or "unable to connect to activity manager" in line
        ):
          break

    if crash_dump:
      return self.parse_crash_dump_lines(crash_dump)
    return None

  def nth_message(self, n, message, timestamp=None, regex=False):
    """Finds and returns the nth occurrence of a message in the log.

    Args:
        n (int): The occurrence number to find.
        message (str): The message to search for.
        timestamp (str, optional): A specific timestamp to match. Defaults to
          None.
        regex (bool, optional): If true, the message is a regular expression. Defaults to False

    Returns:
        str: The log entry containing the nth occurrence of the message, or None
        if not found.
    """
    _, log_message = self._find_message(message, n=n, timestamp=timestamp, regex=regex)
    return log_message

  def message_timestamp(self, message, regex=False):
    """Finds and returns the timestamp of the first occurrence of a message in the log.

    Args:
        message (str): The message to search for.
        regex (bool, optional): If true, the message is a regular expression. Defaults to False

    Returns:
        str: The timestamp of the message, or None if not found.
    """
    log_timestamp, _ = self._find_message(message, regex=regex)
    return log_timestamp

  def find_message_after_message(self, first_message, second_message, regex=False):
    """Finds and returns the first occurrence of a message that appears after another specified message.

    Args:
        first_message (str): The reference message to look for.
        second_message (str): The message to find after the first_message.
        regex (bool, optional): If true, the message is a regular expression. Defaults to False

    Returns:
        tuple: (timestamp, message) of the found second_message, or (None, None)
        if not found.
    """
    return self._find_message(second_message, after_message=first_message, regex=regex)


  def find_message_after_timestamp(self, message, timestamp, regex=False):
    """Finds the first occurrence of a message after a given timestamp.

    Args:
        timestamp (str): The timestamp to search after ("MM-DD HH:MM:SS.mmm").
        message (str): The message to search for.
        regex (bool, optional): If true, the message is a regular expression. Defaults to False

    Returns:
        tuple: (timestamp, message) if found, or (None, None) if not.
    """
    return self._find_message(message, timestamp=timestamp, regex=regex)
