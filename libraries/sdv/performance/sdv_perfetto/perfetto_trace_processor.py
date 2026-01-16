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

"""SDV trace processor

This class is used to process perfetto trace file.
It provides methods to read the trace file and export the data to CrystalBall.

Example:

```
# Read the trace file
trace_processor = perfetto_trace_processor.PerfettoTraceProcessor(trace_file)
trace_metrics = trace_processor.query(
    'select c.ts, t.name, c.value / 1024 as value_kb from counters as c left
    join counter_track as t on c.track_id = t.id'
)

trace_metrics_dict = <processing_query_result_to_metrics_dict>(trace_data)

or

trace_metrics = trace_processor.get_built_in_metrics(['android_cpu'])
trace_metrics_dict =
perfetto_trace_processor.traceMetrics_to_dict(trace_metrics)

# Export to CrystalBall
# the trace_metrics_dict should be a dictionary in the format of json or similar
like:
# trace_metrics_dict =  {
#      'some_prefix_with_test_name': {
#      'metrics': [
#        {
#            'name': 'metric1',
#            'avg': 50,
#            'max': 800000,
#            'min': 100000,
#        },
#        {
#            'name': 'metric2',
#            'avg': 25,
#            'max': 500000,
#            'min': 100000,
#        }
#      ]
#      }
# }
# or
# trace_metrics_dict =  {
#      'some_prefix_with_test_name': {
#      'metrics':
#        {
#            'metric1_avg': 50,
#            'metric1_max': 800000,
#            'metric1_min': 100000,
#            'metric2_avg': 25,
#            'metric2_max': 500000,
#            'metric2_min': 100000,
#        }
#      }
#}

crystalball_exporter.export_to_crystalball(
    trace_metrics_dict,
    output_dir,
    test_name,
)

```
"""
import itertools
import logging
from typing import Any, Dict, Iterator, List, Optional
from google.protobuf import json_format
from perfetto import trace_processor
from perfetto.common.query_result_iterator import QueryResultIterator
from protos.perfetto.metrics import metrics_pb2
from sdv_perf_dashboard import crystalball_exporter

# Crystalball results file name
CB_FILENAME = 'test_results.txt'


class PerfettoTraceProcessor:

  def __init__(self, trace_file_path: str, timeout: int = 30):
    self.trace_file_path = trace_file_path
    self.trace_processor = trace_processor.TraceProcessor(trace=trace_file_path, config=trace_processor.TraceProcessorConfig(load_timeout=timeout))

  def query(self, query: str) -> QueryResultIterator:
    return self.trace_processor.query(query)

  def get_built_in_metrics(
      self, metric_names: List[str]
  ) -> metrics_pb2.TraceMetrics:
    return self.trace_processor.metric(metric_names)

  def get_trace_start_timestamp(self) -> int:
    """Returns the start timestamp of the trace in nanoseconds."""
    it = self.trace_processor.query('SELECT TRACE_START() AS trace_start')
    start_timestamp_ns = next(it).trace_start
    return start_timestamp_ns

  def get_trace_end_timestamp(self) -> int:
    """Returns the end timestamp of the trace in nanoseconds."""
    it = self.trace_processor.query('SELECT TRACE_END() AS trace_end')
    end_timestamp_ns = next(it).trace_end
    return end_timestamp_ns

  def export_built_in_metrics_to_crystalball(self, metric_names: List[str], output_dir: str,  test_name: str, omit_base_name: bool = True) -> 'PerfettoTraceProcessor':
    """Exports the data to crystalball.

    Args:
      metric_names: List[str], names of the perfetto built_in metrics to export
      output_dir: str, directory of the output file
      test_name: str, name of the test
      omit_base_name: bool, whether to omit the base name of the test
    Returns:
      PerfettoTraceProcessor, the trace processor object
    """
    logging.info('Exporting built-in metrics to CrystalBall.')
    crystalball_exporter.export_to_crystalball(
      data={test_name: traceMetrics_to_dict(self.get_built_in_metrics(metric_names))},
      output_dir = output_dir,
      test_name = test_name,
      omit_base_name = omit_base_name,
      name_field_key='name|process_name'
    )
    return self

def query_iterator_to_dict(query_iter: Iterator, keys: List[str]) -> Dict[str, List[Any]]:
    """
    Converts the {@link QueryResultIterator} returned by a Perfetto query into a dictionary
    where each entry represents one column and the values are the timeseries
    of that respective column.

    Note: This is needed as we do not have the 'pandas' library in Android.
    """
    num_rows_query_result = 0
    out = {key: [] for key in keys}
    for row in query_iter:
        for key in keys:
            v = getattr(row, key)
            if v is None:
                raise ValueError(f"No value for key '{key}' in the query result.")
            out[key].append(v)
        num_rows_query_result += 1

    if num_rows_query_result == 0:
        logging.warning(
            "Received empty query resultiterator. This indicates that the query was not successful or that the trace was empty.")
    else:
        logging.info(f"Query result contains {num_rows_query_result} row(s)")
    return out


