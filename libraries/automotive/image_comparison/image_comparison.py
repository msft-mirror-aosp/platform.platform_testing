# Copyright 2025 Google LLC

import abc
import logging
from typing import Optional, Tuple

import cv2
import numpy as np

from PIL import Image
from PIL import ImageChops
from PIL import ImageDraw


_LOG_TAG = 'ImageComparison'


class ImageComparisonError(Exception):
  """
    Base exception for image comparison errors.
  """

  pass


class ImageComparator(abc.ABC):
  """
    Abstract base class for image comparison strategies.
  """

  def __init__(self, golden_image_path: str, test_image_path: str):
    self._golden_image_path = golden_image_path
    self._test_image_path = test_image_path
    self._diff_image = None

  @abc.abstractmethod
  def are_images_similar(self) -> bool:
    """
      Compares the two images and determines if they are similar.

      Returns:
          True if the images are considered similar, False otherwise.
    """
    raise NotImplementedError

  @abc.abstractmethod
  def save_diff_image(self, diff_image_path: str) -> None:
    """
      Saves a visual representation of the difference between the images.

      Args:
          diff_image_path: The path where the difference image will be saved.
    """
    raise NotImplementedError


class CompareImagesUsingMSE(ImageComparator):
  """
    Compares two images using mean squared error (MSE).

    This method is effective for detecting pixel-level differences and is
    robust against minor variations that might not be perceptible to the human
    eye. It is particularly useful when comparing images that should be nearly
    identical.
  """

  _DEFAULT_DIFF_THRESHOLD = 1.0

  def __init__(
      self,
      golden_image_path: str,
      test_image_path: str,
      diff_threshold: float = _DEFAULT_DIFF_THRESHOLD,
  ):
    super().__init__(golden_image_path, test_image_path)
    self.diff_threshold = diff_threshold
    self._load_and_process_images()

  def _load_and_process_images(self):
    """
      Loads images, validates them, and calculates the difference.
    """
    try:
      logging.info(
          f'{_LOG_TAG}: Loading golden image: {self._golden_image_path}')
      golden_image = cv2.imread(self._golden_image_path)
      if golden_image is None:
        raise ImageComparisonError(
            f'Failed to load golden image at: {self._golden_image_path}'
        )

      logging.info(f'{_LOG_TAG}: Loading test image: {self._test_image_path}')
      test_image = cv2.imread(self._test_image_path)
      if test_image is None:
        raise ImageComparisonError(
            f'Failed to load test image at: {self._test_image_path}'
        )

      if golden_image.shape != test_image.shape:
        raise ImageComparisonError(
            'Images have different dimensions: '
            f'Golden={golden_image.shape}, Test={test_image.shape}'
        )

      self._golden_image = cv2.cvtColor(golden_image, cv2.COLOR_BGR2RGB)
      self._test_image = cv2.cvtColor(test_image, cv2.COLOR_BGR2RGB)
      self._diff_image = cv2.subtract(self._golden_image, self._test_image)

    except Exception as e:
      raise ImageComparisonError(
          'Error processing images for MSE comparison.') from e

  def are_images_similar(self) -> bool:
    height, width, _ = self._golden_image.shape
    error_margin = np.sum(self._diff_image**2)
    mse = error_margin / (height * width)
    logging.info(
        f'{_LOG_TAG}: MSE value: {mse:.2f} (Threshold:'
        f' {self.diff_threshold:.2f})'
    )
    return mse <= self.diff_threshold

  def save_diff_image(self, diff_image_path: str) -> None:
    logging.info(f'{_LOG_TAG}: Saving diff image to {diff_image_path}')
    # Convert back to BGR for saving with OpenCV
    diff_bgr = cv2.cvtColor(self._diff_image, cv2.COLOR_RGB2BGR)
    cv2.imwrite(diff_image_path, diff_bgr)


