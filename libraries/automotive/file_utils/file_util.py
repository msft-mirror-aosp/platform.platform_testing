# Copyright 2025 Google LLC

import os
import importlib_resources


def join_path(base_dir: str, file_name: str) -> str:
  """
    Join a base directory and a file name to form an absolute path.

    Args:
      base_dir: The base directory to join with the file name.
      file_name: The file name to join with the base directory.

    Returns:
      The absolute path of the file.
  """
  return os.path.join(base_dir, file_name)


def is_valid_path(path: str) -> bool:
  """
    Check if a path exists.

    Args:
      path: The path to check.

    Returns:
      True if the path exists, False otherwise.
  """
  return os.path.exists(path)


def find_resource_path(pkg_name: str, resource_path: str) -> str:
  """
    Find a resource file packaged with the library.

    Args:
      pkg_name: The package name of the library.
      resource_path: The path of the resource file within the library.

    Returns:
      The absolute path of the resource file.
  """
  with importlib_resources.as_file(
      importlib_resources.files(pkg_name)
  ) as _path:
    return join_path(_path, resource_path)
