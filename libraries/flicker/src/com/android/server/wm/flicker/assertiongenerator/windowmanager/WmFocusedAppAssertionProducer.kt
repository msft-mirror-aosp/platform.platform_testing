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

import com.android.server.wm.flicker.Utils
import com.android.server.wm.flicker.assertiongenerator.common.Assertion
import com.android.server.wm.flicker.assertiongenerator.common.TraceContent
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerStateSubject

class WmFocusedAppAssertionProducer : WmAssertionProducer {
    var previousFocusedApp: String? = null
    var initializedAssertion: Boolean = false
    var assertion = WmAssertion()

    override fun produce(traceContents: List<TraceContent>): List<Assertion> {
        val wmTraceContents = traceContents.filterIsInstance<WmTraceContent>()
        if (wmTraceContents.isEmpty()) {
            return listOf()
        }
        val traceContent = wmTraceContents[0]
        val traceLifecycle = traceContent.traceLifecycle
        val focusedApps = traceLifecycle.focusedApps
        focusedApps.map { focusedApp -> produceAssertionForFocusedApp(focusedApp) }
        assertion.name = "WmFocusedApp"
        return listOf(assertion)
    }

    private fun addToAssertionString(assertionStrToAdd: String, focusedApp: String) {
        if (initializedAssertion) {
            assertion.assertionString += ".then()"
        }
        assertion.assertionString += ".$assertionStrToAdd(\"$focusedApp\")"
    }

    private fun addAssertionFor(focusedApp: String) {
        val assertionStrToAdd = "isFocusedApp"
        val assertionFunction = WindowManagerStateSubject::isFocusedApp
        val componentMatcher = Utils.componentNameMatcherFromName(focusedApp)
        componentMatcher ?: run { throw RuntimeException("$focusedApp is not a component") }
        assertion.assertionsChecker.add("$assertionStrToAdd($focusedApp)", isOptional = false) {
            assertionFunction(it, focusedApp)
        }
        addToAssertionString(assertionStrToAdd, focusedApp)
    }

    private fun setPrevious(focusedApp: String?) {
        this.previousFocusedApp = focusedApp
        initializedAssertion = true
    }

    private fun produceAssertionForFocusedApp(focusedApp: String) {
        val previousFocusedApp = previousFocusedApp
        if (!initializedAssertion || previousFocusedApp != focusedApp) {
            addAssertionFor(focusedApp)
        }
        setPrevious(focusedApp)
    }
}
