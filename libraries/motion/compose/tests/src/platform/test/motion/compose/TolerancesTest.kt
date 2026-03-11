/*
 * Copyright (C) 2026 The Android Open Source Project
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

package platform.test.motion.compose

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.MotionTestRule
import platform.test.motion.compose.ComposeRecordingSpec.Companion.untilIdle
import platform.test.motion.testing.createGoldenPathManager

@RunWith(AndroidJUnit4::class)
class TolerancesTest {
    private val pathManager =
        createGoldenPathManager("platform_testing/libraries/motion/compose/tests/goldens")
    val testScope = TestScope()

    @get:Rule(order = 0)
    val composeRule = createComposeRule(testScope.coroutineContext + Dispatchers.Main)
    @get:Rule(order = 1)
    val motionRule = MotionTestRule(ComposeToolkit(composeRule, testScope), pathManager)

    @Test
    fun captureSize_lowDensityWithTolerance_matchesHighAndLowDensityRecordings() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { play -> ContentUnderTest(play, Density(1f)) },
                    recordingSpec =
                        untilIdle {
                            feature(
                                hasTestTag("foo"),
                                ComposeFeatureCaptures.positionInRoot.withTolerance(
                                    DpOffset(1.dp, 1.dp)
                                ),
                            )
                        },
                )

            // The motion was recorded with a tolerance of 1.dp, thus the high density recording
            // must match too.
            assertThat(motion).timeSeriesMatchesGolden("tolerance_high_density")
            assertThat(motion).timeSeriesMatchesGolden("tolerance_low_density")
        }

    @Test
    fun captureSize_highDensityNoTolerance_matchesHighDensityRecordingOnly() =
        motionRule.runTest {
            val motion =
                recordMotion(
                    content = { play -> ContentUnderTest(play, Density(4.5f)) },
                    recordingSpec =
                        untilIdle {
                            feature(hasTestTag("foo"), ComposeFeatureCaptures.positionInRoot)
                        },
                )

            Assert.assertThrows(AssertionError::class.java) {
                // The motion was recorded with a tolerance of zero, thus the low density recording
                // must not match.
                assertThat(motion).timeSeriesMatchesGolden("tolerance_low_density")
            }
            assertThat(motion).timeSeriesMatchesGolden("tolerance_high_density")
        }

    @Composable
    fun ContentUnderTest(play: Boolean, density: Density) {
        val fixedConfiguration = remember(density) { FixedConfiguration(density) }
        FixedConfigurationProvider(fixedConfiguration) {
            val offset by animateDpAsState(if (play) 90.dp else 0.dp)
            Box(
                modifier =
                    Modifier.offset(x = offset).testTag("foo").size(10.dp).background(Color.Red)
            )
        }
    }
}
