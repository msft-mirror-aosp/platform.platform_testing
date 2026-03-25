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

"""SDV Feature Flag Decorator"""

import functools
from mobly import signals

def skip_if_feature_disabled(feature_name):
    """Decorator that skips a test if a feature flag is disabled on the device."""

    def decorator(test_func):
        @functools.wraps(test_func)
        def wrapper(self, *args, **kwargs):
            flags = getattr(self, 'feature_flags', None)

            if flags is None:
                raise signals.TestSkip(
                    f"Skipping test: Feature flags not initialized in {self.__class__.__name__}."
                )

            if not flags.is_feature_enabled(feature_name):
                raise signals.TestSkip(
                    f"Skipping test: feature '{feature_name}' is disabled on the device."
                )
            return test_func(self, *args, **kwargs)
        return wrapper
    return decorator
