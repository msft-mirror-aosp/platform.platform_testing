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

package com.android.server.wm.flicker.helpers

import kotlin.reflect.KProperty

fun <T> clearableLazy(initializer: () -> T) = ClearableLazyDelegate(initializer)

/** NOTE: This implementation is not thread safe */
class ClearableLazyDelegate<T>(private val initializer: () -> T) {
    private var uninitialized = true
    private var value: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (uninitialized) {
            value = initializer()
            uninitialized = false
        }
        return value!!
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        require(value === null) { "Can only set to null to clear memory" }
        this.value = null
        uninitialized = true
    }
}
