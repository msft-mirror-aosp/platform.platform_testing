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

