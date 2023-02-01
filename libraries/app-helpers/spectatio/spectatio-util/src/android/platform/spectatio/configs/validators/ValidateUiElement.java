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

package android.platform.spectatio.configs.validators;

import android.platform.spectatio.configs.UiElement;
import android.platform.spectatio.constants.JsonConfigConstants;

import com.google.gson.JsonDeserializer;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link ValidateUiElement} is a deserializer that validates Ui Elements in Spectatio JSON Config
 * while deserializing it to a Java Object.
 *
 * <p>It checks for - 1. If TYPE is valid ( TEXT, TEXT_CONTAINS, DESCRIPTION, RESOURCE_ID, CLASS )
 * 2. If values are correctly provided ( PACKAGE is required if TYPE is RESOURCE_ID )
 */
public class ValidateUiElement implements JsonDeserializer<UiElement> {
    private Set<String> mSupportedTypes =
            Set.of(
                    JsonConfigConstants.TEXT,
                    JsonConfigConstants.TEXT_CONTAINS,
                    JsonConfigConstants.DESCRIPTION,
                    JsonConfigConstants.CLASS,
                    JsonConfigConstants.RESOURCE_ID);

    private Set<String> mSupportedProperties =
            Set.of(
                    JsonConfigConstants.TYPE,
                    JsonConfigConstants.VALUE,
                    JsonConfigConstants.PACKAGE);

    @Override
    public UiElement deserialize(
            JsonElement jsonElement, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = jsonElement.getAsJsonObject();

        validateProperties(jsonObject);

        String type =
                validateAndGetValue(JsonConfigConstants.TYPE, jsonObject, /*isOptional*/ false);

        validateType(type);

        String value =
                validateAndGetValue(JsonConfigConstants.VALUE, jsonObject, /*isOptional*/ false);

        // Package is not required for TEXT, TEXT_CONTAINS and DESCRIPTION
        String pkg = null;

        // Package is optional for CLASS
        if (JsonConfigConstants.CLASS.equals(type)) {
            pkg = validateAndGetValue(JsonConfigConstants.PACKAGE, jsonObject, /*isOptional*/ true);
        }

        // Package is required for RESOURCE_ID
        if (JsonConfigConstants.RESOURCE_ID.equals(type)) {
            pkg =
                    validateAndGetValue(
                            JsonConfigConstants.PACKAGE, jsonObject, /*isOptional*/ false);
        }

        return new UiElement(type, value, pkg);
    }

    /**
     * Validate and return the value of given property from Json Object
     *
     * @param key : Property name to get the value from Json Object
     * @param jsonObject : Json Object to read the property
     * @param isOptional : If the property is optional, it will be true else false.
     *     <p>If isOptional is false, then throw an exception if the property is missing If
     *     isOptional is true, then ignore if the property is missing
     */
    private String validateAndGetValue(String key, JsonObject jsonObject, boolean isOptional) {
        JsonElement value = jsonObject.get(key);
        if (value != null
                && value.isJsonPrimitive()
                && value.getAsJsonPrimitive().isString()
                && !value.getAsString().trim().isEmpty()) {
            return value.getAsString().trim();
        }
        if (!isOptional) {
            throw new RuntimeException(
                    String.format(
                            "Non-optional property %s for %s in Spectatio JSON Config "
                                    + "is either missing or invalid.",
                            key, jsonObject));
        }
        return null;
    }

    private void validateType(String type) {
        if (!mSupportedTypes.contains(type)) {
            throw new RuntimeException(
                    String.format(
                            "UI Element TYPE %s in Spectatio JSON Config is invalid. Supported"
                                + " Types: [ RESOURCE_ID, TEXT, TEXT_CONTAINS, DESCRIPTION, CLASS"
                                + " ]",
                            type));
        }
    }

    private void validateProperties(JsonObject jsonObject) {
        List<String> unknownProperties =
                jsonObject.entrySet().stream()
                        .map(Entry::getKey)
                        .map(String::trim)
                        .filter(key -> !mSupportedProperties.contains(key))
                        .collect(Collectors.toList());
        if (!unknownProperties.isEmpty()) {
            throw new RuntimeException(
                    String.format(
                            "Unknown properties: [ %s ] for %s in Spectatio JSON Config",
                            unknownProperties.stream().collect(Collectors.joining(", ")),
                            jsonObject));
        }
    }
}
