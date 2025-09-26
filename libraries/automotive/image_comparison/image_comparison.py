# Copyright 2025 Google LLC

import logging

import cv2
import numpy as np

from PIL import Image
from PIL import ImageChops
from PIL import ImageDraw


class CompareImagesUsingMSE:
  """
    Compares two images using mean squared error (MSE).
  """

  __DEFAULT_DIFF_THRESHOLD = 1

  def __init__(
      self,
      golden_image_path,
      test_image_path,
      diff_threshold=__DEFAULT_DIFF_THRESHOLD,
  ):
    self.diff_threshold = diff_threshold

    logging.info('Opening image, golden image path: %s', golden_image_path)
    self.golden_image = cv2.imread(golden_image_path)

    logging.info('Opening image, test image path: %s', test_image_path)
    self.test_image = cv2.imread(test_image_path)

    # Convert to Grey Scale
    logging.info('Converting images to Grey Scale')
    self.golden_image = cv2.cvtColor(self.golden_image, cv2.COLOR_BGR2RGB)
    self.test_image = cv2.cvtColor(self.test_image, cv2.COLOR_BGR2RGB)

    # Find the difference between the images.
    logging.info('Finding difference between images')
    self.diff = cv2.subtract(self.golden_image, self.test_image)

  def are_images_similar(self):
    """
      Find diff images using mean squared error (MSE).

      Returns:
        True if images are similar, False otherwise.
    """
    height, width = self.golden_image.shape[0], self.golden_image.shape[1]
    error_margin = np.sum(self.diff**2)
    mse = error_margin / (float(height * width))
    logging.info('MSE value: %s', mse)
    return mse <= self.diff_threshold

  def save_diff_image(self, diff_image_path):
    """
      Save diff image to file.

      Args:
        diff_image_path: Path to save diff image.
    """
    logging.info('Saving diff image to %s', diff_image_path)
    cv2.imwrite(diff_image_path, self.diff)


class CompareImagesUsingPIL:
  """
    Compares two images using PIL Library.
  """

  can_compare_images = True

  def __init__(
      self,
      golden_image_path,
      test_image_path,
      exclude_area=None,  # (left, upper, right, lower) e.g. (0, 0, 100, 50)
  ):
    logging.info(
        'Opening image, golden image path: %s',
        golden_image_path
    )
    self.golden_image = Image.open(golden_image_path)

    logging.info(
        'Opening image, test image path: %s',
        test_image_path
    )
    self.test_image = Image.open(test_image_path)

    if (
        self.golden_image.size != self.test_image.size
        or self.golden_image.mode != self.test_image.mode
    ):
      logging.error(
          'Images have different sizes or modes, cannot compare directly.'
      )
      self.can_compare_images = False
      return

    # Convert To Grey Scale
    logging.info('Converting images to RGB')
    self.golden_image = self.golden_image.convert('RGB')
    self.test_image = self.test_image.convert('RGB')

    # Exclude the given area from the images.
    if exclude_area:
      logging.info('Draw a black rectange on exclude area: %s', exclude_area)
      # Create a mask to exclude the specified area
      mask = Image.new(
          'L', self.golden_image.size, 255
      )
      # Draw a black rectangle over the exclusion area
      mask_drawer = ImageDraw.Draw(mask)
      mask_drawer.rectangle(exclude_area, fill=0)
      black_img = Image.new('RGB', self.golden_image.size, (0, 0, 0))
      self.golden_image = ImageChops.composite(
          self.golden_image, black_img, mask
      )
      self.test_image = ImageChops.composite(self.test_image, black_img, mask)

    # Find the difference between the images.
    logging.info('Finding difference between images')
    self.diff_image = ImageChops.difference(self.golden_image, self.test_image)

  def are_images_similar(self):
    """
      Find diff images using mean squared error (MSE).

      Returns:
        True if images are similar, False otherwise.
    """
    if not self.can_compare_images:
      logging.info(
          'Images cannot be compared, so they are not similar.'
      )
      return False

    if self.diff_image.getbbox():
      logging.info(
          'Images are not similar, diff image bbox: %s',
          self.diff_image.getbbox(),
      )
      return False

    logging.info('Images are similar')
    return True

  def save_diff_image(self, diff_image_path):
    """
      Save diff image to file.

      Args:
        diff_image_path: Path to save diff image.
    """
    logging.info('Saving diff image to %s', diff_image_path)
    self.diff_image.save(diff_image_path)
