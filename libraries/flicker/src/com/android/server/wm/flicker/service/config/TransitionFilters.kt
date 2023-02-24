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

package com.android.server.wm.flicker.service.config

import com.android.server.wm.flicker.service.extractors.TransitionsTransform
import com.android.server.wm.traces.common.component.matchers.ComponentNameMatcher
import com.android.server.wm.traces.common.transition.Transition

object TransitionFilters {
    val OPEN_APP_TRANSITION_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { t ->
            t.changes.any {
                it.transitMode == Transition.Companion.Type.OPEN || // cold launch
                it.transitMode == Transition.Companion.Type.TO_FRONT // warm launch
            }
        }
    }

    val CLOSE_APP_TO_LAUNCHER_FILTER: TransitionsTransform = { ts, _, reader ->
        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val layers =
            layersTrace.entries.flatMap { it.flattenedLayers.asList() }.distinctBy { it.id }
        val launcherLayers = layers.filter { ComponentNameMatcher.LAUNCHER.layerMatchesAnyOf(it) }

        ts.filter { t ->
            t.changes.any {
                it.transitMode == Transition.Companion.Type.CLOSE ||
                    it.transitMode == Transition.Companion.Type.TO_BACK
            } &&
                t.changes.any { change ->
                    launcherLayers.any { it.id == change.layerId }
                    change.transitMode == Transition.Companion.Type.TO_FRONT
                }
        }
    }

    val QUICK_SWITCH_TRANSITION_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { t ->
            t.changes.size == 2 &&
                t.changes.any { it.transitMode == Transition.Companion.Type.TO_BACK } &&
                t.changes.any { it.transitMode == Transition.Companion.Type.TO_FRONT }
        }
    }

    val QUICK_SWITCH_TRANSITION_MERGE: TransitionsTransform = { transitions, _, _ ->
        require(transitions.size == 2) { "Expected 2 transitions but got ${transitions.size}" }

        require(transitions[0].changes.size == 2)
        require(transitions[0].changes.any { it.transitMode == Transition.Companion.Type.TO_BACK })
        require(transitions[0].changes.any { it.transitMode == Transition.Companion.Type.TO_FRONT })

        require(transitions[1].changes.size == 2)
        require(transitions[1].changes.any { it.transitMode == Transition.Companion.Type.TO_BACK })
        require(transitions[1].changes.any { it.transitMode == Transition.Companion.Type.TO_FRONT })

        val candidateWallpaper1 =
            transitions[0].changes.first { it.transitMode == Transition.Companion.Type.TO_FRONT }
        val candidateWallpaper2 =
            transitions[1].changes.first { it.transitMode == Transition.Companion.Type.TO_BACK }

        require(candidateWallpaper1.layerId == candidateWallpaper2.layerId)

        val closingAppChange =
            transitions[0].changes.first { it.transitMode == Transition.Companion.Type.TO_BACK }
        val openingAppChange =
            transitions[1].changes.first { it.transitMode == Transition.Companion.Type.TO_FRONT }

        listOf(
            Transition(
                start = transitions[0].start,
                sendTime = transitions[0].sendTime,
                startTransactionId = transitions[0].startTransactionId,
                // NOTE: Relies on the implementation detail that the second
                // finishTransaction is merged into the first and applied.
                finishTransactionId = transitions[0].finishTransactionId,
                changes = listOf(closingAppChange, openingAppChange),
                played = true,
                aborted = false
            )
        )
    }
}
