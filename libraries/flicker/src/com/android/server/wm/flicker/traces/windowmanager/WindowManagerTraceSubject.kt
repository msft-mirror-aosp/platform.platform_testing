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

package com.android.server.wm.flicker.traces.windowmanager

import androidx.annotation.VisibleForTesting
import com.android.server.wm.flicker.assertions.Assertion
import com.android.server.wm.flicker.traces.FlickerFailureStrategy
import com.android.server.wm.flicker.traces.FlickerTraceSubject
import com.android.server.wm.flicker.traces.region.RegionTraceSubject
import com.android.server.wm.traces.common.ComponentNameMatcher
import com.android.server.wm.traces.common.IComponentMatcher
import com.android.server.wm.traces.common.region.RegionTrace
import com.android.server.wm.traces.common.windowmanager.WindowManagerTrace
import com.android.server.wm.traces.common.windowmanager.windows.WindowState
import com.google.common.truth.Fact
import com.google.common.truth.FailureMetadata
import com.google.common.truth.StandardSubjectBuilder
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory

/**
 * Truth subject for [WindowManagerTrace] objects, used to make assertions over behaviors that occur
 * throughout a whole trace.
 *
 * To make assertions over a trace it is recommended to create a subject using
 * [WindowManagerTraceSubject.assertThat](myTrace). Alternatively, it is also possible to use
 * Truth.assertAbout(WindowManagerTraceSubject.FACTORY), however it will provide less debug
 * information because it uses Truth's default [FailureStrategy].
 *
 * Example:
 * ```
 *    val trace = WindowManagerTraceParser.parseFromTrace(myTraceFile)
 *    val subject = WindowManagerTraceSubject.assertThat(trace)
 *        .contains("ValidWindow")
 *        .notContains("ImaginaryWindow")
 *        .showsAboveAppWindow("NavigationBar")
 *        .forAllEntries()
 * ```
 * Example2:
 * ```
 *    val trace = WindowManagerTraceParser.parseFromTrace(myTraceFile)
 *    val subject = WindowManagerTraceSubject.assertThat(trace) {
 *        check("Custom check") { myCustomAssertion(this) }
 *    }
 * ```
 */
