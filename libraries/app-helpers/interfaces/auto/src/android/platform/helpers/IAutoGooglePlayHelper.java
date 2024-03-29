/*
 * Copyright (C) 2019 The Android Open Source Project
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

package android.platform.helpers;

public interface IAutoGooglePlayHelper extends IAppHelper, Scrollable {

    /**
     * Setup expectations: Google Play app is open.
     *
     * This method is used to search a app on Google Play.
     */
    void searchApp(String appName);

    /**
     * Setup expectations: Google Play app is open.
     *
     * This method is used to install a app.
     */
    void installApp();

    /**
     * Setup expectations: Google Play app is open.
     *
     * This method is used to cancel a download.
     */
    void cancelDownload();

    /**
     * Setup expectations: Google Play app is open.
     *
     * This method is used to open a installed app.
     */
    void openApp();

    /**
     * Setup expectations: Google Play app is open.
     *
     * This method is used to return back to Google Play main page
     */
    void returnToMainPage();
}
