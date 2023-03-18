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

package android.tools.common.flicker.config

import android.tools.common.datatypes.component.ComponentNameMatcher
import android.tools.common.flicker.extractors.TransitionsTransform
import android.tools.common.traces.wm.Transition
import android.tools.common.traces.wm.TransitionType

object TransitionFilters {
    val OPEN_APP_TRANSITION_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { t ->
            t.changes.any {
                it.transitMode == TransitionType.OPEN || // cold launch
                it.transitMode == TransitionType.TO_FRONT // warm launch
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
                it.transitMode == TransitionType.CLOSE || it.transitMode == TransitionType.TO_BACK
            } &&
                t.changes.any { change ->
                    launcherLayers.any { it.id == change.layerId }
                    change.transitMode == TransitionType.TO_FRONT
                }
        }
    }

    val QUICK_SWITCH_TRANSITION_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { t ->
            t.changes.size == 2 &&
                t.changes.any { it.transitMode == TransitionType.TO_BACK } &&
                t.changes.any { it.transitMode == TransitionType.TO_FRONT }
        }
    }

    val QUICK_SWITCH_TRANSITION_MERGE: TransitionsTransform = { transitions, _, _ ->
        require(transitions.size == 2) { "Expected 2 transitions but got ${transitions.size}" }

        require(transitions[0].changes.size == 2)
        require(transitions[0].changes.any { it.transitMode == TransitionType.TO_BACK })
        require(transitions[0].changes.any { it.transitMode == TransitionType.TO_FRONT })

        require(transitions[1].changes.size == 2)
        require(transitions[1].changes.any { it.transitMode == TransitionType.TO_BACK })
        require(transitions[1].changes.any { it.transitMode == TransitionType.TO_FRONT })

        val candidateWallpaper1 =
            transitions[0].changes.first { it.transitMode == TransitionType.TO_FRONT }
        val candidateWallpaper2 =
            transitions[1].changes.first { it.transitMode == TransitionType.TO_BACK }

        require(candidateWallpaper1.layerId == candidateWallpaper2.layerId)

        val closingAppChange =
            transitions[0].changes.first { it.transitMode == TransitionType.TO_BACK }
        val openingAppChange =
            transitions[1].changes.first { it.transitMode == TransitionType.TO_FRONT }

        listOf(
            Transition(
                createTime = transitions[0].createTime,
                sendTime = transitions[0].sendTime,
                // NOTE: Relies on the implementation detail that the second
                // finishTransaction is merged into the first and applied.
                finishTime = transitions[0].finishTime,
                startTransactionId = transitions[0].startTransactionId,
                // NOTE: Relies on the implementation detail that the second
                // finishTransaction is merged into the first and applied.
                finishTransactionId = transitions[0].finishTransactionId,
                type = transitions[1].type,
                changes = listOf(closingAppChange, openingAppChange),
                played = transitions[1].played,
                aborted = transitions[1].aborted,
            )
        )
    }

    val APP_CLOSE_TO_PIP_TRANSITION_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { it.type == TransitionType.PIP }
    }

    val ENTER_SPLIT_SCREEN_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { isSplitscreenEnterTransition(it) }
    }

    val EXIT_SPLIT_SCREEN_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { isSplitscreenExitTransition(it) }
    }

    val RESIZE_SPLIT_SCREEN_FILTER: TransitionsTransform = { ts, _, _ ->
        ts.filter { isSplitscreenResizeTransition(it) }
    }

    fun isSplitscreenEnterTransition(transition: Transition): Boolean {
        return transition.type == TransitionType.SPLIT_SCREEN_PAIR_OPEN ||
            transition.type == TransitionType.SPLIT_SCREEN_OPEN_TO_SIDE
    }

    fun isSplitscreenExitTransition(transition: Transition): Boolean {
        return transition.type == TransitionType.SPLIT_DISMISS ||
            transition.type == TransitionType.SPLIT_DISMISS_SNAP
    }

    fun isSplitscreenResizeTransition(transition: Transition): Boolean {
        // This transition doesn't have a special type
        return transition.type == TransitionType.CHANGE &&
            transition.changes.size == 2 &&
            transition.changes.all { change -> change.transitMode == TransitionType.CHANGE }
    }
}