class WindowManagerTraceSubject
private constructor(
    fm: FailureMetadata,
    val trace: WindowManagerTrace,
    override val parent: WindowManagerTraceSubject?,
    private val facts: Collection<Fact>
) :
    FlickerTraceSubject<WindowManagerStateSubject>(fm, trace),
    IWindowManagerSubject<WindowManagerTraceSubject, RegionTraceSubject> {

    override val selfFacts by lazy {
        val allFacts = super.selfFacts.toMutableList()
        allFacts.addAll(facts)
        allFacts
    }

    override val subjects by lazy {
        trace.entries.map { WindowManagerStateSubject.assertThat(it, this, this) }
    }

    /** {@inheritDoc} */
    override fun then(): WindowManagerTraceSubject = apply { super.then() }

    /** {@inheritDoc} */
    override fun skipUntilFirstAssertion(): WindowManagerTraceSubject = apply {
        super.skipUntilFirstAssertion()
    }

    /** {@inheritDoc} */
    override fun isEmpty(): WindowManagerTraceSubject = apply {
        check("Trace").that(trace).isEmpty()
    }

    /** {@inheritDoc} */
    override fun isNotEmpty(): WindowManagerTraceSubject = apply {
        check("Trace").that(trace).isNotEmpty()
    }

    /**
     * @return List of [WindowStateSubject]s matching [componentMatcher] in the order they
     * ```
     *      appear on the trace
     *
     * @param componentMatcher
     * ```
     * Components to search
     */
    fun windowStates(componentMatcher: IComponentMatcher): List<WindowStateSubject> =
        subjects
            .map { it.windowState { windows -> componentMatcher.windowMatchesAnyOf(windows) } }
            .filter { it.isNotEmpty }

    /**
     * @return List of [WindowStateSubject]s matching [predicate] in the order they
     * ```
     *      appear on the trace
     *
     * @param predicate
     * ```
     * To search
     */
    fun windowStates(predicate: (WindowState) -> Boolean): List<WindowStateSubject> {
        return subjects
            .map { it.windowState { window -> predicate(window) } }
            .filter { it.isNotEmpty }
    }

    /** {@inheritDoc} */
    override fun notContains(componentMatcher: IComponentMatcher): WindowManagerTraceSubject =
        notContains(componentMatcher, isOptional = false)

    /** See [notContains] */
    fun notContains(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("notContains(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.notContains(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun isAboveAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isAboveAppWindowVisible(componentMatcher, isOptional = false)

    /** See [isAboveAppWindowVisible] */
    fun isAboveAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isAboveAppWindowVisible(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isAboveAppWindowVisible(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isAboveAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isAboveAppWindowInvisible(componentMatcher, isOptional = false)

    /** See [isAboveAppWindowInvisible] */
    fun isAboveAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isAboveAppWindowInvisible(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isAboveAppWindowInvisible(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isBelowAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isBelowAppWindowVisible(componentMatcher, isOptional = false)

    /** See [isBelowAppWindowVisible] */
    fun isBelowAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isBelowAppWindowVisible(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isBelowAppWindowVisible(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isBelowAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isBelowAppWindowInvisible(componentMatcher, isOptional = false)

    /** See [isBelowAppWindowInvisible] */
    fun isBelowAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isBelowAppWindowInvisible(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isBelowAppWindowInvisible(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isNonAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isNonAppWindowVisible(componentMatcher, isOptional = false)

    /** See [isNonAppWindowVisible] */
    fun isNonAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isNonAppWindowVisible(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isNonAppWindowVisible(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isNonAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isNonAppWindowInvisible(componentMatcher, isOptional = false)

    /** See [isNonAppWindowInvisible] */
    fun isNonAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isNonAppWindowInvisible(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isNonAppWindowInvisible(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isAppWindowOnTop(componentMatcher: IComponentMatcher): WindowManagerTraceSubject =
        isAppWindowOnTop(componentMatcher, isOptional = false)

    /** See [isAppWindowOnTop] */
    fun isAppWindowOnTop(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAppWindowOnTop(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.isAppWindowOnTop(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun isAppWindowNotOnTop(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isAppWindowNotOnTop(componentMatcher, isOptional = false)

    /** See [isAppWindowNotOnTop] */
    fun isAppWindowNotOnTop(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("appWindowNotOnTop(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.isAppWindowNotOnTop(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun isAppWindowVisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isAppWindowVisible(componentMatcher, isOptional = false)

    /** See [isAppWindowVisible] */
    fun isAppWindowVisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAppWindowVisible(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.isAppWindowVisible(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun hasNoVisibleAppWindow(): WindowManagerTraceSubject =
        hasNoVisibleAppWindow(isOptional = false)

    /** See [hasNoVisibleAppWindow] */
    fun hasNoVisibleAppWindow(isOptional: Boolean): WindowManagerTraceSubject = apply {
        addAssertion("hasNoVisibleAppWindow()", isOptional) { it.hasNoVisibleAppWindow() }
    }

    /** {@inheritDoc} */
    override fun isKeyguardShowing(): WindowManagerTraceSubject =
        isKeyguardShowing(isOptional = false)

    /** See [isKeyguardShowing] */
    fun isKeyguardShowing(isOptional: Boolean): WindowManagerTraceSubject = apply {
        addAssertion("isKeyguardShowing()", isOptional) { it.isKeyguardShowing() }
    }

    /** {@inheritDoc} */
    override fun isAppSnapshotStartingWindowVisibleFor(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject =
        isAppSnapshotStartingWindowVisibleFor(componentMatcher, isOptional = false)

    /** See [isAppSnapshotStartingWindowVisibleFor] */
    fun isAppSnapshotStartingWindowVisibleFor(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "isAppSnapshotStartingWindowVisibleFor(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.isAppSnapshotStartingWindowVisibleFor(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isAppWindowInvisible(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = isAppWindowInvisible(componentMatcher, isOptional = false)

    /** See [isAppWindowInvisible] */
    fun isAppWindowInvisible(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("isAppWindowInvisible(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.isAppWindowInvisible(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun doNotOverlap(
        vararg componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = apply {
        val repr = componentMatcher.joinToString(", ") { it.toWindowIdentifier() }
        addAssertion("noWindowsOverlap($repr)") { it.doNotOverlap(*componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun isAboveWindow(
        aboveWindowComponentMatcher: IComponentMatcher,
        belowWindowComponentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = apply {
        val aboveWindowTitle = aboveWindowComponentMatcher.toWindowIdentifier()
        val belowWindowTitle = belowWindowComponentMatcher.toWindowIdentifier()
        addAssertion("$aboveWindowTitle is above $belowWindowTitle") {
            it.isAboveWindow(aboveWindowComponentMatcher, belowWindowComponentMatcher)
        }
    }

    /** See [isAppWindowInvisible] */
    override fun visibleRegion(componentMatcher: IComponentMatcher?): RegionTraceSubject {
        val regionTrace =
            RegionTrace(
                componentMatcher,
                subjects.map { it.visibleRegion(componentMatcher).regionEntry }.toTypedArray()
            )

        return RegionTraceSubject.assertThat(regionTrace, this)
    }

    /** {@inheritDoc} */
    override fun contains(componentMatcher: IComponentMatcher): WindowManagerTraceSubject =
        contains(componentMatcher, isOptional = false)

    /** See [contains] */
    fun contains(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("contains(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.contains(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun containsAboveAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = containsAboveAppWindow(componentMatcher, isOptional = false)

    /** See [containsAboveAppWindow] */
    fun containsAboveAppWindow(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "containsAboveAppWindow(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.containsAboveAppWindow(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun containsAppWindow(componentMatcher: IComponentMatcher): WindowManagerTraceSubject =
        containsAppWindow(componentMatcher, isOptional = false)

    /** See [containsAppWindow] */
    fun containsAppWindow(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("containsAppWindow(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.containsAboveAppWindow(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun containsBelowAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = containsBelowAppWindow(componentMatcher, isOptional = false)

    /** See [containsBelowAppWindow] */
    fun containsBelowAppWindow(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion(
            "containsBelowAppWindows(${componentMatcher.toWindowIdentifier()})",
            isOptional
        ) { it.containsBelowAppWindow(componentMatcher) }
    }

    /** {@inheritDoc} */
    override fun containsNonAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = containsNonAppWindow(componentMatcher, isOptional = false)

    /** See [containsNonAppWindow] */
    fun containsNonAppWindow(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("containsNonAppWindow(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.containsNonAppWindow(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun isHomeActivityInvisible(): WindowManagerTraceSubject =
        isHomeActivityInvisible(isOptional = false)

    /** See [isHomeActivityInvisible] */
    fun isHomeActivityInvisible(isOptional: Boolean): WindowManagerTraceSubject = apply {
        addAssertion("isHomeActivityInvisible", isOptional) { it.isHomeActivityInvisible() }
    }

    /** {@inheritDoc} */
    override fun isHomeActivityVisible(): WindowManagerTraceSubject =
        isHomeActivityVisible(isOptional = false)

    /** See [isHomeActivityVisible] */
    fun isHomeActivityVisible(isOptional: Boolean): WindowManagerTraceSubject = apply {
        addAssertion("isHomeActivityVisible", isOptional) { it.isHomeActivityVisible() }
    }

    /** {@inheritDoc} */
    override fun hasRotation(rotation: Int, displayId: Int): WindowManagerTraceSubject =
        hasRotation(rotation, displayId, isOptional = false)

    /** See [hasRotation] */
    fun hasRotation(rotation: Int, displayId: Int, isOptional: Boolean): WindowManagerTraceSubject =
        apply {
            addAssertion("hasRotation($rotation, display=$displayId)", isOptional) {
                it.hasRotation(rotation, displayId)
            }
        }

    /** {@inheritDoc} */
    override fun isNotPinned(componentMatcher: IComponentMatcher): WindowManagerTraceSubject =
        isNotPinned(componentMatcher, isOptional = false)

    /** See [isNotPinned] */
    fun isNotPinned(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("isNotPinned(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.isNotPinned(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun isPinned(componentMatcher: IComponentMatcher): WindowManagerTraceSubject =
        isPinned(componentMatcher, isOptional = false)

    /** See [isPinned] */
    fun isPinned(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("isPinned(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.isPinned(componentMatcher)
        }
    }

    /** {@inheritDoc} */
    override fun isRecentsActivityInvisible(): WindowManagerTraceSubject =
        isRecentsActivityInvisible(isOptional = false)

    /** See [isRecentsActivityInvisible] */
    fun isRecentsActivityInvisible(isOptional: Boolean): WindowManagerTraceSubject = apply {
        addAssertion("isRecentsActivityInvisible", isOptional) { it.isRecentsActivityInvisible() }
    }

    /** {@inheritDoc} */
    override fun isRecentsActivityVisible(): WindowManagerTraceSubject =
        isRecentsActivityVisible(isOptional = false)

    /** See [isRecentsActivityVisible] */
    fun isRecentsActivityVisible(isOptional: Boolean): WindowManagerTraceSubject = apply {
        addAssertion("isRecentsActivityVisible", isOptional) { it.isRecentsActivityVisible() }
    }

    @VisibleForTesting
    override fun isValid(): WindowManagerTraceSubject = apply {
        addAssertion("isValid") { it.isValid() }
    }

    /** {@inheritDoc} */
    override fun notContainsAppWindow(
        componentMatcher: IComponentMatcher
    ): WindowManagerTraceSubject = notContainsAppWindow(componentMatcher, isOptional = false)

    /** See [notContainsAppWindow] */
    fun notContainsAppWindow(
        componentMatcher: IComponentMatcher,
        isOptional: Boolean
    ): WindowManagerTraceSubject = apply {
        addAssertion("notContainsAppWindow(${componentMatcher.toWindowIdentifier()})", isOptional) {
            it.notContainsAppWindow(componentMatcher)
        }
    }

    /** Checks that all visible layers are shown for more than one consecutive entry */
    fun visibleWindowsShownMoreThanOneConsecutiveEntry(
        ignoreWindows: List<ComponentNameMatcher> =
            listOf(ComponentNameMatcher.SPLASH_SCREEN, ComponentNameMatcher.SNAPSHOT)
    ): WindowManagerTraceSubject = apply {
        visibleEntriesShownMoreThanOneConsecutiveTime { subject ->
            subject.wmState.windowStates
                .filter { it.isVisible }
                .filter { ignoreWindows.none { windowName -> windowName.windowMatchesAnyOf(it) } }
                .map { it.name }
                .toSet()
        }
    }

    /** Executes a custom [assertion] on the current subject */
    @JvmOverloads
    operator fun invoke(
        name: String,
        isOptional: Boolean = false,
        assertion: Assertion<WindowManagerStateSubject>
    ): WindowManagerTraceSubject = apply { addAssertion(name, isOptional, assertion) }

    /** Run the assertions for all trace entries within the specified time range */
    fun forRange(startTime: Long, endTime: Long) {
        val subjectsInRange = subjects.filter { it.wmState.timestamp in startTime..endTime }
        assertionsChecker.test(subjectsInRange)
    }

    /**
     * User-defined entry point for the trace entry with [timestamp]
     *
     * @param timestamp of the entry
     */
    fun entry(timestamp: Long): WindowManagerStateSubject =
        subjects.first { it.wmState.timestamp == timestamp }

    companion object {
        /** Boilerplate Subject.Factory for WmTraceSubject */
        private fun getFactory(
            parent: WindowManagerTraceSubject?,
            facts: Collection<Fact> = emptyList()
        ): Factory<Subject, WindowManagerTrace> = Factory { fm, subject ->
            WindowManagerTraceSubject(fm, subject, parent, facts)
        }

        /**
         * Creates a [WindowManagerTraceSubject] representing a WindowManager trace, which can be
         * used to make assertions.
         *
         * @param trace WindowManager trace
         */
        @JvmStatic
        @JvmOverloads
        fun assertThat(
            trace: WindowManagerTrace,
            parent: WindowManagerTraceSubject? = null,
            facts: Collection<Fact> = emptyList()
        ): WindowManagerTraceSubject {
            val strategy = FlickerFailureStrategy()
            val subject =
                StandardSubjectBuilder.forCustomFailureStrategy(strategy)
                    .about(getFactory(parent, facts))
                    .that(trace) as WindowManagerTraceSubject
            strategy.init(subject)
            return subject
        }

        /** Static method for getting the subject factory (for use with assertAbout()) */
        @JvmStatic
        fun entries(
            parent: WindowManagerTraceSubject?,
            facts: Collection<Fact>
        ): Factory<Subject, WindowManagerTrace> = getFactory(parent, facts)
    }
}
