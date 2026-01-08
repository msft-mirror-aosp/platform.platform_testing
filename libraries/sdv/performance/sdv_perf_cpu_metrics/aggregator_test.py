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
from unittest.mock import patch
from sdv_perf_cpu_metrics.aggregator import (
    Aggregate,
    compute_aggregated_metrics,
    query_iterator_to_dict,
    query_iterator_to_dict_split_by_value,
)


class MockQueryResult:
    def __init__(self, **kwargs):
        for k, v in kwargs.items():
            setattr(self, k, v)


class AggregatorTest(unittest.TestCase):
    def test_query_iterator_to_dict_with_valid_input(self):
        mock_iterator = iter([
            MockQueryResult(col1=1, col2='a'),
            MockQueryResult(col1=2, col2='b'),
        ])
        keys = ['col1', 'col2']
        expected = {'col1': [1, 2], 'col2': ['a', 'b']}
        self.assertEqual(query_iterator_to_dict(mock_iterator, keys), expected)

    def test_query_iterator_to_dict_split_by_value_with_valid_input(self):
        mock_iterator = iter([
            MockQueryResult(id=0, cpu=0, util=0.2),
            MockQueryResult(id=1, cpu=0, util=0.3),
            MockQueryResult(id=0, cpu=1, util=0.5),
            MockQueryResult(id=1, cpu=1, util=0.1),
        ])
        cols = ['id', 'cpu', 'util']
        expected = {
            'id': {'cpu_0': [0, 1], 'cpu_1': [0, 1]},
            'util': {'cpu_0': [0.2, 0.3], 'cpu_1': [0.5, 0.1]},
        }
        self.assertEqual(
            query_iterator_to_dict_split_by_value('cpu', mock_iterator, cols),
            expected,
        )

    def test_compute_aggregated_metrics_with_list_input(self):
        timeseries = {'test_data': [1, 2, 3, 4, 5]}
        aggregates = [Aggregate.MIN, Aggregate.MAX, Aggregate.MEAN]
        expected_results = {
            'test_data_min': 1,
            'test_data_max': 5,
            'test_data_mean': 3
        }
        self.assertEqual(
            compute_aggregated_metrics(timeseries, 'test_data', aggregates),
            expected_results)

    def test_compute_aggregated_metrics_with_dict_input(self):
        timeseries = {
            'test_data': {
                'sub1': [10, 20],
                'sub2': [30, 40]
            }
        }
        aggregates = [Aggregate.MEAN]
        expected_results = {
            'test_data_sub1_mean': 15,
            'test_data_sub2_mean': 35
        }
        self.assertEqual(
            compute_aggregated_metrics(timeseries, 'test_data', aggregates),
            expected_results)

    @patch('logging.warning')
    def test_compute_aggregated_metrics_with_empty_list(self, mock_warning):
        timeseries = {'empty_data': []}
        aggregates = [Aggregate.MIN]
        self.assertEqual(
            compute_aggregated_metrics(timeseries, 'empty_data', aggregates), {})
        mock_warning.assert_called_with(
            "Timeseries for column key 'empty_data' is empty, cannot compute aggregate 'Aggregate.MIN'")

    def test_compute_aggregated_metrics_with_invalid_column_key(self):
        timeseries = {'some_data': [1, 2, 3]}
        with self.assertRaises(KeyError):
            compute_aggregated_metrics(timeseries, 'wrong_key', [Aggregate.MIN])

    def test_compute_aggregated_metrics_with_invalid_data_type(self):
        timeseries = {'invalid_data': 'not a list or dict'}
        with self.assertRaises(TypeError):
            compute_aggregated_metrics(timeseries, 'invalid_data', [Aggregate.MIN])

    def test_compute_aggregated_metrics_with_prefix_and_suffix(self):
        timeseries = {'data': [10, 20]}
        aggregates = [Aggregate.MEAN]
        expected_results = {'prefix_data_mean_suffix': 15}
        self.assertEqual(
            compute_aggregated_metrics(
                timeseries, 'data', aggregates, key_prefix='prefix', key_suffix='suffix'),
            expected_results)


if __name__ == '__main__':
    unittest.main()
