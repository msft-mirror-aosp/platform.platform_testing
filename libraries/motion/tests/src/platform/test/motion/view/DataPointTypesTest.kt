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

package platform.test.motion.view

import android.graphics.Point
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Expect
import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import platform.test.motion.golden.DataPoint
import platform.test.motion.golden.DataPoint.Companion.unknownType
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.ValueDataPoint
import platform.test.motion.testing.JsonSubject
import platform.test.motion.view.DataPointTypes.cornerRadii
import platform.test.motion.view.DataPointTypes.point
import platform.test.motion.view.DataPointTypes.rect

@RunWith(AndroidJUnit4::class)
class DataPointTypesTest {

    @get:Rule val expect: Expect = Expect.create()
    private fun <T> assertConversions(
        subject: DataPointType<T>,
        nativeObject: T,
        jsonRepresentation: String
    ) {
        expect
            .withMessage("serialize to JSON")
            .about(JsonSubject.json())
            .that(subject.toJson(nativeObject))
            .isEqualTo(JSONObject(jsonRepresentation))

        expect
            .withMessage("deserialize from JSON")
            .that(subject.fromJson(JSONObject(jsonRepresentation)))
            .isEqualTo(DataPoint.of(nativeObject, subject))
    }

    @Test
    fun point_fromToJson() {
        assertConversions(point, Point(/* x = */ 1, /* y = */ 2), """{"x":1, "y": 2}""")
    }

    @Test
    fun point_dataPoint_isImmutable() {
        val native = Point(/* x = */ 1, /* y = */ 2)
        val dataPoint = DataPoint.of(native, point)
        native.x = 3
        assertThat((dataPoint as ValueDataPoint).value.x).isEqualTo(1)
    }

    @Test
    fun point_fromInvalidJson_returnsUnknown() {
        assertThat(point.fromJson(JSONObject())).isEqualTo(unknownType<Point>())
        assertThat(point.fromJson(1)).isEqualTo(unknownType<Point>())
    }

    @Test
    fun rect_fromToJson() {
        assertConversions(
            rect,
            Rect(/* left = */ 1, /* top = */ 2, /* right = */ 3, /* bottom = */ 4),
            """{"left":1, "top": 2,"right":3, "bottom": 4}"""
        )
    }

    @Test
    fun rect_fromInvalidJson_returnsUnknown() {
        assertThat(rect.fromJson(JSONObject())).isEqualTo(unknownType<Rect>())
        assertThat(rect.fromJson(1)).isEqualTo(unknownType<Rect>())
    }

    fun rect_dataPoint_isImmutable() {
        val native = Rect(/* left = */ 1, /* top = */ 2, /* right = */ 3, /* bottom = */ 4)
        val dataPoint = DataPoint.of(native, rect)
        native.top = 5
        assertThat((dataPoint as ValueDataPoint).value.left).isEqualTo(1)
    }

    @Test
    fun cornerRadii_fromToJson() {
        assertConversions(
            cornerRadii,
            CornerRadii(FloatArray(8) { (it + 1).toFloat() }),
            """{
                "top_left_x": 1,
                "top_left_y": 2,
                "top_right_x": 3,
                "top_right_y": 4,
                "bottom_right_x": 5,
                "bottom_right_y": 6,
                "bottom_left_x": 7,
                "bottom_left_y": 8
                }"""
        )
    }

    fun cornerRadii_dataPoint_isImmutable() {
        val native = FloatArray(8) { (it + 1).toFloat() }
        val dataPoint = DataPoint.of(CornerRadii(native), cornerRadii)
        native[0] = 10f
        assertThat((dataPoint as ValueDataPoint).value.rawValues[0]).isEqualTo(1)
    }
}
