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

package android.tools.common.traces.events

import android.tools.CleanFlickerEnvironmentRule
import android.tools.common.CrossPlatform
import com.google.common.truth.Truth
import org.junit.ClassRule
import org.junit.Test

/** Tests for [CujTrace]. Run with `atest FlickerLibTest:CujTraceTest` */
class CujTraceTest {
    @Test
    fun canCreateFromArrayOfCujEvents() {
        val trace =
            CujTrace.from(
                arrayOf(
                    createCujEvent(
                        1,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_BEGIN_TAG
                    ),
                    createCujEvent(
                        2,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_END_TAG
                    ),
                )
            )

        Truth.assertThat(trace.entries).hasLength(1)
        Truth.assertThat(trace.entries[0].cuj).isEqualTo(CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL)
        Truth.assertThat(trace.entries[0].startTimestamp.unixNanos).isEqualTo(1)
        Truth.assertThat(trace.entries[0].endTimestamp.unixNanos).isEqualTo(2)
        Truth.assertThat(trace.entries[0].canceled).isFalse()
    }

    @Test
    fun canCreateCanceledCujsFromArrayOfCujEvents() {
        val trace =
            CujTrace.from(
                arrayOf(
                    createCujEvent(
                        1,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_BEGIN_TAG
                    ),
                    createCujEvent(
                        2,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_CANCEL_TAG
                    ),
                )
            )

        Truth.assertThat(trace.entries).hasLength(1)
        Truth.assertThat(trace.entries[0].cuj).isEqualTo(CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL)
        Truth.assertThat(trace.entries[0].startTimestamp.unixNanos).isEqualTo(1)
        Truth.assertThat(trace.entries[0].endTimestamp.unixNanos).isEqualTo(2)
        Truth.assertThat(trace.entries[0].canceled).isTrue()
    }

    @Test
    fun canHandleIncompleteCujs() {
        val trace =
            CujTrace.from(
                arrayOf(
                    createCujEvent(
                        1,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_CANCEL_TAG
                    ),
                    createCujEvent(
                        2,
                        CujType.CUJ_BIOMETRIC_PROMPT_TRANSITION,
                        CujEvent.JANK_CUJ_END_TAG
                    ),
                    createCujEvent(
                        3,
                        CujType.CUJ_LAUNCHER_APP_CLOSE_TO_HOME,
                        CujEvent.JANK_CUJ_BEGIN_TAG
                    ),
                )
            )

        Truth.assertThat(trace.entries).hasLength(0)
    }

    @Test
    fun canHandleOutOfOrderEntries() {
        val trace =
            CujTrace.from(
                arrayOf(
                    createCujEvent(
                        2,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_END_TAG
                    ),
                    createCujEvent(
                        1,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_BEGIN_TAG
                    ),
                )
            )

        Truth.assertThat(trace.entries).hasLength(1)
        Truth.assertThat(trace.entries[0].cuj).isEqualTo(CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL)
        Truth.assertThat(trace.entries[0].startTimestamp.unixNanos).isEqualTo(1)
        Truth.assertThat(trace.entries[0].endTimestamp.unixNanos).isEqualTo(2)
        Truth.assertThat(trace.entries[0].canceled).isFalse()
    }

    @Test
    fun canHandleMissingStartAndEnds() {
        val trace =
            CujTrace.from(
                arrayOf(
                    createCujEvent(
                        1,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_END_TAG
                    ),
                    createCujEvent(
                        2,
                        CujType.CUJ_LAUNCHER_ALL_APPS_SCROLL,
                        CujEvent.JANK_CUJ_BEGIN_TAG
                    ),
                )
            )

        Truth.assertThat(trace.entries).hasLength(0)
    }

    private fun createCujEvent(
        timestamp: Long,
        cuj: CujType,
        type: String,
        tag: String? = null
    ): CujEvent {
        return CujEvent(
            CrossPlatform.timestamp.from(unixNanos = timestamp),
            cuj,
            0,
            "root",
            0,
            type,
            tag,
        )
    }

    companion object {
        @ClassRule @JvmField val cleanFlickerEnvironmentRule = CleanFlickerEnvironmentRule()
    }
}