def query_iterator_to_dict_split_by_value(column_to_split_by_value: str, rows_iter: Iterator, cols: List[str]) -> Dict[str, Dict[str, List[Any]]]:
    """
    Converts the {@link QueryResultIterator} returned by a Perfetto query into a dictionary
    where each entry represents one column with an additional sub-column introduced
    by the 'column_to_split_by_value' argument. The combination of column and
    sub-column yields the respective timeseries for each distinct value of the sub-column.

    Example:
    Suppose an iterator has three columns: 'interval_id', 'cpu', and 'util'. Given
    that 'cpu' has the distinct values '0' and '1', there are multiple identical
    entries for 'interval_id' for distinct value of 'cpu':

    interval_id | cpu | util
    -------------------------
         0      |  0  |  0.2
         1      |  0  |  0.3
         0      |  1  |  0.5
         1      |  1  |  0.1

    If 'column_to_split_by_value' is 'cpu', this function will create a dictionary
    with the following structure:
    {
        'interval_id': {'cpu_0': [0, 1], 'cpu_1': [0, 1]}
        'util': {'cpu_0': [0.2, 0.3], 'cpu_1': [0.5, 0.1]}
    }

    Args:
        column_to_split_by_value: The column to use for splitting the query result.
        rows_iter: The query result iterator to convert to a dictionary.
        cols: The list of columns to include in the dictionary.
    Returns:
        A dictionary where each entry represents one column with an additional sub-column introduced
        by the 'column_to_split_by_value' argument. The combination of column and
        sub-column yields the respective timeseries for each distinct value of the sub-column.
    Raises:
        KeyError: If the column to use for splitting is not in the list of columns.
    """

    if column_to_split_by_value not in cols:
        raise KeyError(
            "Column to use for splitting is not in query result.")

    rows_iter, dist_values_rows_iter = itertools.tee(rows_iter)
    distinct_values = set((getattr(row, column_to_split_by_value)
                          for row in dist_values_rows_iter))

    out = {col: {} for col in cols if col != column_to_split_by_value}
    for col, col_value in out.items():
        for value in distinct_values:
            col_value[column_to_split_by_value + f"_{value}"] = []

    for row in rows_iter:
        split_value = getattr(row, column_to_split_by_value)
        for col in cols:
            row_value = getattr(row, col)
            if col != column_to_split_by_value:
                out[col][column_to_split_by_value +
                         f"_{split_value}"].append(row_value)

    return out

def traceMetrics_to_dict(
    trace_metrics: metrics_pb2.TraceMetrics,
) -> Dict[str, Any]:
  """Converts a TraceMetrics proto to a dictionary.

  The built-in metrics are stored in
  protos.perfetto.metrics.metrics_pb2.TraceMetrics proto. This method converts
  the proto to a dictionary.

  Args:
    trace_metrics: The TraceMetrics proto to convert.
  """
  result = json_format.MessageToDict(
      trace_metrics,
      preserving_proto_field_name=True,
  )
  # json_format.MessageToDict converts int64 to string,
  # convert the string values back to number
  _convert_string_to_float(result)
  return result

def _convert_string_to_float(obj: Any) -> Dict[str, Any]:
  """Converts the string values in a dictionary to float value."""
  if isinstance(obj, dict):
    for key, value in obj.items():
      if isinstance(value, dict) or isinstance(value, list):
        _convert_string_to_float(value)
      elif isinstance(value, str):
        try:
          # Convert all convertible string to float for simplification
          obj[key] = float(value)
        except ValueError:
          pass  # Keep as string
  elif isinstance(obj, list):
    for item in obj:
      _convert_string_to_float(item)

# deprecated:use crystalball_utils.export_to_crystalball instead
def export_to_crystalball(
    data: Dict[str, Any],
    output_dir: str,
    test_name: str,
    omit_base_name: bool = True,
    name_field_key: Optional[str] = None,
) -> None:
  """Writes data to a CrystalBall output file.

  The data is first converted to a flattened format by compressing the keys.

  Repeated calls with the same output dir will append data to the same file.

  Args:
    data: input data
    output_dir: directory of the output file
    test_name: name used by CrystalBall to identify the set of metrics
    omit_base_name: omit the base metric name from each entry key
    name_field_key: if not None, use the value of this field to be partial of
      the flattened format key name
  """
  crystalball_exporter.export_to_crystalball(
      data, output_dir, test_name, omit_base_name, name_field_key
  )