class CompareImagesUsingPIL(ImageComparator):
  """
    Compares two images using the Python Imaging Library (PIL).

    This method is useful for structural comparisons and can optionally
    exclude regions from the comparison, which is ideal for ignoring dynamic
    elements like clocks or status bars.
  """

  def __init__(
      self,
      golden_image_path: str,
      test_image_path: str,
      exclude_area: Optional[Tuple[int, int, int, int]] = None,
      include_area: Optional[Tuple[int, int, int, int]] = None,
  ):
    super().__init__(golden_image_path, test_image_path)
    self._exclude_area = exclude_area
    self._include_area = include_area
    self._load_and_process_images()

  def _load_and_process_images(self):
    """
      Loads images, validates them, and calculates the difference.
    """
    try:
      logging.info(
          f'{_LOG_TAG}: Loading golden image: {self._golden_image_path}')
      golden_image = Image.open(self._golden_image_path).convert('RGB')

      logging.info(
          f'{_LOG_TAG}: Loading test image: {self._test_image_path}')
      test_image = Image.open(self._test_image_path).convert('RGB')

      if golden_image.size != test_image.size:
        raise ImageComparisonError(
            'Images have different sizes: '
            f'Golden={golden_image.size}, Test={test_image.size}'
        )

      self._golden_image = golden_image
      self._test_image = test_image

      if self._exclude_area or self._include_area:
        self._apply_exclusion_mask()

      self._diff_image = ImageChops.difference(
          self._golden_image, self._test_image
      )
    except FileNotFoundError as e:
      raise ImageComparisonError(f'Image file not found: {e.filename}') from e
    except Exception as e:
      raise ImageComparisonError(
          'Error processing images for PIL comparison.') from e

  def _create_exclusion_mask(self):
    """
      Create a black mask to be used to exclude an area of both images.
    """
    logging.info(
        f'{_LOG_TAG}: Creating exclusion mask'
    )
    mask = Image.new('L', self._golden_image.size, 255)
    mask_drawer = ImageDraw.Draw(mask)
    if self._exclude_area:
      mask_drawer.rectangle(self._exclude_area, fill=0)
    if self._include_area:
      image_right, image_bottom = self._golden_image.size
      include_left, include_top, include_right, include_bottom = self._include_area
      mask_drawer.rectangle((0, 0, image_right, include_top), fill=0)
      mask_drawer.rectangle((0, include_bottom, image_right, image_bottom), fill=0)
      mask_drawer.rectangle((0, include_top, include_left, include_bottom), fill=0)
      mask_drawer.rectangle((include_right, include_top, image_right, include_bottom), fill=0)
    return mask

  def _apply_exclusion_mask(self):
    """
      Applies a black mask to the excluded area, or all but the included area, on both images.
    """
    logging.info(
        f'{_LOG_TAG}: Applying exclusion mask to area: {self._exclude_area}'
    )
    mask = self._create_exclusion_mask()
    black_img = Image.new('RGB', self._golden_image.size, (0, 0, 0))

    self._golden_image = ImageChops.composite(
        self._golden_image, black_img, mask
    )
    self._test_image = ImageChops.composite(
        self._test_image, black_img, mask
    )

  def are_images_similar(self) -> bool:
    if not self._diff_image:
      return False

    diff_bbox = self._diff_image.getbbox()
    if diff_bbox:
      logging.info(
          f'{_LOG_TAG}: Images are not similar. Difference bounding box:'
          f' {diff_bbox}'
      )
      return False

    logging.info(f'{_LOG_TAG}: Images are similar.')
    return True

  def save_diff_image(self, diff_image_path: str) -> None:
    logging.info(f'{_LOG_TAG}: Saving diff image to {diff_image_path}')
    self._diff_image.save(diff_image_path)

    # Highlight the difference image in violet color (138,43,226)
    # Convert the difference image to grayscale for thresholding
    diff_gray = self._diff_image.convert('L')
    # Create a binary mask where differences are white and
    # no differences are black
    mask = diff_gray.point(lambda p: 255 if p > 0 else 0)
    highlight_color = Image.new('RGB', self._golden_image.size, (138,43,226))
    highlighted_diff_image = Image.composite(
        highlight_color, self._golden_image, mask)

    # Save the highlighted difference image
    diff_image_path_parts = diff_image_path.rsplit('.', 1)
    highlighted_diff_image_path = (
        f'{diff_image_path_parts[0]}_highlighted.{diff_image_path_parts[1]}'
    )
    logging.info(
        f'{_LOG_TAG}: Saving highlighted diff image to'
        f' {highlighted_diff_image_path}'
    )
    highlighted_diff_image.save(highlighted_diff_image_path)
