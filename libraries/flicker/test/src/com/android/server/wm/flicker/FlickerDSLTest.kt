/*
 * Copyright (C) 2020 The Android Open Source Project
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
import com.android.server.wm.flicker.common.AssertionResult
import com.android.server.wm.flicker.dsl.FlickerBuilder
import com.android.server.wm.flicker.dsl.runWithFlicker
import com.android.server.wm.flicker.traces.windowmanager.WmTraceSubject
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

    @Test
    fun preventDuplicatedTag() {
        val builder = FlickerBuilder(instrumentation)

        try {
            runWithFlicker(builder) {
                transitions {
                    this.createTag("myTag")
                    this.withTag("myTag") {
                        this.device.pressHome()
                    }
                }
            }
            Assert.fail("Should not have allowed duplicated tags")
        } catch (e: Exception) {
            Truth.assertWithMessage("Did not prevent duplicated tag use")
                .that(e.message)
                .contains("Tag myTag has already been used")
        }
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
                .that(e.message)
                .contains("The test tag inv lid can not contain spaces")
        }
    }

    private fun defaultAssertion(trace: WmTraceSubject): WmTraceSubject {
        return trace("Has dump") {
            AssertionResult("Has dump") { it.windows.isNotEmpty() }
        }
    }

    @Test
    fun assertCreatedTags() {
        val builder = FlickerBuilder(instrumentation)

        val myTag = "myTag"
        runWithFlicker(builder) {
            transitions {
                this.createTag(myTag)
                device.pressHome()
            }
            assertions {
                windowManagerTrace {
                    tag(myTag) { defaultAssertion(this) }

                    start { defaultAssertion(this) }

                    end { defaultAssertion(this) }

                    tag("invalid") {
                        this.failWithMessage("`Invalid` tag was not created, so it should not " +
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
                        tag("tag") { defaultAssertion(this) }
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
        builder.transitions { throw RuntimeException("Crashed transition") }
        val flicker = builder.build()
        try {
            flicker.execute()
            Assert.fail("Should have raised an exception with message $exceptionMessage")
        } catch (e: Exception) {
            Truth.assertWithMessage("The test did not store the last exception")
                    .that(flicker.error?.message)
                    .contains(exceptionMessage)
            Truth.assertWithMessage("Test exception does not contain original crash message")
                    .that(e.message)
                    .contains(exceptionMessage)
        }
    }
}
