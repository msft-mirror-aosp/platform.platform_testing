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
import logging
import math
import struct
from typing import List, Tuple
import wave
import numpy as np


def read_wav(file_path: str) -> Tuple[List[int], int]:
  """Reads a WAV file and returns audio data and sample rate."""
  try:
    with wave.open(file_path, 'rb') as wav_file:
      sr = wav_file.getframerate()
      num_frames = wav_file.getnframes()
      num_channels = wav_file.getnchannels()
      raw_audio = wav_file.readframes(num_frames)
      audio = struct.unpack(
          '<' + ('h' * (num_frames * num_channels)), raw_audio
      )
    return list(audio), sr
  except Exception as e:
    logging.error(f"Failed to read WAV file '{file_path}': {e}")
    raise Exception(f'Error reading WAV file: {e}')

def compute_mfcc(audio, sr, num_coeffs=13):
  """Computes simplified MFCCs without external libraries."""
  frame_size = sr // 40
  frames = [
      audio[i : i + frame_size]
      for i in range(0, len(audio), frame_size)
      if len(audio[i : i + frame_size]) == frame_size
  ]
  return [
      [math.log1p(sum(abs(sample) for sample in frame) / len(frame))]
      * num_coeffs
      for frame in frames
  ]


def calculate_similarity(mfcc1, mfcc2):
  """Calculates similarity score based on DTW distance."""
  return math.exp(-dtw_distance(mfcc1, mfcc2))


def dtw_distance(seq1, seq2):
  """Computes Dynamic Time Warping distance."""
  n, m = len(seq1), len(seq2)
  dtw = [[float('inf')] * (m + 1) for _ in range(n + 1)]
  dtw[0][0] = 0

  for i in range(1, n + 1):
    for j in range(1, m + 1):
      cost = cosine_distance(seq1[i - 1], seq2[j - 1])
      dtw[i][j] = cost + min(dtw[i - 1][j], dtw[i][j - 1], dtw[i - 1][j - 1])

  return dtw[-1][-1]


def cosine_distance(vec1, vec2):
  """Computes cosine distance between two vectors."""
  dot_product = sum(a * b for a, b in zip(vec1, vec2))
  mag1 = math.sqrt(sum(a * a for a in vec1))
  mag2 = math.sqrt(sum(b * b for b in vec2))
  return 1.0 if mag1 == 0 or mag2 == 0 else 1 - (dot_product / (mag1 * mag2))


def process_and_compare_wavs(original_file, captured_file):
  """Reads, normalizes, trims silence, and computes similarity between two WAV files."""
  y1, sr1 = read_wav(original_file)
  y2, sr2 = read_wav(captured_file)

  if not y1 or not y2:
    logging.error('Audio files are empty. Cannot compute MFCCs.')
    return None, None, None

  y1 = trim_silence(y1)
  y2 = trim_silence(y2)
  len1 = len(y1)
  len2 = len(y2)
  y1 = y1[:min(len1,len2)]
  y2 = y2[:min(len1,len2)]

  mfcc1, mfcc2 = compute_mfcc(y1, sr1), compute_mfcc(y2, sr2)
  similarity = calculate_similarity(mfcc1, mfcc2)

  return similarity, y1, y2


def detect_multiple_audio_sources(
    original_wav, recorded_wav, energy_threshold=0.2, similarity_threshold=0.8
):
  """Detects if multiple instances of the original WAV file were played at once."""
  similarity, y1, y2 = process_and_compare_wavs(original_wav, recorded_wav)
  if similarity is None:
    return False

  def compute_rms(audio, window_size):
    """Computes RMS (Root Mean Square) energy of an audio signal."""
    return [
        math.sqrt(
            sum(sample**2 for sample in audio[i : i + window_size])
            / window_size
        )
        for i in range(0, len(audio) - window_size, window_size)
    ]

  sr1 = read_wav(original_wav)[1]
  sr2 = read_wav(recorded_wav)[1]

  avg_original_rms = sum(compute_rms(y1, sr1 // 40)) / len(y1)
  avg_recorded_rms = sum(compute_rms(y2, sr2 // 40)) / len(y2)
  energy_diff = avg_recorded_rms - avg_original_rms

  return energy_diff > energy_threshold or similarity < similarity_threshold


def compare_wav_energy_and_quality(
    original_file, captured_file, expected_ratio=0.5, tolerance=0.1
):
  """Compares energy, peak amplitude, and spectral similarity of two WAV files."""
  similarity, y1, y2 = process_and_compare_wavs(original_file, captured_file)
  if similarity is None:
    return False

  def calculate_peak_amplitude(audio):
    return max(abs(sample) for sample in audio) if audio else 0

  peak_original = calculate_peak_amplitude(y1)
  peak_captured = calculate_peak_amplitude(y2)
  peak_ratio = peak_captured / peak_original if peak_original > 0 else 0

  lower_bound, upper_bound = (
      expected_ratio - tolerance,
      expected_ratio + tolerance,
  )
  return lower_bound <= peak_ratio <= upper_bound and similarity >= 0.8


def trim_silence(audio_data, threshold=0.1):
  start = 0
  end = len(audio_data)

  max_amplitude = max(abs(sample) for sample in audio_data) or 1
  threshold_value = max_amplitude * threshold

  for i, sample in enumerate(audio_data):
    if abs(sample) > threshold_value:
      start = i
      break

  for i in range(len(audio_data) - 1, -1, -1):
    if abs(audio_data[i]) > threshold_value:
      end = i + 1
      break

  return audio_data[start:end]


def wav_files_roughly_same_wave(file1, file2, tolerance=0.1):
  """
  Checks if two WAV files are roughly the same using the 'wave' library.

  Args:
    file1 (str): Path to the first WAV file.
    file2 (str): Path to the second WAV file.
    tolerance (float): Tolerance threshold for difference (0 to 1). Lower values are stricter.

  Returns:
    bool: True if the files are roughly the same, False otherwise.
  """
  with wave.open(file1, 'rb') as wav1, wave.open(file2, 'rb') as wav2:

    if wav1.getframerate() != wav2.getframerate():
      logging.error(f"Frame rates do not match. wav1: {wav1.getframerate()}, wav2: {wav2.getframerate()}")
      return False
    if wav1.getnchannels() != wav2.getnchannels():
      logging.error(f"Number of channels do not match. wav1: {wav1.getnchannels()}, wav2: {wav2.getnchannels()}")
      return False
    if wav1.getsampwidth() != wav2.getsampwidth():
      logging.error(f"Sample widths do not match. wav1: {wav1.getsampwidth()}, wav2: {wav2.getsampwidth()}")
      return False

    frames1 = wav1.readframes(wav1.getnframes())
    frames2 = wav2.readframes(wav2.getnframes())

    data1 = np.frombuffer(frames1, dtype=np.int16)
    data2 = np.frombuffer(frames2, dtype=np.int16)

    data1 = trim_silence(data1, tolerance)
    data2 = trim_silence(data2, tolerance)

    len1 = len(data1)
    len2 = len(data2)

    if abs(len1 - len2) > int(max(len1,len2)*tolerance):
      return False

    data1 = data1[:min(len1,len2)]
    data2 = data2[:min(len1,len2)]

    difference = data1.astype(np.int64) - data2.astype(np.int64)

    max_diff = np.max(np.abs(difference))
    max_val = max(np.max(np.abs(data1)), np.max(np.abs(data2)))

    if max_val == 0:
      return max_diff == 0

    relative_diff = max_diff / max_val

    return relative_diff <= tolerance
