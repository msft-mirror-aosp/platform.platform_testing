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

package android.tools.common.flicker.assertions

class ServiceFlickerTest(defaultAssertionName: String) : BaseFlickerTest(defaultAssertionName) {
    private val assertionsMap = mutableMapOf<String, AssertionData>()
    val assertions: Collection<AssertionData> = assertionsMap.values

    override fun doProcess(assertion: AssertionData) {
        val name = assertion.name
        require(!assertionsMap.containsKey(assertion.name)) { "Assertion name $name already used" }
        assertionsMap[name] = assertion
    }
}
