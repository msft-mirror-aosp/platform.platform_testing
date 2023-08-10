/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <string>
#include <utility>
#include <functional>
#include <string>
#include <sstream>

#include <flag_checker.h>
#include <gtest/gtest.h>

namespace android::test::flag {

void SkipIfFlagRequirementsNotMet(
  const std::vector<std::pair<bool, std::vector<p_flag>>> flag_conditions) {
  std::ostringstream oss;
  for (const std::pair<bool, std::vector<p_flag>> flag_condition : flag_conditions) {
    bool expected_condition = flag_condition.first;
    for (const p_flag feature_flag : flag_condition.second) {
      if (!CheckFlagCondition(expected_condition, feature_flag)) {
        // Record the feature flag if it doesn't meet the expected condition.
        oss << " flag("
            << feature_flag.second << ")="
            << (expected_condition ? "true":"false");
      }
    }
  }
  std::string skip_message = oss.str();
  if (skip_message != "") {
    GTEST_SKIP() << "Skipping test: not meet feature flag conditions:" << skip_message;
  }
}

bool CheckFlagCondition(bool expected_condition, const p_flag feature_flag) {
  if (feature_flag.first == nullptr) {
    // TODO: Add check for the legacy feature flag.
    return false;
  }
  return feature_flag.first() == expected_condition;
}

}  // namespace android::test::flag