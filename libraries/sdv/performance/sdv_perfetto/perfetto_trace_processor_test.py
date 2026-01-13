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
from unittest import mock

from perfetto.common.query_result_iterator import QueryResultIterator
from protos.perfetto.metrics import metrics_pb2
from protos.perfetto.metrics.android import cpu_metric_pb2
from sdv_perfetto import perfetto_trace_processor


class PerfettoTraceProcessorTest(unittest.TestCase):

  @mock.patch('perfetto.trace_processor.TraceProcessor')
  def test_query(self, mock_trace_processor):
    """Test that the query method calls the trace processor's query method."""
    trace_file_path = 'test_trace.pftrace'
    processor = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)
    mock_query_result = mock.MagicMock(spec=QueryResultIterator)
    mock_trace_processor.return_value.query.return_value = mock_query_result

    query_string = 'SELECT * FROM slice'
    result = processor.query(query_string)

    # Assert that the trace processor's query method was called with the correct query
    mock_trace_processor.return_value.query.assert_called_once_with(
        query_string
    )
    # Assert that the returned result is the mock QueryResultIterator
    self.assertEqual(result, mock_query_result)

  def test_traceMetrics_to_dict(self):
    trace_metrics = metrics_pb2.TraceMetrics(
        android_cpu=cpu_metric_pb2.AndroidCpuMetric(
            process_info=[
                cpu_metric_pb2.AndroidCpuMetric.Process(
                    name='proc1',
                    metrics=cpu_metric_pb2.AndroidCpuMetric.Metrics(
                        mcycles=50, avg_freq_khz=800000
                    ),
                ),
                cpu_metric_pb2.AndroidCpuMetric.Process(
                    name='proc2',
                    metrics=cpu_metric_pb2.AndroidCpuMetric.Metrics(
                        mcycles=25, avg_freq_khz=500000
                    ),
                ),
            ]
        )
    )
    expected = {
        'android_cpu': {
            'process_info': [
                {
                    'name': 'proc1',
                    'metrics': {'mcycles': 50, 'avg_freq_khz': 800000},
                },
                {
                    'name': 'proc2',
                    'metrics': {'mcycles': 25, 'avg_freq_khz': 500000},
                },
            ]
        }
    }

    self.assertEqual(
        perfetto_trace_processor.traceMetrics_to_dict(trace_metrics), expected
    )


  @mock.patch(
      'sdv_perfetto.perfetto_trace_processor.crystalball_exporter.export_to_crystalball'
  )
  @mock.patch('sdv_perfetto.perfetto_trace_processor.traceMetrics_to_dict')
  @mock.patch('perfetto.trace_processor.TraceProcessor')
  def test_export_built_in_metrics_to_crystalball(
      self,
      mock_trace_processor,
      mock_trace_metrics_to_dict,
      mock_export_to_crystalball,
  ):
    """Test export_built_in_metrics_to_crystalball method."""
    # Arrange
    print('test_export_built_in_metrics_to_crystalball')
    trace_file_path = 'test_trace.pftrace'
    processor = perfetto_trace_processor.PerfettoTraceProcessor(trace_file_path)

    metric_names = ['android_cpu']
    output_dir = '/fake/log/path'
    test_name = 'my_test'
    omit_base_name = False

    mock_metrics = metrics_pb2.TraceMetrics()
    # Since get_built_in_metrics is a method on the real object, we mock it directly on the instance.
    processor.get_built_in_metrics = mock.MagicMock(return_value=mock_metrics)

    mock_metrics_dict = {'android_cpu': {'foo': 'bar'}}
    mock_trace_metrics_to_dict.return_value = mock_metrics_dict

    # Act
    print('test_export_built_in_metrics_to_crystalball 2')
    print(type(processor))
    result = processor.export_built_in_metrics_to_crystalball(
        metric_names,
        output_dir,
        test_name,
        omit_base_name=omit_base_name,
    )

    # Assert
    processor.get_built_in_metrics.assert_called_once_with(metric_names)
    mock_trace_metrics_to_dict.assert_called_once_with(mock_metrics)
    mock_export_to_crystalball.assert_called_once_with(
        data={test_name: mock_metrics_dict},
        output_dir=output_dir,
        test_name=test_name,
        omit_base_name=omit_base_name,
        name_field_key='name|process_name',
    )
    self.assertEqual(result, processor)


if __name__ == '__main__':
  unittest.main()
