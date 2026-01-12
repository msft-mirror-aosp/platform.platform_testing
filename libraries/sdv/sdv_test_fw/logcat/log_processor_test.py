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

import unittest
from sdv_test_fw.logcat import log_processor


class TestLogProcessor(unittest.TestCase):

  def setUp(self):
    log_contents = (
        "01-01 12:00:00.000 1234 5678 I crash_dump64: Message one, type:"
        " DebuggerdTombstoneProto\n01-01 12:01:00.000 1234 5678 I crash_dump64:"
        " Message two, type: DebuggerdTombstoneProto\n01-01 12:02:00.000 1234"
        " 5678 E DEBUG   : failed to read process info: failed to open"
        " /proc/569: No such file or directory\n  Tombstone written to"
        " /data/tombstones/tombstone_01\n01-01 12:04:00.000 1234 5678 I crash_dump64:"
        " Message with regex: 123\n01-01 12:05:00.000 12345 5678 I crash_dump64:"
        " 12345\n01-01 12:06:00.000 12345 5678 I crash_dump64: Some message here.\n"
    )

    # Instantiate the LogProcessor with the log contents
    self.logcat_processor = log_processor.LogcatProcessor(log_contents)

  def test_parse_log_entry_valid(self):
    log_entry = "01-01 12:00:00.000 1234 5678 I TAG: Test message"
    timestamp, message = self.logcat_processor._parse_log_entry(log_entry)
    self.assertEqual(timestamp, "01-01 12:00:00.000")
    self.assertEqual(message, "Test message")

  def test_parse_log_entry_invalid(self):
    log_entry = "Invalid log entry"
    timestamp, message = self.logcat_processor._parse_log_entry(log_entry)
    self.assertIsNone(timestamp)
    self.assertIsNone(message)

  def test_nth_message_found(self):
    result = self.logcat_processor.nth_message(2, "Message")
    self.assertEqual(result, "Message two, type: DebuggerdTombstoneProto")

  def test_nth_message_not_found(self):
    result = self.logcat_processor.nth_message(3, "Nonexistent message")
    self.assertIsNone(result)

  def test_nth_message_regex_found(self):
    result = self.logcat_processor.nth_message(1, r"Message with regex: \d+", regex=True)
    self.assertEqual(result, "Message with regex: 123")

  def test_nth_message_regex_found_2(self):
    result = self.logcat_processor.nth_message(1, r"^\d+$", regex=True)
    self.assertEqual(result, "12345")

  def test_message_timestamp_found(self):
    timestamp = self.logcat_processor.message_timestamp("Message two")
    self.assertEqual(timestamp, "01-01 12:01:00.000")

  def test_message_timestamp_not_found(self):
    timestamp = self.logcat_processor.message_timestamp("Nonexistent message")
    self.assertIsNone(timestamp)

  def test_message_timestamp_regex_found(self):
    timestamp = self.logcat_processor.message_timestamp(r"Message with regex: \d+", regex=True)
    self.assertEqual(timestamp, "01-01 12:04:00.000")

  def test_message_timestamp_regex_found_2(self):
    timestamp = self.logcat_processor.message_timestamp(r"^\d+$", regex=True)
    self.assertEqual(timestamp, "01-01 12:05:00.000")

  def test_find_message_after_message_found(self):
    timestamp, message = self.logcat_processor.find_message_after_message(
        "Message one", "Message two"
    )
    self.assertEqual(timestamp, "01-01 12:01:00.000")
    self.assertEqual(message, "Message two, type: DebuggerdTombstoneProto")

  def test_find_message_after_message_regex_found(self):
    timestamp, message = self.logcat_processor.find_message_after_message(
      "Message one", r"Message with regex: \d+", regex=True
    )
    self.assertEqual(timestamp, "01-01 12:04:00.000")
    self.assertEqual(message, "Message with regex: 123")

  def test_find_message_after_message_not_found(self):
    timestamp, message = self.logcat_processor.find_message_after_message(
        "Message two", "Nonexistent message"
    )
    self.assertIsNone(timestamp)
    self.assertIsNone(message)

  def test_find_message_with_timestamp_filter(self):
    timestamp, message = self.logcat_processor._find_message(
        "Message", n=1, timestamp="01-01 12:00:30.000"
    )
    self.assertEqual(timestamp, "01-01 12:01:00.000")
    self.assertEqual(message, "Message two, type: DebuggerdTombstoneProto")

  def test_find_message_with_timestamp_excluded(self):
    timestamp, message = self.logcat_processor._find_message(
        "Nonexistent message", n=1, timestamp="01-01 12:02:30.000"
    )
    self.assertIsNone(timestamp)
    self.assertIsNone(message)

  def test_find_message_with_after_message(self):
    timestamp, message = self.logcat_processor._find_message(
        "Message", n=1, after_message="Message one"
    )
    self.assertEqual(timestamp, "01-01 12:01:00.000")
    self.assertEqual(message, "Message two, type: DebuggerdTombstoneProto")

  def test_find_message_with_after_message_not_found(self):
    timestamp, message = self.logcat_processor._find_message(
        "Nonexistent message", n=1, after_message="Message one"
    )
    self.assertIsNone(timestamp)
    self.assertIsNone(message)

  def test_extract_tokens_found(self):
    tokens = self.logcat_processor.extract_tokens(self.logcat_processor.log)
    self.assertIsNotNone(tokens)
    self.assertEqual(tokens["timestamp"], "01-01 12:00:00.000")
    self.assertEqual(tokens["binary"], "DEBUG")
    self.assertIn(
        "failed to read process info: failed to open /proc/569: No such file or"
        " directory",
        tokens["msg"],
    )
    self.assertIn(
        "Tombstone written to /data/tombstones/tombstone_01", tokens["msg"]
    )

  def test_extract_tokens_not_found(self):
    log_contents = "No crash dumps here"
    logcat_processor = log_processor.LogcatProcessor(log_contents)
    tokens = logcat_processor.extract_tokens(logcat_processor.log)
    self.assertIsNone(tokens)

  def test_nth_message_wildcard_found(self):
      result = self.logcat_processor.nth_message(1, ".*message.*", regex=True)
      self.assertEqual(result, "Some message here.")

  def test_message_timestamp_wildcard_found(self):
      timestamp = self.logcat_processor.message_timestamp(r".*message.*", regex=True)
      self.assertEqual(timestamp, "01-01 12:06:00.000")


if __name__ == "__main__":
  unittest.main()
