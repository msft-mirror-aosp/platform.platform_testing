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

import org.json.JSONException

/**
 * Golden value type detailing how to convert to/from JSON, and how two [ValueDataPoint.value]s.
 *
 * When using [DataPointType.createWithTolerance], the equality check is delegated to
 * [toleranceAwareEquality], that defines the algorithm to verify whether two [T] are within
 * [tolerance] of each other, and thus are considered equals.
 *
 * @param typeName identifier written to the JSON, to support de-serialization of the values.
 * @param jsonToValue convert the [jsonValue] to a native [T], throws [JSONException] if conversion
 *   fails.
 * @param valueToJson converts the native [T] to a `org.json` supported type.
 * @param tolerance a tolerance passed to the `toleranceAwareEquality`. Clients can create copies of
 *   this [DataPointType.withAdjustedTolerance] to specify a different tolerance value.
 * @param toleranceAwareEquality the function to verify whether the properties of two [T] instances
 *   are within the [tolerance].
 * @param ensureImmutable copies mutable objects, to avoid subsequent modification.
 */
class DataPointType<T>
internal constructor(
    val typeName: String,
    private val jsonToValue: (jsonValue: Any) -> T,
    private val valueToJson: (T) -> Any,
    internal val tolerance: T?,
    internal val toleranceAwareEquality: ((expected: T, actual: T, tolerance: T) -> Boolean)?,
    internal val ensureImmutable: (T & Any) -> T & Any,
) {
    fun makeDataPoint(nativeValue: T?): DataPoint<T> {
        return DataPoint.of(nativeValue, this)
    }

    fun fromJson(jsonValue: Any): DataPoint<T> {
        return when {
            NullDataPoint.isNullValue(jsonValue) -> DataPoint.nullValue()
            NotFoundDataPoint.isNotFoundValue(jsonValue) -> DataPoint.notFound()
            else ->
                try {
                    makeDataPoint(jsonToValue(jsonValue))
                } catch (e: JSONException) {
                    DataPoint.unknownType()
                }
        }
    }

    fun toJson(value: T): Any = valueToJson(value)

    /**
     * Whether [expected] and [actual] are equal, or with the [tolerance] if the type was originally
     * created with [createWithTolerance].
     */
    fun isEqualWithinTolerance(expected: T, actual: T): Boolean {
        if (expected == actual) return true
        if (tolerance == null) return false
        checkNotNull(toleranceAwareEquality)
        return toleranceAwareEquality(expected, actual, tolerance)
    }

    override fun toString(): String {
        return typeName
    }

    /**
     * Returns a copy of this [DataPoint], which allows a difference of up to [tolerance].
     *
     * Only [DataPointType]s originally created with [createWithTolerance] support adjusting the
     * tolerance.
     */
    fun withAdjustedTolerance(tolerance: T): DataPointType<T> {
        check(toleranceAwareEquality != null) { "Type [$typeName] does not support tolerances" }
        return DataPointType(
            typeName,
            jsonToValue,
            valueToJson,
            tolerance,
            toleranceAwareEquality,
            ensureImmutable,
        )
    }

    companion object {
        /**
         * Creates a [DataPointType] that uses default equality on [T] to verify if two values are
         * the same.
         *
         * @param typeName identifier written to the JSON, to support de-serialization of the
         *   values.
         * @param jsonToValue convert the [jsonValue] to a native [T], throws [JSONException] if
         *   conversion fails.
         * @param valueToJson converts the native [T] to a `org.json` supported type.
         * @param tolerance a tolerance passed to the `toleranceAwareEquality`. Clients can create
         *   copies of this [DataPointType.withAdjustedTolerance] to specify a different tolerance
         *   value.
         * @param toleranceAwareEquality the function to verify whether the properties of two [T]
         *   instances are within the [tolerance].
         * @param ensureImmutable copies mutable objects, to avoid subsequent modification.
         */
        fun <T> create(
            typeName: String,
            jsonToValue: (jsonValue: Any) -> T,
            valueToJson: (T) -> Any,
            ensureImmutable: (T & Any) -> T & Any = { it },
        ): DataPointType<T> =
            DataPointType(
                typeName,
                jsonToValue,
                valueToJson,
                tolerance = null,
                toleranceAwareEquality = null,
                ensureImmutable = ensureImmutable,
            )

        /**
         * Creates a [DataPointType] that uses [toleranceAwareEquality] two verify whether two [T]
         * are within [tolerance] of each other.
         *
         * @param typeName identifier written to the JSON, to support de-serialization of the
         *   values.
         * @param jsonToValue convert the [jsonValue] to a native [T], throws [JSONException] if
         *   conversion fails.
         * @param valueToJson converts the native [T] to a `org.json` supported type.
         * @param tolerance a tolerance passed to the `toleranceAwareEquality`. Clients can create
         *   copies of this [DataPointType.withAdjustedTolerance] to specify a different tolerance
         *   value.
         * @param toleranceAwareEquality the function to verify whether the properties of two [T]
         *   instances are within the [tolerance].
         * @param ensureImmutable copies mutable objects, to avoid subsequent modification.
         */
        fun <T> createWithTolerance(
            typeName: String,
            jsonToValue: (jsonValue: Any) -> T,
            valueToJson: (T) -> Any,
            tolerance: T,
            toleranceAwareEquality: (expected: T, actual: T, tolerance: T) -> Boolean,
            ensureImmutable: (T & Any) -> T & Any = { it },
        ): DataPointType<T> =
            DataPointType(
                typeName,
                jsonToValue,
                valueToJson,
                tolerance = tolerance,
                toleranceAwareEquality = toleranceAwareEquality,
                ensureImmutable = ensureImmutable,
            )
    }
}

/** Signals that a JSON value cannot be deserialized by a [DataPointType]. */
class UnknownTypeException : JSONException("JSON cannot be converted to DataPoint value")
