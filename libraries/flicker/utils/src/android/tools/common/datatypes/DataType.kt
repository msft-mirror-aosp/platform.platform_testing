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

package android.tools.common.datatypes

import kotlin.js.JsExport
import kotlin.js.JsName

@JsExport
abstract class DataType {
    @JsName("isEmpty") abstract val isEmpty: Boolean

    val isNotEmpty
        get() = !isEmpty

    protected abstract fun doPrintValue(): String

    @JsName("prettyPrint") fun prettyPrint(): String = if (isEmpty) "[empty]" else doPrintValue()

    final override fun toString() = prettyPrint()

    final override fun hashCode() = doPrintValue().hashCode()

    final override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataType) return false
        if (this::class != other::class) return false

        return doPrintValue() == other.doPrintValue()
    }
}
