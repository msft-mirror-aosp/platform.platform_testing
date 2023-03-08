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

package android.tools.common.flicker.assertors.assertions

import android.tools.common.flicker.assertors.AssertionTemplate
import android.tools.common.flicker.assertors.ComponentTemplate

/** Base class for tests that require a [component] named window name */
abstract class AssertionTemplateWithComponent(val component: ComponentTemplate) :
    AssertionTemplate() {

    override val assertionName = "${this::class.simpleName}(${component.name})"

    override fun equals(other: Any?): Boolean {
        if (other !is AssertionTemplateWithComponent) {
            return false
        }

        // Check both assertions are instances of the same class.
        if (this::class != component::class) {
            return false
        }

        // TODO: Make sure equality is properly defined on the component
        return other.component == component
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + component.hashCode()
        return result
    }
}
