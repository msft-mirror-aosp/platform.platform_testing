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

import json
import logging
from typing import Any, Dict

from google import genai


class GeminiError(Exception):
  """Base exception for Gemini-related errors."""

  pass


class GeminiUtil:
  """Util class which provides methods to interact Gemini APIs"""

  _LOG_TAG = "GeminiUtil"

  _DEFAULT_MODEL = "gemini-2.5-flash"
  _INSTRUCTIONS_FOR_GEMINI_MODEL = """
    You are an expert UI and UX tester who validates and verifies the presence of specific UI elements on a screen. Your task is to analyze an image of a UI and answer a question about a specific element.

    Instructions for Verification:

    1. Do not assume anything. Base your response solely on what is visible in the provided UI.

    2. Answer the question by providing a JSON object with three fields:

    `is_element_visible:` A boolean value (true or false) indicating whether the requested element is visible on the screen.

    `short_answer`: Short Answer for the asked question, e.g. If asked for speed it could be 10 mph or 16 km/h. If the element is not present, the answer should be "No"

    `detailed_explanation`: A string that describes the element's location and appearance if it is present. If the element is not present, the explanation should state that it is not visible on the UI.

    3. Do not hallucinate. If you are unsure or cannot identify the element, the is_element_visible field should be false, and the detailed_explanation should state that the element is not visible or cannot be confirmed.

    Example User Query Just for Reference:

    "What is the speed of the car?"

    Example AI Output:
    {

      "is_element_visible": true,

      "short_answer": "10 mph"

      "detailed_explanation": "The speedometer shows 10 as the number"

    }

    Other Example User Query Just for Reference:

    "Is App Grid Button Visible on the screen?"

    Example AI Output:
    {

      "is_element_visible": false,

      "short_answer": "No"

      "detailed_explanation": "App Grid Button Not Available on the screen"

    }
  """

  def __init__(self, api_key: str, model_name: str = _DEFAULT_MODEL):
    self._model_name = model_name
    self._client = genai.Client(api_key=api_key)

  def _parse_response(self, response_text: str) -> Dict[str, Any]:
    """Parses the JSON response from the Gemini model, cleaning it first.

    Args:
        response_text: The raw text response from the model.

    Returns:
        A dictionary parsed from the JSON response.

    Raises:
        GeminiError: If the response cannot be parsed as JSON.
    """
    try:
      cleaned_text = (
          response_text.strip()
          .removeprefix("```json")
          .removesuffix("```")
          .strip()
      )
      return json.loads(cleaned_text)
    except json.JSONDecodeError as e:
      logging.error(
          f'{self._LOG_TAG}: Failed to decode JSON response: {response_text}'
      )
      raise GeminiError('Failed to parse response from Gemini API.') from e

  def verify_ui_element_visibility(
      self, image_path: str, question: str
  ) -> Dict[str, Any]:
    """
      Uploads an image and asks a question to verify a UI element's visibility.

      Args:
          image_path: Path to the image file.
          question: The question to ask about the image.

      Returns:
          A dictionary with the parsed response from the model.

      Raises:
          GeminiError: If there is an error during API interaction.
    """
    try:
      logging.info(f'{self._LOG_TAG}: Uploading image: {image_path}')
      uploaded_file = self._client.files.upload(file=image_path)
      logging.info(
          f'{self._LOG_TAG}: File uploaded successfully: {uploaded_file.name}')

      logging.info(f'{self._LOG_TAG}: Executing query: "{question}"')
      response = self._client.models.generate_content(
          model=self._model_name,
          contents=[
              uploaded_file,
              self._INSTRUCTIONS_FOR_GEMINI_MODEL,
              question,
          ],
      )

      parsed_response = self._parse_response(response.text)
      logging.info(f'{self._LOG_TAG}: Parsed query response: {parsed_response}')
      return parsed_response

    except Exception as e:
      logging.error(
          f'{self._LOG_TAG}: An error occurred during Gemini query'
          f' execution: {e}'
      )
      raise GeminiError('Failed to execute query with Gemini API.') from e

  @staticmethod
  def is_element_visible(response: Dict[str, Any]) -> bool:
    """
      Extracts the 'is_element_visible' field from the response.
    """
    return response.get('is_element_visible', False)

  @staticmethod
  def get_short_answer(response: Dict[str, Any]) -> str:
    """
      Extracts the 'short_answer' field from the response.
    """
    return response.get('short_answer', 'N/A')

  @staticmethod
  def get_detailed_explanation(response: Dict[str, Any]) -> str:
    """
      Extracts the 'detailed_explanation' field from the response.
    """
    return response.get('detailed_explanation', 'No explanation available.')
