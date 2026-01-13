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

from enum import auto
from enum import Enum
import itertools
import logging
from statistics import mean
from statistics import stdev
from typing import Any, Callable, Dict, Iterator, List, Optional


KEYS = ['interval_id', 'interval_start_ns', 'interval_end_ns', 'overall_cpu_perc']
PER_CPU_KEYS = ['interval_id', 'interval_start_ns', 'interval_end_ns', 'cpu', 'overall_cpu_perc']

class Aggregate(Enum):
    MIN = auto()
    MAX = auto()
    MEAN = auto()
    STDEV = auto()

    @classmethod
    def to_fun(cls, key: int) -> Callable[[Iterator[float]], float]:
        """Fetch the corresponding function for a given aggregate type."""
        match key:
            case cls.MIN:
                return cls.agg_min
            case cls.MAX:
                return cls.agg_max
            case cls.MEAN:
                return cls.agg_mean
            case cls.STDEV:
                return cls.agg_stdev
            case _:
                raise KeyError(
                    f"Key value of '{key}' not registered to any function.")

    def agg_min(ts: Iterator[float]) -> float:
        """Computes the minimum value of the iterable."""
        return min(ts)

    def agg_max(ts: Iterator[float]) -> float:
        """Computes the maximum value of the iterable."""
        return max(ts)

    def agg_mean(ts: Iterator[float]) -> float:
        """Computes the mean value of the iterable."""
        return mean(ts)

    def agg_stdev(ts: Iterator[float]) -> float:
        """Computes the standard deviation of the iterable."""
        data = list(ts)
        if len(data) < 2:
            return 0.0
        return stdev(data)

    def to_str(self) -> str:
        """Converts the enum value to a string representation."""
        match self:
            case self.MIN:
                return "min"
            case self.MAX:
                return "max"
            case self.MEAN:
                return "mean"
            case self.STDEV:
                return "stdev"
            case _:
                raise ValueError(
                    f"Value of '{self}' cannot be converted to any string.")

# TODO: move this to a separate library or perfetto_trace_processor module
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


def compute_aggregated_metrics(
        timeseries: Dict[str, List[Any] | Dict[str, List[Any]]],
        column_key: str,
        aggregates: List[Aggregate],
        key_prefix: Optional[str] = None,
        key_suffix: Optional[str] = None) -> Dict[Aggregate, float]:
    """
    Computes aggregated metrics, such as the maximum value, as specified by 'aggregates' for the input
    'timeseries' data given a specific column 'column_key'.
    Args:
        timeseries: The timeseries data to compute aggregated metrics from.
        column_key: The column key to compute aggregated metrics for.
        aggregates: The list of aggregates to compute.
        key_prefix: An optional prefix to add to the key of each aggregate.
        key_suffix: An optional suffix to add to the key of each aggregate.
    Returns:
        A dictionary of aggregated metrics, where the key is the aggregate and the value is the
        computed value.
    Raises:
        TypeError: If the data for the given column is not of type 'list' or 'dict'.
        KeyError: If the column key is not found in the timeseries.
        ValueError: If the timeseries data is empty or if the column key is not found in the timeseries.
    """
    def _compute_aggregated_metrics(data: List[Any], column_key: str, aggregates: List[Aggregate], key_prefix: Optional[str] = None, key_suffix: Optional[str] = None):
        aggregated_metrics = {}
        for aggregate in aggregates:
            aggregate_metric_key = column_key + "_" + aggregate.to_str()
            if key_prefix:
                aggregate_metric_key = key_prefix + "_" + aggregate_metric_key
            if key_suffix:
                aggregate_metric_key = aggregate_metric_key + "_" + key_suffix

            if len(data) > 0:
                aggregated_metrics[aggregate_metric_key] = Aggregate.to_fun(
                    aggregate)(data)
            else:
                logging.warning(
                    f"Timeseries for column key '{column_key}' is empty, cannot compute aggregate '{aggregate}'")
        return aggregated_metrics

    data = timeseries[column_key]
    if data is None:
        raise ValueError(
            f"Column key '{column_key}' not found in the timeseries.")
    aggregated_metrics = {}
    if isinstance(data, list):
        aggregated_metrics.update(_compute_aggregated_metrics(
            data, column_key, aggregates, key_prefix, key_suffix))
    elif isinstance(data, dict):
        for sub_col, timeseries_for_sub_col in data.items():
            aggregated_metrics.update(_compute_aggregated_metrics(
                timeseries_for_sub_col, column_key + "_" + sub_col, aggregates, key_prefix, key_suffix))
    else:
        raise TypeError(
            f"Unexpected type '{type(data)}' for column '{column_key}'. Supported types are 'list' and 'dict'.")
    return aggregated_metrics

