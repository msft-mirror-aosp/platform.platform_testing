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

package com.android.server.wm.flicker.service.extractors

import com.android.server.wm.flicker.helpers.MILLISECOND_AS_NANOSECONDS
import com.android.server.wm.flicker.io.IReader
import com.android.server.wm.flicker.service.extractors.TransitionTransforms.inCujRangeFilter
import com.android.server.wm.flicker.service.extractors.TransitionTransforms.mergeTrampolineTransitions
import com.android.server.wm.traces.common.events.Cuj
import com.android.server.wm.traces.common.transition.Transition

typealias TransitionsTransform =
    (transitions: List<Transition>, cujEntry: Cuj, reader: IReader) -> List<Transition>

class TransitionMatcher(
    private val mainTransform: TransitionsTransform,
    private val associatedTransitionRequired: Boolean = true,
    // Transformations applied, in order, to all transitions the reader returns to end up with the
    // targeted transition.
    private val transforms: List<TransitionsTransform> =
        listOf(mainTransform, inCujRangeFilter, mergeTrampolineTransitions)
) : ITransitionMatcher {
    override fun getTransition(cujEntry: Cuj, reader: IReader): Transition? {
        val transitionsTrace = reader.readTransitionsTrace() ?: error("Missing transitions trace")

        val completeTransitions = transitionsTrace.entries.filter { !it.isIncomplete }

        val matchedTransitions =
            transforms.fold(completeTransitions) { transitions, transform ->
                transform(transitions, cujEntry, reader)
            }

        require(!associatedTransitionRequired || matchedTransitions.isNotEmpty()) {
            "Required an associated transition for " +
                "${cujEntry.cuj.name}(${cujEntry.startTimestamp},${cujEntry.endTimestamp}) " +
                "but no transition left after all filters from: " +
                "[\n${transitionsTrace.entries.joinToString(",\n").prependIndent()}\n]!"
        }

        require(!associatedTransitionRequired || matchedTransitions.size == 1) {
            "Got too many associated transitions expected only 1."
        }

        return if (matchedTransitions.isNotEmpty()) matchedTransitions[0] else null
    }
}

object TransitionTransforms {
    val inCujRangeFilter: TransitionsTransform = { transitions, cujEntry, _ ->
        transitions.filter { transition ->
            val transitionSentWithinCujTags =
                cujEntry.startTimestamp <= transition.sendTime &&
                    transition.sendTime <= cujEntry.endTimestamp

            // TODO: This threshold should be made more robust. Can fail to match on slower devices.
            val toleranceNanos = 50 * MILLISECOND_AS_NANOSECONDS
            val transitionSentJustBeforeCujStart =
                cujEntry.startTimestamp - toleranceNanos <= transition.sendTime &&
                    transition.sendTime <= cujEntry.startTimestamp

            return@filter transitionSentWithinCujTags || transitionSentJustBeforeCujStart
        }
    }

    val mergeTrampolineTransitions: TransitionsTransform = { transitions, _, reader ->
        require(transitions.size <= 2)
        if (
            transitions.size == 2 &&
                isTrampolinedOpenTransition(transitions[0], transitions[1], reader)
        ) {
            // Remove the trampoline transition
            listOf(transitions[0])
        } else {
            transitions
        }
    }

    private fun isTrampolinedOpenTransition(
        firstTransition: Transition,
        secondTransition: Transition,
        reader: IReader
    ): Boolean {
        val candidateTaskLayers =
            firstTransition.changes
                .filter {
                    it.transitMode == Transition.Companion.Type.OPEN ||
                        it.transitMode == Transition.Companion.Type.TO_FRONT
                }
                .map { it.layerId }
        if (candidateTaskLayers.isEmpty()) {
            return false
        }

        require(candidateTaskLayers.size == 1) {
            "Unhandled case (more than 1 task candidate) in isTrampolinedOpenTransition()"
        }

        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val layers =
            layersTrace.entries.flatMap { it.flattenedLayers.asList() }.distinctBy { it.id }

        val candidateTaskLayerId = candidateTaskLayers[0]
        val candidateTaskLayer = layers.first { it.id == candidateTaskLayerId }
        if (!candidateTaskLayer.name.contains("Task")) {
            return false
        }

        val candidateTrampolinedActivities =
            secondTransition.changes
                .filter { it.transitMode == Transition.Companion.Type.CLOSE }
                .map { it.layerId }
        val candidateTargetActivities =
            secondTransition.changes
                .filter {
                    it.transitMode == Transition.Companion.Type.OPEN ||
                        it.transitMode == Transition.Companion.Type.TO_FRONT
                }
                .map { it.layerId }
        if (candidateTrampolinedActivities.isEmpty() || candidateTargetActivities.isEmpty()) {
            return false
        }

        require(candidateTargetActivities.size == 1) {
            "Unhandled case (more than 1 trampolined candidate) in " +
                "isTrampolinedOpenTransition()"
        }
        require(candidateTargetActivities.size == 1) {
            "Unhandled case (more than 1 target candidate) in isTrampolinedOpenTransition()"
        }

        val candidateTrampolinedActivityId = candidateTargetActivities[0]
        val candidateTrampolinedActivity = layers.first { it.id == candidateTrampolinedActivityId }
        if (candidateTrampolinedActivity.parent?.id != candidateTaskLayerId) {
            return false
        }

        val candidateTargetActivityId = candidateTargetActivities[0]
        val candidateTargetActivity = layers.first { it.id == candidateTargetActivityId }
        if (candidateTargetActivity.parent?.id != candidateTaskLayerId) {
            return false
        }

        return true
    }
}
