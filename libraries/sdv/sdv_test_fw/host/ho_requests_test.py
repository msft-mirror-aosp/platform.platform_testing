# Copyright (C) 2026 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import unittest

from sdv_test_fw.host import api_client
from sdv_test_fw.host import ho_requests


class HoRequestsDataTest(unittest.TestCase):
    """Tests for Host Orchestrator data model parsing."""

    def test_cvd_from_json_valid_data(self):
        """Verifies Cvd object creation from a populated JSON dictionary.

        Ensures that 'group' and 'name' are correctly extracted from the
        response structure typically returned by the host.
        """
        data = {"group": "cvd-group", "name": "cvd-1"}

        cvd = ho_requests.Cvd.from_json(data)

        self.assertEqual(cvd.group, "cvd-group")
        self.assertEqual(cvd.name, "cvd-1")

    def test_cvd_from_json_ignores_extra_fields(self):
        """Verifies that unrecognized fields in the JSON are ignored.

        The dataclass should strictly adhere to its schema and not store
        arbitrary extra data passed in the dictionary.
        """
        data = {
            "group": "cvd-group",
            "name": "cvd-1",
            "unknown_field": "should_be_ignored",
            "nested": {"id": 1},
        }

        cvd = ho_requests.Cvd.from_json(data)

        # Verify core fields are correct
        self.assertEqual(cvd.group, "cvd-group")
        self.assertEqual(cvd.name, "cvd-1")

        # Verify the object does not accidentally adopt extra attributes
        self.assertFalse(hasattr(cvd, "unknown_field"))
        self.assertFalse(hasattr(cvd, "nested"))

    def test_cvd_from_json_empty_data(self):
        """Verifies Cvd object creation handles missing fields gracefully."""
        data = {}

        cvd = ho_requests.Cvd.from_json(data)

        self.assertEqual(cvd.group, "")
        self.assertEqual(cvd.name, "")

    def test_operation_from_json_valid_data(self):
        """Verifies Operation object creation from a populated JSON dictionary.

        Ensures that 'name' and 'done' status are correctly parsed.
        """
        data = {"name": "operation-123", "done": True}

        op = ho_requests.Operation.from_json(data)

        self.assertEqual(op.name, "operation-123")
        self.assertTrue(op.done)

    def test_operation_from_json_defaults(self):
        """Verifies Operation object defaults (done=False) when fields are missing."""
        data = {"name": "operation-pending"}

        op = ho_requests.Operation.from_json(data)

        self.assertEqual(op.name, "operation-pending")
        self.assertFalse(op.done)


class HoRequestsFactoryTest(unittest.TestCase):
    """Tests for Host Orchestrator request generation factories."""

    def test_list_cvds_factory(self):
        """Verifies the list_cvds factory returns the correct ApiRequest."""
        request = ho_requests.list_cvds()

        self.assertEqual(request.path, "cvds")
        self.assertEqual(request.method, api_client.HttpMethod.GET)
        self.assertIsNone(request.payload)

    def test_get_operation_factory(self):
        """Verifies get_operation factory returns correct path and method."""
        request = ho_requests.get_operation("op-123")

        self.assertEqual(request.path, "operations/op-123")
        self.assertEqual(request.method, api_client.HttpMethod.GET)
        self.assertIsNone(request.payload)

    def test_cvd_action_start(self):
        """Verifies the START action request format.

        Ensures the payload is included and the URL is constructed with the
        correct group, name, and action suffix.
        """
        request = ho_requests.cvd_action(
            group="cvd-group",
            name="cvd-1",
            action=ho_requests.CvdAction.START,
            payload={"some": "config"},
        )

        self.assertEqual(request.path, "cvds/cvd-group/cvd-1/:start")
        self.assertEqual(request.method, api_client.HttpMethod.POST)
        self.assertEqual(request.payload, {"some": "config"})

    def test_cvd_action_stop(self):
        """Verifies the STOP action request format."""
        request = ho_requests.cvd_action(
            group="cvd-group", name="cvd-1", action=ho_requests.CvdAction.STOP
        )

        self.assertEqual(request.path, "cvds/cvd-group/cvd-1/:stop")
        self.assertEqual(request.method, api_client.HttpMethod.POST)
        self.assertIsNone(request.payload)

    def test_cvd_action_powerwash(self):
        """Verifies the POWERWASH action request format."""
        request = ho_requests.cvd_action(
            group="cvd-group",
            name="cvd-1",
            action=ho_requests.CvdAction.POWERWASH,
        )

        self.assertEqual(request.path, "cvds/cvd-group/cvd-1/:powerwash")
        self.assertEqual(request.method, api_client.HttpMethod.POST)
        self.assertIsNone(request.payload)

    def test_cvd_action_powerbtn(self):
        """Verifies the POWERBTN action request format."""
        request = ho_requests.cvd_action(
            group="cvd-group",
            name="cvd-1",
            action=ho_requests.CvdAction.POWERBTN,
        )

        self.assertEqual(request.path, "cvds/cvd-group/cvd-1/:powerbtn")
        self.assertEqual(request.method, api_client.HttpMethod.POST)
        self.assertIsNone(request.payload)


if __name__ == "__main__":
    unittest.main()
