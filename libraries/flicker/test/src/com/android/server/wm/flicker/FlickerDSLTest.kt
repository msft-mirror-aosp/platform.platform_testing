/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.server.wm.flicker

import androidx.test.platform.app.InstrumentationRegistry
import com.android.server.wm.flicker.dsl.AssertionBlockBuilder
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.dsl.runWithFlicker
import com.android.server.wm.flicker.traces.eventlog.EventLogSubject
import com.android.server.wm.flicker.traces.eventlog.FocusEventSubject
import com.android.server.wm.flicker.traces.layers.LayerTraceEntrySubject
import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [Flicker] and [FlickerBuilder] tests.
 *
 * To run this test: `atest FlickerLibTest:FlickerDSLTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class FlickerDSLTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val TAG = "tag"

    @Test
    fun checkExpectedSubjectClass() {
        val flicker = FlickerBuilder(instrumentation).apply {
            assertions {
                presubmit {
                    windowManagerTrace {
                        start("start") { }
                        end("end") { }
                        tag(TAG) { }
                        all("all") { }
                    }
                    layersTrace {
                        start("start") { }
                        end("end") { }
                        tag(TAG) { }
                        all("all") { }
                    }
                    eventLog {
                        start("start") { }
                        end("end") { }
                        tag(TAG) { }
                        all("all") { }
                    }
                }
            }
        }.build()

        val expectedClasses = listOf(
            WindowManagerStateSubject::class,
            WindowManagerStateSubject::class,
            WindowManagerStateSubject::class,
            WindowManagerTraceSubject::class,
            LayerTraceEntrySubject::class,
            LayerTraceEntrySubject::class,
            LayerTraceEntrySubject::class,
            LayersTraceSubject::class,
            FocusEventSubject::class,
            FocusEventSubject::class,
            FocusEventSubject::class,
            EventLogSubject::class
        )

        val actualClasses = flicker.assertions.map { it.expectedSubjectClass }

        Truth.assertWithMessage("Unexpected subject type")
            .that(actualClasses)
            .isEqualTo(expectedClasses)
    }

    @Test
    fun supportDuplicatedTag() {
        val builder = FlickerBuilder(instrumentation)
        var count = 0

        runWithFlicker(builder) {
            transitions {
                this.createTag(TAG)
                this.withTag(TAG) {
                    this.device.pressHome()
                }
            }
            assertions {
                presubmit {
                    windowManagerTrace {
                        tag(TAG) {
                            count++
                        }
                    }
                }
            }
        }

        Truth.assertWithMessage("Should have asserted $TAG 2x")
            .that(count)
            .isEqualTo(2)
    }

    @Test
    fun preventInvalidTagNames() {
        val builder = FlickerBuilder(instrumentation)

        try {
            runWithFlicker(builder) {
                transitions {
                    this.createTag("inv lid")
                }
            }
            Assert.fail("Should not have allowed invalid tag name")
        } catch (e: Exception) {
            Truth.assertWithMessage("Did not validate tag name")
                .that(e.cause?.message)
                .contains("The test tag inv lid can not contain spaces")
        }
    }

    @Test
    fun assertCreatedTags() {
        val builder = FlickerBuilder(instrumentation)

        runWithFlicker(builder) {
            transitions {
                this.createTag(TAG)
                device.pressHome()
            }
            assertions {
                windowManagerTrace {
                    tag(TAG) {
                        this.isNotEmpty()
                    }

                    start("start") {
                        this.isNotEmpty()
                    }

                    end("end") {
                        this.isNotEmpty()
                    }

                    tag("invalid", "invalid") {
                        fail("`Invalid` tag was not created, so it should not " +
                            "have been asserted")
                    }
                }
            }
        }
    }

    @Test
    fun detectEmptyResults() {
        try {
            val builder = FlickerBuilder(instrumentation)
            runWithFlicker(builder) {
                assertions {
                    windowManagerTrace {
                        tag(TAG) {
                            this.isNotEmpty()
                        }
                    }
                }
            }
            Assert.fail("Should not have allowed empty transition")
        } catch (e: Exception) {
            Truth.assertWithMessage("Flicker did not warn of empty transitions")
                .that(e.message)
                .contains("A flicker test must include transitions to run")
        }
    }

    @Test
    fun detectCrashedTransition() {
        val exceptionMessage = "Crashed transition"
        val builder = FlickerBuilder(instrumentation)
        builder.transitions { error("Crashed transition") }
        val flicker = builder.build()
        try {
            flicker.execute()
            Assert.fail("Should have raised an exception with message $exceptionMessage")
        } catch (e: Throwable) {
            Truth.assertWithMessage("Incorrect exception message")
                .that(e.message)
                .contains("Unable to execute transition")
            Truth.assertWithMessage("Test exception does not contain original crash message")
                .that(e.cause?.message)
                .contains(exceptionMessage)
        }
    }

    private fun detectFailedAssertion(assertions: AssertionBlockBuilder.() -> Any): Throwable {
        val builder = FlickerBuilder(instrumentation)
        return assertThrows(AssertionError::class.java) {
            runWithFlicker(builder) {
                transitions {
                    createTag(TAG)
                    device.pressHome()
                }
                assertions {
                    assertions()
                }
            }
        }
    }

    @Test
    fun detectFailedWMAssertion_All() {
        val error = detectFailedAssertion {
            windowManagerTrace {
                all("fail") { fail("Correct error") }
                all("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedWMAssertion_Start() {
        val error = detectFailedAssertion {
            windowManagerTrace {
                start("fail") { fail("Correct error") }
                start("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedWMAssertion_End() {
        val error = detectFailedAssertion {
            windowManagerTrace {
                end("fail") { fail("Correct error") }
                end("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedWMAssertion_Tag() {
        val error = detectFailedAssertion {
            windowManagerTrace {
                tag(TAG) { fail("Correct error") }
                tag(TAG, enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedLayersAssertion_All() {
        val error = detectFailedAssertion {
            layersTrace {
                all("fail") { fail("Correct error") }
                all("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedLayersAssertion_Start() {
        val error = detectFailedAssertion {
            layersTrace {
                start("fail") { fail("Correct error") }
                start("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedLayersAssertion_End() {
        val error = detectFailedAssertion {
            layersTrace {
                end("fail") { fail("Correct error") }
                end("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedLayersAssertion_Tag() {
        val error = detectFailedAssertion {
            layersTrace {
                tag(TAG) { fail("Correct error") }
                tag(TAG, enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }

    @Test
    fun detectFailedEventLogAssertion_All() {
        val error = detectFailedAssertion {
            eventLog {
                all("fail") { fail("Correct error") }
                all("ignored", enabled = false) { fail("Ignored error") }
            }
        }
        assertFailure(error).hasMessageThat().contains("Correct error")
        assertFailure(error).hasMessageThat().doesNotContain("Ignored error")
    }
}
