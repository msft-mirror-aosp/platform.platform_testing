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

package com.android.server.wm.flicker.service.assertors

import com.android.server.wm.flicker.traces.layers.LayersTraceSubject
import com.android.server.wm.flicker.traces.windowmanager.WindowManagerTraceSubject
import com.android.server.wm.traces.common.service.AssertionInvocationGroup
import com.android.server.wm.traces.common.service.AssertionInvocationGroup.NON_BLOCKING
import com.android.server.wm.traces.common.service.FlickerServiceScenario
import com.android.server.wm.traces.common.service.ScenarioInstance
import com.android.server.wm.traces.common.transition.Transition

/** Base class for a FaaS assertion */
abstract class BaseAssertionBuilder {
    internal var invocationGroup: AssertionInvocationGroup = NON_BLOCKING

    // Assertion name
    open val name: String = this::class.java.simpleName

    /** Notifies the assertion of both trace contents */
    protected open fun doSetup(
        transition: Transition,
        wmSubject: WindowManagerTraceSubject?,
        layerSubject: LayersTraceSubject?
    ) {
        // Does nothing, unless overridden
    }

    /**
     * Evaluates assertions that require only WM traces. NOTE: Will not run if WM trace is not
     * available.
     */
    protected open fun doEvaluate(transition: Transition, wmSubject: WindowManagerTraceSubject) {
        // Does nothing, unless overridden
    }

    /**
     * Evaluates assertions that require only SF traces. NOTE: Will not run if layers trace is not
     * available.
     */
    protected open fun doEvaluate(transition: Transition, layerSubject: LayersTraceSubject) {
        // Does nothing, unless overridden
    }

    /**
     * Evaluate the assertion on a transition [Tag] in a [WindowManagerTraceSubject] and
     * [LayersTraceSubject]
     *
     * @param tag a list with all [TransitionTag]s
     * @param wmSubject Window Manager trace subject
     * @param layerSubject Surface Flinger trace subject
     */
    fun evaluate(
        scenarioInstance: ScenarioInstance,
        wmSubject: WindowManagerTraceSubject?,
        layerSubject: LayersTraceSubject?,
        scenario: FlickerServiceScenario
    ): AssertionResult {
        var assertionError: Throwable? = null
        try {
            doSetup(scenarioInstance.associatedTransition, wmSubject, layerSubject)
            if (wmSubject !== null) {
                doEvaluate(scenarioInstance.associatedTransition, wmSubject)
            }
            if (layerSubject !== null) {
                doEvaluate(scenarioInstance.associatedTransition, layerSubject)
            }
        } catch (e: Throwable) {
            assertionError = e
        }
        return AssertionResult(name, scenario, invocationGroup, assertionError)
    }

    infix fun runAs(invocationGroup: AssertionInvocationGroup): BaseAssertionBuilder {
        this.invocationGroup = invocationGroup
        return this
    }

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }
        // Ensure both assertions are instances of the same class.
        return this::class == other::class
    }

    override fun hashCode(): Int {
        var result = invocationGroup.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}
