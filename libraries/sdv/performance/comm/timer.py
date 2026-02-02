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

import functools
import logging
import time
from typing import Any, Callable


DEFAULT_METRIC_COMPUTATION_WARNING_THRESHOLD_SECONDS = 10


def log_exec_time(
    _func: Callable = None,
    *,
    threshold: int = DEFAULT_METRIC_COMPUTATION_WARNING_THRESHOLD_SECONDS,
) -> Callable:
  """Decorator that times the execution of the annotated metric computation function
  and logs the result.

  It can be used as a simple decorator `@log_exec_time` or with a custom
  threshold, e.g., `@log_exec_time(threshold=20)`.

  Args:
    _func: The function to be decorated. This is used to allow the decorator
      to be used with or without arguments.
    threshold: The execution time in seconds above which a warning will be
      logged.
  """

  def decorator(func: Callable) -> Callable:
    @functools.wraps(func)
    def timed_wrapper(*args, **kwargs) -> Any:
      logging.info(f"Executing '{func.__name__}' to compute metrics...")
      start = time.time()
      result = func(*args, **kwargs)
      duration = time.time() - start
      logging.info(
          f"Finished executing '{func.__name__}' to compute metrics after"
          f" '{duration:.2f}' seconds."
      )
      if duration > threshold:
        logging.warning(
            f"Computation of metrics with '{func.__name__}' exceeded"
            f" {threshold} seconds. This could"
            " have an impact on the timeout of the test itself!"
        )
      return result

    return timed_wrapper

  if _func is None:
    return decorator
  return decorator(_func)