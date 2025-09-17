# Copyright 2025 Google LLC

import sys

from mobly import test_runner


def run():
  """
    Pass test arguments after '--' to the test runner. Needed for Mobly Test Runner.

    Splits the arguments vector by '--'. Anything before separtor is treated as absl flags.
    Everything after is a Mobly Test Runner arguments. Example:

        python3 <test> -- -c /tmp/config.yaml

    Example usage:

        if __name__ == '__main__':
            test_runner.run()
  """
  if '--' in sys.argv:
    index = sys.argv.index('--')
    sys.argv = sys.argv[:1] + sys.argv[index + 1 :]
  test_runner.main()
