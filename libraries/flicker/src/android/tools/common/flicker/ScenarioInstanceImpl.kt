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

package android.tools.common.flicker

import android.tools.common.Logger
import android.tools.common.Rotation
import android.tools.common.Timestamp
import android.tools.common.flicker.assertions.ScenarioAssertion
import android.tools.common.flicker.assertions.ScenarioAssertionImpl
import android.tools.common.flicker.config.ScenarioConfig
import android.tools.common.io.Reader
import android.tools.common.traces.events.CujType
import android.tools.common.traces.wm.Transition

internal data class ScenarioInstanceImpl(
    override val config: ScenarioConfig,
    override val startRotation: Rotation,
    override val endRotation: Rotation,
    val startTimestamp: Timestamp,
    val endTimestamp: Timestamp,
    override val reader: Reader,
    val associatedCuj: CujType? = null,
    override val associatedTransition: Transition? = null,
) : ScenarioInstance {
    // b/227752705
    override val navBarMode
        get() = error("Unsupported")

    override val key = "${config.type.name}_${startRotation}_$endRotation"

    override val description = key

    override val isEmpty = false

    override fun <T> getConfigValue(key: String): T? = null

    override fun generateAssertions(): Collection<ScenarioAssertion> =
        Logger.withTracing("generateAssertions") {
            config.assertionTemplates.flatMap { template ->
                template.createAssertions(this).map { assertion ->
                    ScenarioAssertionImpl(reader, assertion, template.stabilityGroup)
                }
            }
        }

    override fun toString() = key
}
