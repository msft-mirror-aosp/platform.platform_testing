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

package android.tools.common.traces.wm

import android.tools.common.CrossPlatform
import com.google.common.truth.Truth
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runners.MethodSorters

/**
 * Contains [WindowManagerTrace] tests. To run this test: `atest
 * FlickerLibTest:WindowManagerTraceTest`
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TransitionTest {
    @Test
    fun canMerge() {
        val transition1 =
            Transition(
                id = 1,
                wmData =
                    WmTransitionData(
                        createTime = CrossPlatform.timestamp.from(10),
                        sendTime = CrossPlatform.timestamp.from(20),
                    ),
            )

        val transition2 =
            Transition(
                id = 1,
                shellData =
                    ShellTransitionData(
                        dispatchTime = CrossPlatform.timestamp.from(22),
                        handler = "DefaultHandler"
                    ),
            )

        val transition3 =
            Transition(
                id = 1,
                wmData =
                    WmTransitionData(
                        finishTime = CrossPlatform.timestamp.from(40),
                    ),
            )

        val mergedTransition = transition1.merge(transition2).merge(transition3)

        Truth.assertThat(mergedTransition.createTime.elapsedNanos).isEqualTo(10)
        Truth.assertThat(mergedTransition.sendTime.elapsedNanos).isEqualTo(20)
        Truth.assertThat(mergedTransition.dispatchTime.elapsedNanos).isEqualTo(22)
        Truth.assertThat(mergedTransition.finishTime.elapsedNanos).isEqualTo(40)
    }

    @Test
    fun mergeOverrideValues() {
        val transition1 =
            Transition(
                id = 1,
                wmData =
                    WmTransitionData(
                        createTime = CrossPlatform.timestamp.from(10),
                        sendTime = CrossPlatform.timestamp.from(20),
                        abortTime = CrossPlatform.timestamp.from(30),
                        finishTime = CrossPlatform.timestamp.from(40),
                        startTransactionId = 1,
                        finishTransactionId = 2,
                        type = TransitionType.CLOSE,
                        changes = arrayOf(),
                    ),
                shellData =
                    ShellTransitionData(
                        dispatchTime = CrossPlatform.timestamp.from(21),
                        mergeRequestTime = CrossPlatform.timestamp.from(22),
                        mergeTime = CrossPlatform.timestamp.from(23),
                        abortTime = CrossPlatform.timestamp.from(24),
                        handler = "Handler1",
                        mergedInto = 1,
                    )
            )

        val transition2 =
            Transition(
                id = 1,
                wmData =
                    WmTransitionData(
                        createTime = CrossPlatform.timestamp.from(100),
                        sendTime = CrossPlatform.timestamp.from(200),
                        abortTime = CrossPlatform.timestamp.from(300),
                        finishTime = CrossPlatform.timestamp.from(400),
                        startTransactionId = 10,
                        finishTransactionId = 20,
                        type = TransitionType.OPEN,
                        changes = arrayOf(),
                    ),
                shellData =
                    ShellTransitionData(
                        dispatchTime = CrossPlatform.timestamp.from(210),
                        mergeRequestTime = CrossPlatform.timestamp.from(220),
                        mergeTime = CrossPlatform.timestamp.from(230),
                        abortTime = CrossPlatform.timestamp.from(240),
                        handler = "Handler2",
                        mergedInto = 10,
                    )
            )

        val mergedTransition = transition1.merge(transition2)

        Truth.assertThat(mergedTransition.createTime.elapsedNanos).isEqualTo(100)
        Truth.assertThat(mergedTransition.sendTime.elapsedNanos).isEqualTo(200)
        Truth.assertThat(mergedTransition.abortTime?.elapsedNanos).isEqualTo(300)
        Truth.assertThat(mergedTransition.finishTime.elapsedNanos).isEqualTo(400)
        Truth.assertThat(mergedTransition.startTransactionId).isEqualTo(10)
        Truth.assertThat(mergedTransition.finishTransactionId).isEqualTo(20)

        Truth.assertThat(mergedTransition.dispatchTime.elapsedNanos).isEqualTo(210)
        Truth.assertThat(mergedTransition.mergeRequestTime?.elapsedNanos).isEqualTo(220)
        Truth.assertThat(mergedTransition.mergeTime?.elapsedNanos).isEqualTo(230)
        Truth.assertThat(mergedTransition.shellAbortTime?.elapsedNanos).isEqualTo(240)
        Truth.assertThat(mergedTransition.handler).isEqualTo("Handler2")
        Truth.assertThat(mergedTransition.mergedInto).isEqualTo(10)
    }
}
