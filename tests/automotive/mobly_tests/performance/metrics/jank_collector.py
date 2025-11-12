import dataclasses
import re
from enum import Enum
from mobly.controllers.android_device_lib.services import base_service
from typing import Optional

class GfxInfoMetric(Enum):
  """
  Parses various metrics from the 'adb shell dumpsys gfxinfo' command output.

  Each enum member holds a compiled regex pattern, the capture group index
  to extract, and a string metric ID.
  """

  # The __init__ method below will be called with these arguments:
  # (pattern_str, group_index, metric_id)

  # Example: "Total frames rendered: 20391"
  TOTAL_FRAMES = (r"Total frames rendered: (\d+)", 1, "total_frames")

  # Example: "Janky frames: 785 (3.85%)"
  JANKY_FRAMES_COUNT = (
      r"Janky frames: (\d+) \(([0-9]+[\.]?[0-9]+)\%\)",
      1,
      "janky_frames_count",
  )

  # Example: "Janky frames: 785 (3.85%)"
  JANKY_FRAMES_PRCNT = (
      r"Janky frames: (\d+) \(([0-9]+[\.]?[0-9]+)\%\)",
      2,
      "janky_frames_percent",
  )

  # Example: "Janky frames (legacy): 785 (3.85%)"
  JANKY_FRAMES_LEGACY_COUNT = (
      r"Janky frames \(legacy\): (\d+) \(([0-9]+[\.]?[0-9]+)\%\)",
      1,
      "janky_frames_legacy_count",
  )

  # Example: "Janky frames (legacy): 785 (3.85%)"
  JANKY_FRAMES_LEGACY_PRCNT = (
      r"Janky frames \(legacy\): (\d+) \(([0-9]+[\.]?[0-9]+)\%\)",
      2,
      "janky_frames_legacy_percent",
  )

  # Example: "50th percentile: 9ms"
  FRAME_TIME_50TH = (r"50th percentile: (\d+)ms", 1, "frame_render_time_percentile_50")

  # Example: "90th percentile: 9ms"
  FRAME_TIME_90TH = (r"90th percentile: (\d+)ms", 1, "frame_render_time_percentile_90")

  # Example: "95th percentile: 9ms"
  FRAME_TIME_95TH = (r"95th percentile: (\d+)ms", 1, "frame_render_time_percentile_95")

  # Example: "99th percentile: 9ms"
  FRAME_TIME_99TH = (r"99th percentile: (\d+)ms", 1, "frame_render_time_percentile_99")

  # Example: "Number Missed Vsync: 0"
  NUM_MISSED_VSYNC = (r"Number Missed Vsync: (\d+)", 1, "missed_vsync")

  # Example: "Number High input latency: 0"
  NUM_HIGH_INPUT_LATENCY = (
      r"Number High input latency: (\d+)",
      1,
      "high_input_latency",
  )

  # Example: "Number Slow UI thread: 0"
  NUM_SLOW_UI_THREAD = (r"Number Slow UI thread: (\d+)", 1, "slow_ui_thread")

  # Example: "Number Slow bitmap uploads: 0"
  NUM_SLOW_BITMAP_UPLOADS = (
      r"Number Slow bitmap uploads: (\d+)",
      1,
      "slow_bmp_upload",
  )

  # Example: "Number Slow issue draw commands: 0"
  NUM_SLOW_DRAW = (r"Number Slow issue draw commands: (\d+)", 1, "slow_issue_draw_cmds")

  # Example: "Number Frame deadline missed: 0"
  NUM_FRAME_DEADLINE_MISSED = (
      r"Number Frame deadline missed: (\d+)",
      1,
      "deadline_missed",
  )

  # Number Frame deadline missed (legacy): 0
  NUM_FRAME_DEADLINE_MISSED_LEGACY = (
      r"Number Frame deadline missed \(legacy\): (\d+)",
      1,
      "deadline_missed_legacy",
  )

  # Example: "50th gpu percentile: 9ms"
  GPU_FRAME_TIME_50TH = (
      r"50th gpu percentile: (\d+)ms",
      1,
      "gpu_frame_render_time_percentile_50",
  )

  # Example: "90th gpu percentile: 9ms"
  GPU_FRAME_TIME_90TH = (
      r"90th gpu percentile: (\d+)ms",
      1,
      "gpu_frame_render_time_percentile_90",
  )

  # Example: "95th gpu percentile: 9ms"
  GPU_FRAME_TIME_95TH = (
      r"95th gpu percentile: (\d+)ms",
      1,
      "gpu_frame_render_time_percentile_95",
  )

  # Example: "99th gpu percentile: 9ms"
  GPU_FRAME_TIME_99TH = (
      r"99th gpu percentile: (\d+)ms",
      1,
      "gpu_frame_render_time_percentile_99",
  )

  def __init__(self, pattern_str: str, group_index: int, metric_id: str):
    """Initializes the enum member."""
    # We compile the pattern here, using re.DOTALL to match Java's Pattern.DOTALL
    self.pattern = re.compile(pattern_str, re.DOTALL)
    self.group_index = group_index
    self.metric_id = metric_id

  def parse(self, lines: str):
    """
    Applies the enum's regex pattern to the input string.

    Args:
        lines: The string output (e.g., from 'dumpsys gfxinfo') to parse.

    Returns:
        A float of the captured group value, or None if no match is found.
    """
    # re.search() is the equivalent of Java's matcher.find()
    matcher = self.pattern.search(lines)
    if matcher:
      try:
        # Java's group(int) is 1-indexed, which matches Python's.
        # Convert to float, similar to Java's Double.valueOf()
        return float(matcher.group(self.group_index))
      except (ValueError, IndexError):
        # This handles cases where the group isn't a valid number
        return None
    else:
      # Java returns null, Python's equivalent is None
      return None

  def get_metric_id(self):
    return self.metric_id

