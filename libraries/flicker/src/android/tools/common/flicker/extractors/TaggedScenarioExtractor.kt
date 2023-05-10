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

package android.tools.common.flicker.extractors

import android.tools.common.CrossPlatform
import android.tools.common.Timestamp
import android.tools.common.flicker.ScenarioInstance
import android.tools.common.flicker.config.FaasScenarioType
import android.tools.common.io.IReader
import android.tools.common.traces.events.Cuj
import android.tools.common.traces.events.CujType
import android.tools.common.traces.surfaceflinger.LayerTraceEntry
import android.tools.common.traces.wm.Transition
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class TaggedScenarioExtractor(
    val targetTag: CujType,
    val type: FaasScenarioType,
    val transitionMatcher: ITransitionMatcher = TransitionMatcher(),
    val adjustCuj: (cujEntry: Cuj, reader: IReader) -> Cuj = { cujEntry, reader -> cujEntry }
) : IScenarioExtractor {
    override fun extract(reader: IReader): List<ScenarioInstance> {

        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val cujTrace = reader.readCujTrace() ?: error("Missing CUJ trace")

        val targetCujEntries =
            cujTrace.entries
                .filter { it.cuj === targetTag }
                .filter { !it.canceled }
                .map { adjustCuj(it, reader) }

        if (targetCujEntries.isEmpty()) {
            // No scenarios to extract here
            return emptyList()
        }

        return targetCujEntries.map { cujEntry ->
            val associatedTransition = transitionMatcher.getTransition(cujEntry, reader)

            require(
                cujEntry.startTimestamp.hasAllTimestamps && cujEntry.endTimestamp.hasAllTimestamps
            )

            val startTimestamp =
                estimateScenarioStartTimestamp(cujEntry, associatedTransition, reader)
            val endTimestamp = estimateScenarioEndTimestamp(cujEntry, associatedTransition, reader)

            ScenarioInstance(
                type,
                startRotation =
                    layersTrace
                        .getEntryAt(startTimestamp)
                        .displays
                        .first { !it.isVirtual && it.layerStackSpace.isNotEmpty }
                        .transform
                        .getRotation(),
                endRotation =
                    layersTrace
                        .getEntryAt(endTimestamp)
                        .displays
                        .first { !it.isVirtual && it.layerStackSpace.isNotEmpty }
                        .transform
                        .getRotation(),
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp,
                associatedCuj = cujEntry.cuj,
                associatedTransition = associatedTransition,
                reader = reader.slice(startTimestamp, endTimestamp)
            )
        }
    }

    private fun estimateScenarioStartTimestamp(
        cujEntry: Cuj,
        associatedTransition: Transition?,
        reader: IReader
    ): Timestamp {
        val interpolatedStartTimestamp =
            if (associatedTransition != null) {
                interpolateStartTimestampFromTransition(associatedTransition, reader)
            } else {
                null
            }

        return CrossPlatform.timestamp.from(
            elapsedNanos =
                min(
                    cujEntry.startTimestamp.elapsedNanos,
                    interpolatedStartTimestamp?.elapsedNanos ?: cujEntry.startTimestamp.elapsedNanos
                ),
            systemUptimeNanos =
                min(
                    cujEntry.startTimestamp.systemUptimeNanos,
                    interpolatedStartTimestamp?.systemUptimeNanos
                        ?: cujEntry.startTimestamp.systemUptimeNanos
                ),
            unixNanos =
                min(
                    cujEntry.startTimestamp.unixNanos,
                    interpolatedStartTimestamp?.unixNanos ?: cujEntry.startTimestamp.unixNanos
                )
        )
    }

    private fun interpolateStartTimestampFromTransition(
        transition: Transition,
        reader: IReader
    ): Timestamp {
        val wmTrace = reader.readWmTrace() ?: error("Missing WM trace")
        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val transactionsTrace =
            reader.readTransactionsTrace() ?: error("Missing transactions trace")

        val lastWmEntryBeforeTransitionCreated = wmTrace.getEntryAt(transition.createTime)
        val elapsedNanos = lastWmEntryBeforeTransitionCreated.timestamp.elapsedNanos
        val unixNanos = lastWmEntryBeforeTransitionCreated.timestamp.unixNanos

        val startTransactionAppliedTimestamp =
            transition.getStartTransaction(transactionsTrace)?.let {
                layersTrace.getEntryForTransaction(it).timestamp
            }

        // If we don't have a startTransactionAppliedTimestamp it's likely because the start
        // transaction was merged into another transaction so we can't match the id, so we need to
        // fallback on the send time reported on the WM side.
        val systemUptimeNanos =
            startTransactionAppliedTimestamp?.systemUptimeNanos
                ?: transition.createTime.systemUptimeNanos

        return CrossPlatform.timestamp.from(elapsedNanos, systemUptimeNanos, unixNanos)
    }

    private fun estimateScenarioEndTimestamp(
        cujEntry: Cuj,
        associatedTransition: Transition?,
        reader: IReader
    ): Timestamp {
        val interpolatedEndTimestamp =
            if (associatedTransition != null) {
                interpolateFinishTimestampFromTransition(associatedTransition, reader)
            } else {
                null
            }

        return CrossPlatform.timestamp.from(
            elapsedNanos =
                max(
                    cujEntry.endTimestamp.elapsedNanos,
                    interpolatedEndTimestamp?.elapsedNanos ?: -1L
                ),
            systemUptimeNanos =
                max(
                    cujEntry.endTimestamp.systemUptimeNanos,
                    interpolatedEndTimestamp?.systemUptimeNanos ?: -1L
                ),
            unixNanos =
                max(cujEntry.endTimestamp.unixNanos, interpolatedEndTimestamp?.unixNanos ?: -1L)
        )
    }

    private fun interpolateFinishTimestampFromTransition(
        transition: Transition,
        reader: IReader
    ): Timestamp {
        val layersTrace = reader.readLayersTrace() ?: error("Missing layers trace")
        val wmTrace = reader.readWmTrace() ?: error("Missing WM trace")
        val transactionsTrace =
            reader.readTransactionsTrace() ?: error("Missing transactions trace")

        // There is a delay between when we flag that transition as finished with the CUJ tags
        // and when it is actually finished on the SF side. We try and account for that by
        // checking when the finish transaction is actually applied.
        // TODO: Figure out how to get the vSyncId that the Jank tracker actually gets to avoid
        //       relying on the transition and have a common end point.
        val finishTransactionAppliedTimestamp =
            transition.getFinishTransaction(transactionsTrace)?.let {
                layersTrace.getEntryForTransaction(it).timestamp
            }

        val elapsedNanos: Long
        val systemUptimeNanos: Long
        val unixNanos: Long
        val sfEntryAtTransitionFinished: LayerTraceEntry
        if (finishTransactionAppliedTimestamp == null) {
            // If we don't have a finishTransactionAppliedTimestamp it's likely because the finish
            // transaction was merged into another transaction so we can't match the id, so we need
            // to fallback on the finish time reported on the WM side.
            val wmEntryAtTransitionFinished =
                wmTrace.entries.firstOrNull { it.timestamp >= transition.finishTime }

            elapsedNanos =
                wmEntryAtTransitionFinished?.timestamp?.elapsedNanos
                    ?: transition.finishTime.elapsedNanos

            unixNanos =
                if (wmEntryAtTransitionFinished != null) {
                    wmEntryAtTransitionFinished.timestamp.unixNanos
                } else {
                    require(wmTrace.entries.isNotEmpty())
                    val closestWmEntry =
                        wmTrace.entries
                            .sortedBy {
                                abs(it.timestamp.elapsedNanos - transition.finishTime.elapsedNanos)
                            }
                            .first()
                    val offset =
                        closestWmEntry.timestamp.unixNanos - closestWmEntry.timestamp.elapsedNanos
                    transition.finishTime.elapsedNanos + offset
                }

            sfEntryAtTransitionFinished =
                layersTrace.entries.firstOrNull { it.timestamp.unixNanos >= unixNanos }
                    ?: error("No SF entry for finish timestamp")
            systemUptimeNanos = sfEntryAtTransitionFinished.timestamp.systemUptimeNanos
        } else {
            elapsedNanos =
                wmTrace.entries
                    .first { it.timestamp >= finishTransactionAppliedTimestamp }
                    .timestamp
                    .elapsedNanos
            systemUptimeNanos =
                layersTrace
                    .getEntryAt(finishTransactionAppliedTimestamp)
                    .timestamp
                    .systemUptimeNanos
            unixNanos =
                layersTrace.getEntryAt(finishTransactionAppliedTimestamp).timestamp.unixNanos
        }

        return CrossPlatform.timestamp.from(elapsedNanos, systemUptimeNanos, unixNanos)
    }
}
