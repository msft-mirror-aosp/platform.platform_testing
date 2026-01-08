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

"""SDV CommStack Test common functions."""
import logging
import re
import time
from typing import List
from mobly import asserts

import numpy as np


COMM_STACK_PUB_COMMAND = (
    '/system/bin/sdv_comms_client_rs publish --instance-name publisher'
    ' --service-unit-name publication --message-size {} --quantity {}'
    ' --interval-msec {}'
)
QUANTITY = [1000, 200, 100, 40, 20]
QUANTITY_LOCAL_PERF = [1000, 10000, 100000, 1000000]
QUANTITY_PING_PONG = [100, 1000, 10000]
INTERVAL_MSEC = [10, 50, 100, 250, 500]
MESSAGE_SIZE = [1024, 3096, 10240, 32768]
COMM_STACK_PUB_EXPECTED_RESULT = """
{{
    "Status":"Ok",
    "Result":{{
        "publisher_fqin":"instance\d+:google.sdv.comms.CommsClientApp/publisher",
        "duration_msec":\d+,
        "messages_written":{}
    }}
}}
"""
COMM_STACK_SUB_COMMAND = (
    '/system/bin/sdv_comms_client_rs subscribe --instance-name subscriber'
    ' --service-unit-name publication --quantity {}'
)
COMM_STACK_SUB_EXPECTED_RESULT = """
{{
    "Status":"Ok",
    "Result":{{
        "duration_msec":\d+,
        "messages_read":{},
        "messages_missed":0,
        "messages_corrupted":0,
        "messages_out_of_order":0,
        "first_message":\d+
    }}
}}
"""
LAST_MESSAGE_COMMAND = (
    '/system/bin/sdv_comms_client_rs get-last-message  --instance-name'
    ' instantreader --service-unit-name publication'
)
LAST_MESSAGE_EXPECTED_RESULT = """
    {{
        "Status":"Ok",
        "Result":{{
            "duration_msec":\d+,
            "message_size":{},
            "message":\[\s*(\d+,\s*)*\d+\s*\]
        }}
    }}
"""
LOCAL_PERF_COMMAND = (
    '/system/bin/sdv_comms_client_rs local-perf-test'
    ' --message-size={} --quantity={}'
)
LOCAL_PERF_EXPECTED_RESULT = """
    {{
        "Status":"Ok",
        "Result":{{
            "total_duration_msec":\d+,
            "total_messages":{},
            "total_bytes":{},
            "avg_read_write_msg_nsec":\d+,
            "avg_read_write_mbps":\d+
        }}
    }}
"""

PING_PONG_SERVER_COMMAND = (
    '/system/bin/sdv_comms_client_rs ping-pong-server'
    ' --instance-name pingpongserver-messagesize{}-quantity{}'
    ' --service-unit-name pingpong-messagesize{}-quantity{}'
)
PING_PONG_CLIENT_COMMAND = (
    '/system/bin/sdv_comms_client_rs ping-pong-client'
    ' --instance-name pingpongclient-messagesize{}-quantity{}'
    ' --service-unit-name pingpong-messagesize{}-quantity{}'
    ' --quantity {}'
    ' --message-size={}'
)
PING_PONG_EXPECTED_SERVER_RESULT = """{{"Status":"Ok","Result":{{"total_duration_msec":\d+,"messages_processed":{}}}}}"""
PING_PONG_EXPECTED_CLIENT_RESULT = """
    {{
        "Status":"Ok",
        "Result":{{
            "total_duration_msec":\d+,
            "message_size": {},
            "messages_processed":{},
            "min_round_trip_time_usec":\d+,
            "max_round_trip_time_usec":\d+,
            "avg_round_trip_time_usec":\d+,
            "total_round_trip_time_usec":\d+,
            "total_transmitted_bytes":{}
        }}
    }}
"""

QUERY_COMMAND = """
  WITH subscriber_events AS (
    SELECT
        s.ts AS sub_msg_ts
    FROM slice s
    WHERE s.name LIKE '%subscriber::message%'
  ),
  publisher_messages AS (
      SELECT
          ts AS pub_msg_ts
      FROM slice
      WHERE name LIKE '%publisher::write_messages%'
  )
  SELECT
      se.sub_msg_ts,
      (
          SELECT MAX(pm.pub_msg_ts)
          FROM publisher_messages pm
          WHERE pm.pub_msg_ts < se.sub_msg_ts -- Closest publisher message before subscriber message
      ) AS pub_msg_ts,
      se.sub_msg_ts - (
          SELECT MAX(pm.pub_msg_ts)
          FROM publisher_messages pm
          WHERE pm.pub_msg_ts < se.sub_msg_ts
      ) AS latency
  FROM subscriber_events se
  ORDER BY se.sub_msg_ts;
  """


def wait_for_condition(
    condition_checker,
    expected_result,
    timeout=30,
    poll_interval=1,
    error_message='Condition not met within timeout',
):
  """Args:

  condition_checker: A function that caller provides.
  expected_result: A string that the condition_checker should match.
  timeout: The maximum amount of time to wait for the condition to be met.
  poll_interval: The interval at which to check the condition.
  error_message: The error message to display if the condition is not met.
  """
  deadline = time.perf_counter() + timeout
  while time.perf_counter() < deadline:
    result = condition_checker()
    if is_log_matching(expected_result, result):
      return True
    time.sleep(poll_interval)
  asserts.fail(
    f'{error_message}: Expected "{expected_result}", but got "{condition_checker()}"'
)


def is_log_matching(expected_log, actual_log):
  match = re.search(expected_log, actual_log, re.VERBOSE)
  return match is not None


def percentiles(data: List[int], percentile_values: List[int]) -> dict[int, int]:
  asserts.assert_not_equal(data, [],'Data is empty')
  asserts.assert_not_equal(percentile_values, [],'Percentiles is empty')
  result = {}
  data_np = np.array(data)
  for p in percentile_values:
    asserts.assert_greater_equal(p,50, 'Percentile value should be greater than 50')
    asserts.assert_less(p,100, 'Percentile value should be less than 100')
    result[str(p)] = np.percentile(data_np, int(p))
  return result
