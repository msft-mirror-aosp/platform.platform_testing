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

import android.platform.spectatio.utils.SpectatioUiUtil;
import android.platform.spectatio.constants.JsonConfigConstants.ScrollActions;
import android.platform.spectatio.constants.JsonConfigConstants.ScrollDirection;
import android.platform.spectatio.constants.JsonConfigConstants.SupportedWorkFlowTasks;
import android.platform.spectatio.exceptions.MissingUiElementException;
import android.support.test.uiautomator.BySelector;
import android.support.test.uiautomator.UiObject2;
import android.util.Log;

import com.google.common.base.Strings;
import com.google.gson.annotations.SerializedName;

/** Workflow Task For Workflows in Spectatio Config JSON Config */
public class WorkflowTask {
    private static final String LOG_TAG = WorkflowTask.class.getSimpleName();

    @SerializedName("NAME")
    private String mName;

    @SerializedName("TYPE")
    private String mType;

    // TEXT or UI_ELEMENT based on the Type of Workflow Task
    @SerializedName("CONFIG")
    private WorkflowTaskConfig mTaskConfig;

    // Number of times to repeat given task, default is 0
    // By default, task will be execute once and won't be repeated as repeat count is 0
    @SerializedName("REPEAT_COUNT")
    private int mRepeatCount;

    // If task needs scrolling, provide Scroll Config
    @SerializedName("SCROLL_CONFIG")
    private ScrollConfig mScrollConfig;

    public WorkflowTask(
            String name,
            String type,
            WorkflowTaskConfig taskConfig,
            int repeatCount,
            ScrollConfig scrollConfig) {
        mName = name;
        mType = type;
        mTaskConfig = taskConfig;
        mRepeatCount = repeatCount;
        mScrollConfig = scrollConfig;
    }

    public String getTaskName() {
        return mName;
    }

    public String getTaskType() {
        return mType;
    }

    public WorkflowTaskConfig getTaskConfig() {
        return mTaskConfig;
    }

    public int getRepeatCount() {
        return mRepeatCount;
    }

    public ScrollConfig getScrollConfig() {
        return mScrollConfig;
    }

    public void executeTask(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        Log.i(
                LOG_TAG,
                String.format(
                        "Executing Task %s with Type %s for Workflow %s",
                        mName, mType, workflowName));

        SupportedWorkFlowTasks taskType = null;
        try {
            taskType = SupportedWorkFlowTasks.valueOf(mType);
        } catch (IllegalArgumentException ex) {
            throwRuntimeException("Workflow Task Type", mType, workflowName, "Not Supported");
        }

        int executionCount = 0;
        // Execute Task once by default. Repeat it again based on repeat count i.e. mRepeatCount > 0
        do {
            executeTask(taskType, workflowName, spectatioUiUtil);
            executionCount++;
            Log.i(
                    LOG_TAG,
                    String.format(
                            "Completed executing Task %s, %d time(s).", mName, executionCount));

            // Wait for 1 Second before executing another task
            spectatioUiUtil.wait1Second();
        } while (executionCount < (1 + mRepeatCount));

        Log.i(
                LOG_TAG,
                String.format(
                        "Done Executing Task %s with Type %s for Workflow %s",
                        mName, mType, workflowName));
    }

