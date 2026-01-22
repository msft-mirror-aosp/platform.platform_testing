# Copyright 2025 Google LLC

import argparse
import sys


def parse_test_args(argv=None):
  r"""
    Get Test Args

    Extracts the test arguments from the System Args

    usage:
    python3 <test> -- -c /tmp/config.yaml --test_args=k1=v1 --test_args=k2=v2

    CATBox usage:
    1. When Using Par File ( --mobly-par-file-name <par_file_name> )
    catbox-tradefed run commandAndExit <test-plan> --mobly-options
    --test_args=k1=v1 --mobly-options --test_args=k2=v2

    2. When Using Module ( -m <ModuleName> )
    ./tools/catbox-tradefed run commandAndExit <test-plan> -m <ModuleName>
    --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-options:--test_args=k1=v1
    --test-arg com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-options:--test_args=k2=v2

    atest usage:
    atest <test> --
    --test-arg
    com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-options:--test_args=k1=v1
    --test-arg
    com.android.tradefed.testtype.mobly.MoblyBinaryHostTest:mobly-options:--test_args=k2=v2

    Returns: Dictionary with key-value pair
    e.g.
    {
        k1: v1,
        k2: v2
    }

    If the value has space, then use double quotes to wrap the value
    and escape the space with a backslash.
    e.g. --test_args=k1="some\ value"
  """
  if argv is None:
    argv = sys.argv

  parser = argparse.ArgumentParser(description='Parse Test Args.')
  group = parser.add_mutually_exclusive_group(required=False)
  group.add_argument(
      '--test_args',
      action='append',
      nargs='+',
      type=str,
      help='A list of test args for the test.',
  )
  parsed_test_args = parser.parse_known_args(argv)[0]

  if not parsed_test_args.test_args:
    return {}

  test_args_list = [
      test_arg
      for test_args in parsed_test_args.test_args
      for test_arg in test_args
  ]
  test_args = {
      test_arg.split('=')[0]: test_arg.split('=')[1].replace('\\','')
      for test_arg in test_args_list
  }
  return test_args
