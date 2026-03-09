/*
 * Copyright (C) 2024 The Android Open Source Project
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

package platform.test.motion.golden

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.DataPoint.Companion.notFound
import platform.test.motion.golden.DataPoint.Companion.nullValue

@RunWith(AndroidJUnit4::class)
class DataPointTypeTest {
    data class Native(val id: String)

    private val subject =
        DataPointType.create(
            "native",
            jsonToValue = { jsonValue ->
                jsonToValueInvocations++
                if (jsonValue is String) Native(jsonValue) else throw UnknownTypeException()
            },
            valueToJson = {
                valueToJsonInvocations++
                it.id
            },
        )
    private var jsonToValueInvocations = 0
    private var valueToJsonInvocations = 0

    @Test
    fun makeDataPoint_ofNull_createsNullValue() {
        assertThat(subject.makeDataPoint(null)).isEqualTo(nullValue<Native>())
    }

    @Test
    fun makeDataPoint_ofInstance_createsValueDataPoint() {
        val nativeValue = Native("one")
        val dataPoint = subject.makeDataPoint(nativeValue)

        assertThat(dataPoint).isInstanceOf(ValueDataPoint::class.java)
        val valueDataPoint = dataPoint as ValueDataPoint
        assertThat(valueDataPoint.value).isSameInstanceAs(nativeValue)
        assertThat(valueDataPoint.type).isSameInstanceAs(subject)
    }

    @Test
    fun fromJson_ofNull_returnsNullValue() {
        val dataPoint = subject.fromJson(JSONObject.NULL)

        assertThat(dataPoint).isEqualTo(nullValue<Native>())
        assertThat(jsonToValueInvocations).isEqualTo(0)
    }

    @Test
    fun fromJson_ofNotFound_returnsNotFound() {
        val dataPoint = subject.fromJson(NotFoundDataPoint.instance.asJson())

        assertThat(dataPoint).isEqualTo(notFound<Native>())
        assertThat(jsonToValueInvocations).isEqualTo(0)
    }

    @Test
    fun fromJson_ofValue_returnsValueDataPoint() {
        val dataPoint = subject.fromJson("one")

        assertThat(dataPoint).isInstanceOf(ValueDataPoint::class.java)
        val valueDataPoint = dataPoint as ValueDataPoint
        assertThat(valueDataPoint.value).isEqualTo(Native("one"))
        assertThat(valueDataPoint.type).isSameInstanceAs(subject)
        assertThat(jsonToValueInvocations).isEqualTo(1)
    }

    @Test
    fun toJson_delegatesToConverter() {
        val json = subject.toJson(Native("one"))

        assertThat(json).isEqualTo("one")
        assertThat(valueToJsonInvocations).isEqualTo(1)
    }

    @Test
    fun withoutTolerance_throwsWhenSettingUpdatedTolerance() {
        val underTest =
            DataPointType.create("foo", jsonToValue = { it as Float }, valueToJson = { it })

        Assert.assertThrows(IllegalStateException::class.java) {
            underTest.withAdjustedTolerance(1f)
        }
    }

    @Test
    fun withoutTolerance_comparesDataPoints() {
        val underTest =
            DataPointType.create("foo", jsonToValue = { it as Int }, valueToJson = { it })

        assertThat(underTest.isEqualWithinTolerance(expected = 1, actual = 1)).isTrue()
        assertThat(underTest.isEqualWithinTolerance(expected = 1, actual = 2)).isFalse()
    }

    @Test
    fun tolerance_toleranceAwareEquality_returnsTrue_dataPointsAreEqual() {
        val underTest =
            DataPointType.createWithTolerance(
                "foo",
                jsonToValue = { it as Float },
                valueToJson = { it },
                tolerance = .1f,
                toleranceAwareEquality = { a, b, tolerance -> true },
            )

        assertThat(underTest.isEqualWithinTolerance(expected = 1f, actual = 2f)).isTrue()
    }

    @Test
    fun tolerance_toleranceAwareEquality_returnsFalse_dataPointsAreEqual() {
        val underTest =
            DataPointType.createWithTolerance(
                "foo",
                jsonToValue = { it as Float },
                valueToJson = { it },
                tolerance = .1f,
                toleranceAwareEquality = { a, b, tolerance -> false },
            )

        assertThat(underTest.isEqualWithinTolerance(expected = 1f, actual = 2f)).isFalse()
    }

    @Test
    fun tolerance_toleranceAwareEquality_returnsTrue_forEqualDataPoints() {
        val underTest =
            DataPointType.createWithTolerance(
                "foo",
                jsonToValue = { it as Float },
                valueToJson = { it },
                tolerance = .1f,
                toleranceAwareEquality = { a, b, tolerance ->
                    fail("must not be called for equal values")
                    false
                },
            )

        assertThat(underTest.isEqualWithinTolerance(expected = 1f, actual = 1f)).isTrue()
    }

    @Test
    fun tolerance_toleranceAwareEquality_valuesArePassedToCorrectArgument() {
        val underTest =
            DataPointType.createWithTolerance(
                "foo",
                jsonToValue = { it as Float },
                valueToJson = { it },
                tolerance = 0f,
                toleranceAwareEquality = { expected, actual, tolerance ->
                    assertThat(expected).isEqualTo(1f)
                    assertThat(actual).isEqualTo(2f)
                    assertThat(tolerance).isEqualTo(0f)

                    true
                },
            )

        underTest.isEqualWithinTolerance(expected = 1f, actual = 2f)
    }

    @Test
    fun tolerance_whenUpdated_isPassedToArgument() {

        val tolerances = mutableListOf<Float>()

        val original =
            DataPointType.createWithTolerance(
                "foo",
                jsonToValue = { it as Float },
                valueToJson = { it },
                tolerance = 0f,
                toleranceAwareEquality = { a, b, tolerance ->
                    tolerances.add(tolerance)
                    true
                },
            )

        val updated = original.withAdjustedTolerance(1f)

        original.isEqualWithinTolerance(0f, 1f)
        updated.isEqualWithinTolerance(0f, 1f)
        original.isEqualWithinTolerance(0f, 1f)

        assertThat(tolerances).containsExactly(0f, 1f, 0f).inOrder()
    }
}