    private void executeTask(
            SupportedWorkFlowTasks taskType, String workflowName, SpectatioUiUtil spectatioUiUtil) {
        switch (taskType) {
            case COMMAND:
                validateAndExecuteCommand(workflowName, spectatioUiUtil);
                break;
            case HAS_PACKAGE_IN_FOREGROUND:
                validateAndVerifyPackage(workflowName, spectatioUiUtil);
                break;
            case HAS_UI_ELEMENT_IN_FOREGROUND:
                validateAndVerifyUiElement(workflowName, spectatioUiUtil);
                break;
            case CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        /* scrollToFind= */ false,
                        /* isLongClick= */ false,
                        /* isOptional= */ false);
                break;
            case CLICK_IF_EXIST:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        /* scrollToFind= */ false,
                        /* isLongClick= */ false,
                        /* isOptional= */ true);
                break;
            case LONG_CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        /* scrollToFind= */ false,
                        /* isLongClick= */ true,
                        /* isOptional= */ false);
                break;
            case PRESS:
                validateAndPressKey(workflowName, spectatioUiUtil, /* isLongPress= */ false);
                break;
            case LONG_PRESS:
                validateAndPressKey(workflowName, spectatioUiUtil, /* isLongPress= */ true);
                break;
            case SCROLL_TO_FIND_AND_CLICK:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        /* scrollToFind= */ true,
                        /* isLongClick= */ false,
                        /* isOptional= */ false);
                break;
            case SCROLL_TO_FIND_AND_CLICK_IF_EXIST:
                validateAndClickUiElement(
                        workflowName,
                        spectatioUiUtil,
                        /* scrollToFind= */ true,
                        /* isLongClick= */ false,
                        /* isOptional= */ true);
                break;
            case WAIT_MS:
                validateAndWait(workflowName, spectatioUiUtil);
                break;
            default:
                throwRuntimeException("Workflow Task Type", mType, workflowName, "Not Supported");
        }
    }

    private void validateAndWait(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        String waitTime = validateAndGetTaskConfigText(workflowName);
        if (!isValidInteger(/* action= */ "WAIT_MS Value", waitTime, workflowName)) {
            throwRuntimeException("Wait", waitTime, workflowName, "Invalid");
        }
        spectatioUiUtil.waitNSeconds(Integer.parseInt(waitTime));
    }

    private void validateAndExecuteCommand(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        String command = validateAndGetTaskConfigText(workflowName);
        spectatioUiUtil.executeShellCommand(command);
    }

    private void validateAndVerifyUiElement(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        UiElement uiElement = validateAndGetTaskConfigUiElement(workflowName);
        BySelector selector = uiElement.getBySelectorForUiElement();
        if (!spectatioUiUtil.hasUiElement(selector)) {
            throwRuntimeException(
                    "UI Element", selector.toString(), workflowName, "Not in Foreground");
        }
    }

    private void validateAndVerifyPackage(String workflowName, SpectatioUiUtil spectatioUiUtil) {
        String pkg = validateAndGetTaskConfigText(workflowName);
        if (!spectatioUiUtil.hasPackageInForeground(pkg)) {
            throwRuntimeException("Package", pkg, workflowName, "Not in Foreground");
        }
    }

    private void validateAndClickUiElement(
            String workflowName,
            SpectatioUiUtil spectatioUiUtil,
            boolean scrollToFind,
            boolean isLongClick,
            boolean isOptional) {
        UiElement uiElement = validateAndGetTaskConfigUiElement(workflowName);
        BySelector selector = uiElement.getBySelectorForUiElement();
        UiObject2 uiObject = spectatioUiUtil.findUiObject(selector);
        if (scrollToFind && !isValidUiObject(uiObject)) {
            ScrollConfig scrollConfig = validateAndGetTaskScrollConfig(workflowName);
            ScrollActions scrollAction = null;
            try {
                scrollAction = ScrollActions.valueOf(scrollConfig.getScrollAction());
            } catch (IllegalArgumentException ex) {
                throwRuntimeException(
                        "Scroll Action",
                        scrollConfig.getScrollAction(),
                        workflowName,
                        "Not Supported");
            }
            try {
                switch (scrollAction) {
                    case USE_BUTTON:
                        BySelector forwardButtonSelector =
                                scrollConfig.getScrollForwardButton().getBySelectorForUiElement();
                        BySelector backwardButtonSelector =
                                scrollConfig.getScrollBackwardButton().getBySelectorForUiElement();
                        uiObject =
                                spectatioUiUtil.scrollAndFindUiObject(
                                        forwardButtonSelector, backwardButtonSelector, selector);
                        break;
                    case USE_GESTURE:
                        BySelector scrollElementSelector =
                                scrollConfig.getScrollElement().getBySelectorForUiElement();
                        ScrollDirection scrollDirection = null;
                        try {
                            scrollDirection =
                                    ScrollDirection.valueOf(scrollConfig.getScrollDirection());
                        } catch (IllegalArgumentException ex) {
                            throwRuntimeException(
                                    "Scroll Direction",
                                    scrollConfig.getScrollDirection(),
                                    workflowName,
                                    "Not Supported");
                        }
                        uiObject =
                                spectatioUiUtil.scrollAndFindUiObject(
                                        scrollElementSelector,
                                        selector,
                                        (scrollDirection == ScrollDirection.VERTICAL));
                        break;
                    default:
                        throwRuntimeException(
                                "Scroll Action",
                                scrollConfig.getScrollAction(),
                                workflowName,
                                "Not Supported");
                }
            } catch (MissingUiElementException ex) {
                throwRuntimeException(
                        "Scroll Button or Element for Scroll Action",
                        scrollConfig.getScrollAction(),
                        workflowName,
                        String.format("Missing. Error: %s", ex.getMessage()));
            }
        }
        if (isOptional && !isValidUiObject(uiObject)) {
            return;
        }
        validateUiObject(uiObject, workflowName);
        if (isLongClick) {
            spectatioUiUtil.longPress(uiObject);
        } else {
            spectatioUiUtil.clickAndWait(uiObject);
        }
    }

    private void validateAndPressKey(
            String workflowName, SpectatioUiUtil spectatioUiUtil, boolean isLongPress) {
        String key = validateAndGetTaskConfigText(workflowName);
        // Check if Key is an integer i.e. KeyCode
        if (isValidInteger(/* action= */ "PRESS", key, workflowName)) {
            int keyCode = Integer.parseInt(key);
            if (isLongPress) {
                spectatioUiUtil.longPressKey(keyCode);
            } else {
                spectatioUiUtil.pressKeyCode(keyCode);
            }
            return;
        }
        switch (key) {
            case "POWER":
                if (isLongPress) {
                    spectatioUiUtil.longPressPower();
                } else {
                    spectatioUiUtil.pressPower();
                }
                break;
            case "HOME":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressHome();
                }
                break;
            case "BACK":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.pressBack();
                }
                break;
            case "SCREEN_CENTER":
                if (isLongPress) {
                    spectatioUiUtil.longPressScreenCenter();
                } else {
                    throwRuntimeException("Press", key, workflowName, "Not Supported");
                }
                break;
            case "WAKE_UP":
                if (isLongPress) {
                    throwRuntimeException("Long Press", key, workflowName, "Not Supported");
                } else {
                    spectatioUiUtil.wakeUp();
                }
                break;
            default:
                throwRuntimeException("Config", key, workflowName, "Not Supported");
        }
    }

    private boolean isValidInteger(String action, String value, String workflowName) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue < 0) {
                throwRuntimeException(action, value, workflowName, "Invalid");
            }
        } catch (NumberFormatException ex) {
            return false;
        }
        return true;
    }

    private boolean isValidUiObject(UiObject2 uiObject) {
        return uiObject != null;
    }

    private void validateUiObject(UiObject2 uiObject, String workflowName) {
        if (!isValidUiObject(uiObject)) {
            throwRuntimeException(
                    "UI Element for Config", "UI_ELEMENT", workflowName, "Missing on Device UI");
        }
    }

    private String validateAndGetTaskConfigText(String workflowName) {
        String taskConfigText = mTaskConfig.getText();
        if (Strings.isNullOrEmpty(taskConfigText)) {
            throwRuntimeException(
                    "Config Text", taskConfigText, workflowName, "Missing or Invalid");
        }
        return taskConfigText.trim();
    }

    private UiElement validateAndGetTaskConfigUiElement(String workflowName) {
        UiElement uiElement = mTaskConfig.getUiElement();
        if (uiElement == null) {
            throwRuntimeException("Config", "UI_ELEMENT", workflowName, "Missing or Invalid");
        }
        return uiElement;
    }

    private ScrollConfig validateAndGetTaskScrollConfig(String workflowName) {
        if (mScrollConfig == null) {
            throwRuntimeException("Config", "SCROLL_CONFIG", workflowName, "Missing or Invalid");
        }
        return mScrollConfig;
    }

    private void throwRuntimeException(
            String property, String value, String workflowName, String reason) {
        throw new RuntimeException(
                String.format(
                        "%s %s for task %s with type %s in Workflow %s is %s.",
                        property, value, mName, mType, workflowName, reason));
    }
}
