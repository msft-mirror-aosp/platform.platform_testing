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

package android.platform.spectatio.configs;

import android.platform.spectatio.constants.JsonConfigConstants;
import android.support.test.uiautomator.By;
import android.support.test.uiautomator.BySelector;

import com.google.gson.annotations.SerializedName;

import java.util.regex.Pattern;

/**
 * UI Resource For Spectatio Config JSON Config { "UI_ELEMENTS": { "CONFIG_NAME": { "TYPE":
 * "RESOURCE_TYPE", "VALUE": "RESOURCE_VALUE", "PACKAGE": "RESOURCE_PACKAGE" } } }
 *
 * <p>RESOURCE_TYPE: TEXT, DESCRIPTION, RESOURCE_ID, TEXT_CONTAINS, CLASS; RESOURCE_VALUE: Value of
 * the Resource; RESOURCE_PACKAGE: Package is required only to type RESOURCE_ID
 *
 * <p>Resource Value JSON { "TYPE": "RESOURCE_TYPE", "VALUE": "RESOURCE_VALUE", "PACKAGE":
 * "RESOURCE_PACKAGE" } } } is referred in code using this class
 */
public class UiElement {
    // Type of UI Element - Resource ID, Text, Description, Class
    @SerializedName("TYPE")
    private String mType;

    // Value for the UI Resource - id, text value, description or class for the resource
    @SerializedName("VALUE")
    private String mValue;

    // Application Package for the UI Resource if the type is Resource ID,
    @SerializedName("PACKAGE")
    private String mPackage;

    public UiElement(String type, String value, String pkg) {
        mType = type;
        mValue = value;
        mPackage = pkg;
    }

    /** Get Resource Type ( RESOURCE_ID, TEXT, DESCRIPTION, CLASS ) */
    public String getType() {
        return mType;
    }

    /** Get Resource Value ( resource id, text value, description, class ) */
    public String getValue() {
        return mValue;
    }

    public String getPackage() {
        return mPackage;
    }

    public BySelector getBySelectorForUiElement() {
        switch (mType) {
            case JsonConfigConstants.RESOURCE_ID:
                return By.res(mPackage, mValue);
            case JsonConfigConstants.TEXT:
                return By.text(Pattern.compile(mValue, Pattern.CASE_INSENSITIVE));
            case JsonConfigConstants.TEXT_CONTAINS:
                return By.textContains(mValue);
            case JsonConfigConstants.DESCRIPTION:
                return By.desc(Pattern.compile(mValue, Pattern.CASE_INSENSITIVE));
            case JsonConfigConstants.CLASS:
                if (mPackage != null && !mPackage.isEmpty()) {
                    return By.clazz(mPackage, mValue);
                }
                return By.clazz(mValue);
            default:
                // Unknown UI Resource Type
                return null;
        }
    }
}
