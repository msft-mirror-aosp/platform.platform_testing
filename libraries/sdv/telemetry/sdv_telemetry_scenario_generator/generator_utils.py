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

from datetime import timedelta
from math import ceil
import random
from typing import List, Tuple
from google.protobuf.text_format import MessageToString

CSV_DELIMITER = ','

# A list of words used for generating random field names. Using them instead of
# completely random names serves better for debugging purposes.
_RANDOM_WORDS = ['alpha', 'beta', 'gamma', 'delta', 'epsilon']


# Partitions the provided set into two sets, the first of which will have
# `count` randomly selected elements.
def random_partition_set(all: set, count: int) -> Tuple[set, set]:
    assert count <= len(all)

    a = set(random.sample(sorted(all), count))
    b = all - a

    assert len(a) == count
    assert len(a) + len(b) == len(all)

    return a, b


def as_text_proto(message) -> str:
    return MessageToString(message, use_short_repeated_primitives=True)


def generate_schedule(
    A: int, N: int, S: timedelta
) -> List[Tuple[int, timedelta, timedelta]]:
    """A schedule of metrics config lifecycles where:

      - The total time horizon is S seconds,
      - All N metrics configs are active at least once.
      - At each timestamp, exactly A metrics configs are active,

    Returns:
        a list of config activation periods with period being a tuple
        (config, activation_timestamp, deactivation_timestamp).
    """

    # The following construction will be used to generate the schedule.
    #
    # We split the time horizon of S seconds into `blocks` blocks with the same
    # duration interval of `block_duration`:
    #
    # -------------------- total S seconds ---------------------
    # | block_duration | block_duration | ... | block_duration |
    # ----------------- total `blocks` blocks ------------------
    #
    # Any metrics config activation period corresponds to some block, i.e.
    # the config can only be activated at block start and finishes exactly
    # when this block ends.
    #
    # For each block, exactly A metrics config will be active.
    #
    # As the total number of config activations is `blocks` * A, and each of
    # N metrics configs has to be activated at least once, `blocks` * A >= N
    # should be satisfied. Thus, `blocks` >= N / A, and we can set `blocks`
    # to `ceil(N / A)`.
    #
    # Then we fill in blocks with configs, one config by one (cycled),
    # i.e. we take the configs [0..A) and assign them to the first block,
    # then we take the configs [A..2A) and assign them to the second block,
    # and so on.
    #
    # For example, if A = 100, N = 150, S = 120, we get the following arrangement:
    #   * `blocks = ceil(150 / 100) = 2`
    #   * `block_duration = S / blocks = 120 / 2 = 60`
    #   * the 1st block is the interval [0; 60] and configs [0..99] are active
    #   * the 2nd block is the interval [60; 120] and configs [100..149],
    #     [0..49] are active.

    assert A <= N, (
        'the number of active configs must be less than or equal to the total '
        'number of configs'
    )

    # minimum number of blocks to fit all events
    blocks = ceil(N / A)

    # split [0, S] into equal blocks
    block_duration = S / blocks

    schedule = []
    for block_num in range(blocks):
        # Generate the parameters of a block
        start = block_duration * block_num
        end = block_duration * (block_num + 1)

        block = list(
            (config_id % N, start, end)
            for config_id in range(A * block_num, A * (block_num + 1))
        )
        schedule.extend(block)

    return schedule


class RandomNameGenerator:
    existing_names: set

    def __init__(self):
        self.existing_names = set()

    def next(self) -> str:
        """Generates a unique random field name."""
        while True:
            name = f'{random.choice(_RANDOM_WORDS)}_{random.randint(0, 999999)}'
            if name not in self.existing_names:
                self.existing_names.add(name)
                return name
