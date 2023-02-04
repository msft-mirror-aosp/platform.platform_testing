/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.server.wm.flicker.assertiongenerator.windowmanager

import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertions.AssertionsChecker
import com.android.server.wm.flicker.assertions.FlickerSubject
import com.android.server.wm.flicker.service.assertors.ComponentTypeMatcher
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.ITrace
import com.android.server.wm.traces.common.ITraceEntry
import com.android.server.wm.traces.common.transition.Transition
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace

class WmAssertion : Assertion {
    override val assertionsChecker: AssertionsChecker<WindowManagerStateSubject> =
        AssertionsChecker()
    override var name: String = ""
    override var assertionString: String = ""

    val needsInitialization: Boolean
        get() {
            if (lateinitComponentMatchers.size == 0) {
                return false
            }
            return true
        }

    val lateinitComponentMatchers: MutableList<ComponentTypeMatcher> = mutableListOf()

    private fun initializeComponentMatchers(transition: Transition) {
        lateinitComponentMatchers.forEach { componentMatcher ->
            componentMatcher.initialize(transition)
        }
    }

    override fun execute(newTrace: ITrace<out ITraceEntry>, transition: Transition) {
        require(newTrace is WindowManagerTrace) {
            "Requires a trace of type 'WindowManagerTrace' to execute WmAssertion."
        }
        execute(WindowManagerTraceSubject(newTrace), transition)
    }

    override fun execute(
        traceSubject: FlickerTraceSubject<out FlickerSubject>,
        transition: Transition
    ) {
        require(traceSubject is WindowManagerTraceSubject) {
            "Requires a traceSubject of type 'WindowManagerTraceSubject' " +
                "to execute WmAssertion."
        }
        if (needsInitialization && transition == Transition.emptyTransition()) {
            throw RuntimeException(
                "At least one assertion component matcher needs " +
                    "initialization, but the passed transition is empty"
            )
        }
        transition.run { initializeComponentMatchers(transition) }
        assertionsChecker.assertChanges(traceSubject.subjects)
    }

    override fun toString(): String {
        return assertionString
    }

    override fun toString(newTrace: String): String {
        return "assertThat($newTrace)$assertionString"
    }

    override fun isEqual(other: Any?): Boolean {
        if (other !is WmAssertion) {
            return false
        }
        return assertionsChecker.isEqual(other.assertionsChecker) &&
            name == other.name &&
            assertionString == other.assertionString
    }
}
