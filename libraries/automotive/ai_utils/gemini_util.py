# Copyright 2025 Google LLC

import json
import logging

from google import genai


class GeminiUtil:
  """
    Util class which provides methods to interact Gemini APIs
  """

  __LOG_TAG = "GeminiUtil"

  __DEFAULT_MODEL = "gemini-2.5-flash"
  __INSTRUCTIONS_FOR_GEMINI_MODEL = """
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

  def __init__(self, api_key: str, model_name: str = __DEFAULT_MODEL):
    self._model_name = model_name
    self._client = genai.Client(api_key=api_key)

  def _parse_response(self, response: str) -> str:
    """
      Parses the response from the Gemini model.
    """
    try:
      response_string = response.replace("`", "").replace("json", "")
      response_string = response_string.strip("\n")
      return json.loads(response_string)
    except json.JSONDecodeError as e:
      logging.error(f"{self.__LOG_TAG}: Error : %s", e)
      return {
          "is_element_visible": False,
          "short_answer": "Error parsing response",
          "detailed_explanation": "Error parsing response : %s" % e
      }

  def execute_quey_with_image_for_verifying_ui_element_visibility(
      self, image_path: str, question: str
  ):
    """
      Uploads an image and asks a question about it to verify the UI element
      visibility.

      Args:
          image_path: Path to the image file.
          question: The question to ask about the image.

      Returns:
          The response from the model.
    """
    try:
      # Upload the image using the File API
      logging.info(
          f"{self.__LOG_TAG}: Uploading Image : %s",
          image_path
      )
      uploaded_file = self._client.files.upload(file=image_path)
      logging.info(
          f"{self.__LOG_TAG}: File uploaded : %s",
          uploaded_file
      )

      # Call generate_content with the file and the question
      logging.info(
          f"{self.__LOG_TAG}: Executing Query : %s",
          question
      )
      response = self._client.models.generate_content(
          model=self._model_name,
          contents=[
              uploaded_file,
              "\n\n",
              self.__INSTRUCTIONS_FOR_GEMINI_MODEL,
              "\n\n",
              question,
          ],
      )
      query_response = self._parse_response(response.text)
      logging.info(
          f"{self.__LOG_TAG}: Executed Query Response : %s",
          query_response
      )
      return query_response
    except Exception as e:
      logging.error(
          f"{self.__LOG_TAG}: Error : %s",
          e
      )
      return {
          "is_element_visible": False,
          "short_answer": "Error executing query",
          "detailed_explanation": "Error executing query : %s" % e
      }

  def is_element_visible(self, response: str) -> bool:
    return response["is_element_visible"]

  def get_answer_for_query(self, response: str) -> str:
    return response["short_answer"]

  def get_explanation_for_query_response(self, response: str) -> str:
    return response["detailed_explanation"]
