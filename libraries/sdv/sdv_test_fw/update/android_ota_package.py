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

import binascii
import logging
import struct
import zipfile
# This class was copied from /system/update_engine/scripts/update_device.py
class AndroidOTAPackage(object):
  """Android update payload using the .zip format.

  Android OTA packages traditionally used a .zip file to store the payload. When
  applying A/B updates over the network, a payload binary is stored RAW inside
  this .zip file which is used by update_engine to apply the payload. To do
  this, an offset and size inside the .zip file are provided.
  """

  # Android OTA package file paths.
  OTA_PAYLOAD_BIN = "payload.bin"
  OTA_PAYLOAD_PROPERTIES_TXT = "payload_properties.txt"
  SECONDARY_OTA_PAYLOAD_BIN = "secondary/payload.bin"
  SECONDARY_OTA_PAYLOAD_PROPERTIES_TXT = "secondary/payload_properties.txt"
  PAYLOAD_MAGIC_HEADER = b"CrAU"

  def __init__(self, otafilename, secondary_payload=False):
    self.otafilename = otafilename

    otazip = zipfile.ZipFile(otafilename, "r")
    payload_entry = (self.SECONDARY_OTA_PAYLOAD_BIN if secondary_payload else
                     self.OTA_PAYLOAD_BIN)
    payload_info = otazip.getinfo(payload_entry)

    if payload_info.compress_type != 0:
      logging.error(
          "Expected payload to be uncompressed, got compression method %d",
          payload_info.compress_type)
    # Don't use len(payload_info.extra). Because that returns size of extra
    # fields in central directory. We need to look at local file directory,
    # as these two might have different sizes.
    with open(otafilename, "rb") as fp:
      fp.seek(payload_info.header_offset)
      data = fp.read(zipfile.sizeFileHeader)
      fheader = struct.unpack(zipfile.structFileHeader, data)
      # Last two fields of local file header are filename length and
      # extra length
      filename_len = fheader[-2]
      extra_len = fheader[-1]
      self.offset = payload_info.header_offset
      self.offset += zipfile.sizeFileHeader
      self.offset += filename_len + extra_len
      self.size = payload_info.file_size
      fp.seek(self.offset)
      payload_header = fp.read(4)
      if payload_header != self.PAYLOAD_MAGIC_HEADER:
        logging.warning(
            "Invalid header, expected %s, got %s."
            "Either the offset is not correct, or payload is corrupted",
            binascii.hexlify(self.PAYLOAD_MAGIC_HEADER),
            binascii.hexlify(payload_header))

    property_entry = (self.SECONDARY_OTA_PAYLOAD_PROPERTIES_TXT if
                      secondary_payload else self.OTA_PAYLOAD_PROPERTIES_TXT)
    self.properties = otazip.read(property_entry)

  def write_care_map(self, care_map_file_name):
    with zipfile.ZipFile(self.otafilename, "r") as zfp:
      CARE_MAP_ENTRY_NAME = "care_map.pb"
      if CARE_MAP_ENTRY_NAME in zfp.namelist():
        with open(care_map_file_name, "wb") as care_map_fp:
          care_map_fp.write(zfp.read(CARE_MAP_ENTRY_NAME))
          care_map_fp.flush()
