/*
 * Copyright (C) 2024 The Android Open Source Project
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

#include <gtest/gtest.h>

#include <cstdlib>

#include "include/igt_test_helper.h"

namespace igt {
namespace {

/**
 * TEST: kms plane lowres
 * Category: Display
 * Description: Test atomic mode setting with a plane by switching between high
 *              and low resolutions
 * Functionality: plane, tiling
 * Mega feature: General Display Features
 * Test category: functionality test
 */

class KmsPlaneLowresTests : public ::testing::TestWithParam<IgtSubtestParams>,
                            public IgtTestHelper {
public:
  KmsPlaneLowresTests() : IgtTestHelper("kms_plane_lowres") {}
};

constexpr std::string_view kDescription =
    "Tests the visibility of the planes when switching between high and low "
    "resolution";
constexpr std::string_view kRationale =
    "revealing issues with memory management or the interaction between "
    "scaling and tiling";

IgtSubtestParams subtests[] = {
    // Fundamental Validation tests.
    {.name = "tiling-none", .desc = kDescription, .rationale = kRationale},
    {.name = "tiling-x", .desc = kDescription, .rationale = kRationale},
    {.name = "tiling-y", .desc = kDescription, .rationale = kRationale},
    {.name = "tiling-4", .desc = kDescription, .rationale = kRationale},

};

TEST_P(KmsPlaneLowresTests, RunSubTests) { runSubTest(GetParam()); }

INSTANTIATE_TEST_SUITE_P(KmsPlaneLowresTests, KmsPlaneLowresTests,
                         ::testing::ValuesIn(subtests),
                         IgtTestHelper::generateGTestName);

} // namespace
} // namespace igt