@dataclasses.dataclass
class JankCollectorConfig:
  tracked_packages: Optional[str] = None

class JankCollector(base_service.BaseService):
  """Collector for jank metrics for all or a list of processes."""

  GFXINFO_COMMAND_GET = "dumpsys gfxinfo %s"
  GFXINFO_COMMAND_RESET = GFXINFO_COMMAND_GET + " reset"
  MULTILINE_MATCHER = "[\\s\\S]*%s[\\s\\S]*"
  GFXINFO_OUTPUT_HEADER = "Graphics info for pid (\\d+) \\[(%s)\\]"
  GFXINFO_METRICS_PREFIX = "gfxinfo"
  FAILED_PACKAGES_COUNT_METRIC = GFXINFO_METRICS_PREFIX + "_failed_packages_count"

  def __init__(self, device, configs):
    if configs is None:
      configs = JankCollectorConfig()
    super().__init__(device, configs)

  def verify_matches(self, output, match, message):
    assert re.search(match, output), f"{message}\nExpected pattern: '{match}'\nActual output: '{output}'"

  def get_header_matcher(self, package):
    return self.MULTILINE_MATCHER % (self.GFXINFO_OUTPUT_HEADER % (package if package else ".*"))

  def clear_gfx_info(self, package=""):
    try:
      if not package:
        command = (self.GFXINFO_COMMAND_RESET % "--")
        output = self._device.adb.shell(command)
        self.verify_matches(output.decode('utf-8'), self.get_header_matcher(""), "No package headers in output.")
        self._device.log.debug('Cleared all gfxinfo.')
      else:
        command = (self.GFXINFO_COMMAND_RESET % package)
        output = self._device.adb.shell(command)
        self.verify_matches(output.decode('utf-8'), self.get_header_matcher(package), "No package headers in output.")
        self._device.log.debug('Cleared %s gfxinfo.' % package)
    except Exception as e:
      raise Exception('Failed to clear gfxinfo. %s' % e)

  def start_collecting(self):
    if not self._configs.tracked_packages:
      self.clear_gfx_info()
    else:
      exception_count = 0
      last_exception = None
      for pkg in self._configs.tracked_packages:
        try:
          self.clear_gfx_info(pkg)
        except Exception as e:
          self._device.log.debug('Encountered exception resetting gfxinfo. %s' % e)
          last_exception = e
          exception_count += 1
      if exception_count > 1:
        raise Exception('Multiple exceptions were encountered resetting gfxinfo. Reporting the last one only; others are visible in logs. %s' % last_exception)
      elif exception_count == 1:
        raise Exception('Encountered exception resetting gfxinfo. %s' % last_exception)

  def parse_gfxinfo_metrics(self, output):
    header_pattern = self.get_header_matcher("")
    header = re.fullmatch(header_pattern, output)
    if not header:
      raise Exception('Failed to parse package from gfxinfo output.')
    package_name = header.group(2)
    self._device.log.debug('Collecting metrics for: %s' % package_name)
    results = {}
    for metric in GfxInfoMetric:
      metric_key = "_".join([self.GFXINFO_METRICS_PREFIX, package_name, metric.get_metric_id()])
      value = metric.parse(output)
      if value is None:
        self._device.log.debug('Did not find %s from %s' % (metric_key, package_name))
      else:
        results[metric_key] = value
    return results

  def get_gfxinfo_metrics(self, package=""):
    try:
      command = (self.GFXINFO_COMMAND_GET % package)
      output = self._device.adb.shell(command)
      output_str = output.decode('utf-8')
      self._device.log.debug('output_str: %s' % output_str)
      self.verify_matches(output_str, self.get_header_matcher(package), "Missing package header.")
      package_metric_sections = re.split("\n\\*\\*", output_str)
      result = {}
      for i in range(1, len(package_metric_sections)):
        self._device.log.debug('package_metric_sections_%s: %s' % (str(i), package_metric_sections[i]))
        result.update(self.parse_gfxinfo_metrics(package_metric_sections[i]))
      return result
    except Exception as e:
      raise Exception('Failed to get gfxinfo. %s' % e)

  def get_metrics(self):
    result = {}
    failed_packages_count = 0
    if not self._configs.tracked_packages:
      result.update(self.get_gfxinfo_metrics())
    else:
      for package in self._configs.tracked_packages:
        try:
          result.update(self.get_gfxinfo_metrics(package))
        except Exception as e:
          self._device.log.debug('Encountered exception getting gfxinfo. %s' % e)
          failed_packages_count += 1
    result[self.FAILED_PACKAGES_COUNT_METRIC] = failed_packages_count
    return result