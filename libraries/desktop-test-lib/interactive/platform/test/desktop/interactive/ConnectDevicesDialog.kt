/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.test.desktop.interactive

import com.android.interactive.Step
import java.util.Optional

/** Implementation of dialog shown to the human during tests, relying on Interactive library. */
class ConnectDevicesDialog : ConnectDevicesDialogProvider {
    override fun showDialog(text: String, timeout: Long): Boolean {
        textToShow = text
        deadline = System.currentTimeMillis() + timeout
        unblock = Optional.empty<Boolean>()
        return Step.execute(AskHumanConnectDevicesDialog::class.java) ?: false
    }

    override fun unblockDialog() {
        unblock = Optional.of<Boolean>(true)
    }

    companion object {
        @Volatile private var unblock = Optional.empty<Boolean>()
        private var deadline: Long = 0L
        private var textToShow: String = ""
    }

    class AskHumanConnectDevicesDialog : Step<Boolean>() {
        override fun interact() {
            show(textToShow)
            // Return true, so the test must continue to run
            addButton("Continue", { pass(true) })
            // Return false, so the test is not required to run.
            addButton("Skip", { pass(false) })
        }

        override fun getValue(): Optional<Boolean> {
            // If timeout exceeded - unblock the dialog
            if (System.currentTimeMillis() > deadline) {
                // Return true, so the test must attempt continue to run
                // isConditionSatisfied in [peripheralsSetup] may be set false,
                // but test may still continue if mustRunTest is false
                return Optional.of(true)
            }
            return super.value.or { unblock }
        }
    }
}
