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

package platform.test.motion.compose

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isFinite
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import platform.test.motion.golden.DataPointType
import platform.test.motion.golden.FloatTolerances
import platform.test.motion.golden.UnknownTypeException

fun Dp.asDataPoint() = DataPointTypes.dp.makeDataPoint(this)

fun IntSize.asDataPoint() = DataPointTypes.intSize.makeDataPoint(this)

fun Offset.asDataPoint() = DataPointTypes.offset.makeDataPoint(this)

fun Size.asDataPoint() = DataPointTypes.size.makeDataPoint(this)

fun DpSize.asDataPoint() = DataPointTypes.dpSize.makeDataPoint(this)

fun DpOffset.asDataPoint() = DataPointTypes.dpOffset.makeDataPoint(this)

object DataPointTypes {

    val dp: DataPointType<Dp> =
        DataPointType.createWithTolerance(
                "dp",
                jsonToValue = {
                    when (it) {
                        is Float -> it.dp
                        is Number -> it.toFloat().dp
                        is String -> it.toFloatOrNull()?.dp ?: throw UnknownTypeException()
                        else -> throw UnknownTypeException()
                    }
                },
                valueToJson = { it.value },
                tolerance = 0.dp,
                toleranceAwareEquality = DpTolerances::isWithinTolerance,
            )
            .withAdjustedTolerance(1.dp)

    val intSize: DataPointType<IntSize> =
        DataPointType.create(
            "intSize",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    IntSize(getInt("width"), getInt("height"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width)
                    put("height", it.height)
                }
            },
        )

    val intOffset: DataPointType<IntOffset> =
        DataPointType.create(
            "intOffset",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    IntOffset(getInt("x"), getInt("y"))
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("x", it.x)
                    put("y", it.y)
                }
            },
        )

    val dpSize: DataPointType<DpSize> =
        DataPointType.createWithTolerance(
            "dpSize",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    DpSize(getDouble("width").dp, getDouble("height").dp)
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width.value)
                    put("height", it.height.value)
                }
            },
            tolerance = DpSize.Zero,
            toleranceAwareEquality = { a, b, t ->
                with(DpTolerances) {
                    isWithinTolerance(a.width, b.width, t.width) &&
                        isWithinTolerance(a.height, b.height, t.height)
                }
            },
        )

    val size: DataPointType<Size> =
        DataPointType.createWithTolerance(
            "size",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    Size(getDouble("width").toFloat(), getDouble("height").toFloat())
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("width", it.width)
                    put("height", it.height)
                }
            },
            tolerance = Size.Zero,
            toleranceAwareEquality = { a, b, t ->
                with(FloatTolerances) {
                    isWithinTolerance(a.width, b.width, t.width) &&
                        isWithinTolerance(a.height, b.height, t.height)
                }
            },
        )

    val dpOffset: DataPointType<DpOffset> =
        DataPointType.createWithTolerance(
            "dpOffset",
            jsonToValue = {
                with(it as? JSONObject ?: throw UnknownTypeException()) {
                    DpOffset(getDouble("x").dp, getDouble("y").dp)
                }
            },
            valueToJson = {
                JSONObject().apply {
                    put("x", it.x.value)
                    put("y", it.y.value)
                }
            },
            tolerance = DpOffset.Zero,
            toleranceAwareEquality = { a, b, t ->
                with(DpTolerances) {
                    isWithinTolerance(a.x, b.x, t.x) && isWithinTolerance(a.y, b.y, t.y)
                }
            },
        )

    val offset: DataPointType<Offset> =
        DataPointType.createWithTolerance(
            "offset",
            jsonToValue = {
                when (it) {
                    "unspecified" -> Offset.Unspecified
                    "infinite" -> Offset.Infinite
                    is JSONObject ->
                        Offset(it.getDouble("x").toFloat(), it.getDouble("y").toFloat())
                    else -> throw UnknownTypeException()
                }
            },
            valueToJson = {
                when {
                    it.isUnspecified -> "unspecified"
                    !it.isFinite -> "infinite"
                    else ->
                        JSONObject().apply {
                            put("x", it.x)
                            put("y", it.y)
                        }
                }
            },
            tolerance = Offset.Zero,
            toleranceAwareEquality = { a, b, t ->
                with(FloatTolerances) {
                    isWithinTolerance(a.x, b.x, t.x) && isWithinTolerance(a.y, b.y, t.y)
                }
            },
        )
}
