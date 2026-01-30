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

from argparse import ArgumentParser
from datetime import timedelta
import os
from pathlib import Path

from sdv_telemetry_scenario_generator.generate import generate


def main() -> None:
    parser = ArgumentParser()
    parser.add_argument('output_directory', metavar='output-directory')
    args = parser.parse_args()

    if not os.path.isdir(args.output_directory):
        os.makedirs(args.output_directory)

    generate(Path(args.output_directory), timedelta(minutes=2))


if __name__ == '__main__':
    main()
