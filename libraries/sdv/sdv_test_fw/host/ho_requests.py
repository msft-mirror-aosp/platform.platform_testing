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

"""Factory functions and data models for Host Orchestrator API interaction."""

import dataclasses
import enum
from typing import Any, Dict, Optional

from sdv_test_fw.host import api_client


# ==============================================================================
# Data Models
# ==============================================================================


@dataclasses.dataclass(frozen=True)
class Cvd:
    """Represents a Virtual Instance on the host.

    Attributes:
        group: The group name of the CVD.
        name: The specific name of the CVD.
    """

    group: str
    name: str

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(group=data.get("group", ""), name=data.get("name", ""))


@dataclasses.dataclass(frozen=True)
class Operation:
    """Represents a long-running operation on the host.

    Attributes:
        name: The unique name of the operation.
        done: Boolean indicating if the operation has finished.
    """

    name: str
    done: bool

    @classmethod
    def from_json(cls, data: Dict[str, Any]):
        return cls(name=data.get("name", ""), done=data.get("done", False))


# ==============================================================================
# Enums
# ==============================================================================


class CvdAction(enum.Enum):
    """Available cvd actions."""

    POWERWASH = "powerwash"
    POWERBTN = "powerbtn"
    START = "start"
    STOP = "stop"


# ==============================================================================
# Request Factories
# ==============================================================================


def list_cvds() -> api_client.ApiRequest:
    """Returns the ApiRequest to list all active cvds."""
    return api_client.ApiRequest(path="cvds", method=api_client.HttpMethod.GET)


def get_operation(operation_name: str) -> api_client.ApiRequest:
    """Returns the ApiRequest to get the status of an operation."""
    # Handle cases where operation_name might already include the prefix
    path = (
        operation_name
        if operation_name.startswith("operations/")
        else f"operations/{operation_name}"
    )
    return api_client.ApiRequest(path=path, method=api_client.HttpMethod.GET)


def cvd_action(
    group: str,
    name: str,
    action: CvdAction,
    payload: Optional[Dict[str, Any]] = None,
) -> api_client.ApiRequest:
    """Returns the ApiRequest for a specific cvd action."""
    return api_client.ApiRequest(
        path=f"cvds/{group}/{name}/:{action.value}",
        method=api_client.HttpMethod.POST,
        payload=payload,
    )
